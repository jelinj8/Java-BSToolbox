package cz.bliksoft.javautils.math.polynomial;

import cz.bliksoft.javautils.math.polynomial.functions.AbsFunction;
import cz.bliksoft.javautils.math.polynomial.functions.CeilFunction;
import cz.bliksoft.javautils.math.polynomial.functions.ClampFunction;
import cz.bliksoft.javautils.math.polynomial.functions.CosFunction;
import cz.bliksoft.javautils.math.polynomial.functions.ExpFunction;
import cz.bliksoft.javautils.math.polynomial.functions.FloorFunction;
import cz.bliksoft.javautils.math.polynomial.functions.LerpFunction;
import cz.bliksoft.javautils.math.polynomial.functions.LnFunction;
import cz.bliksoft.javautils.math.polynomial.functions.Log2Function;
import cz.bliksoft.javautils.math.polynomial.functions.LogFunction;
import cz.bliksoft.javautils.math.polynomial.functions.MaxFunction;
import cz.bliksoft.javautils.math.polynomial.functions.MeanFunction;
import cz.bliksoft.javautils.math.polynomial.functions.MinFunction;
import cz.bliksoft.javautils.math.polynomial.functions.PowFunction;
import cz.bliksoft.javautils.math.polynomial.functions.RoundFunction;
import cz.bliksoft.javautils.math.polynomial.functions.SignFunction;
import cz.bliksoft.javautils.math.polynomial.functions.SinFunction;
import cz.bliksoft.javautils.math.polynomial.functions.SqrtFunction;
import cz.bliksoft.javautils.math.polynomial.functions.TanFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;

/**
 * Evaluates arithmetic expressions given as strings.
 * <p>
 * Supports {@code +}, {@code -}, {@code *}, {@code /}, {@code %} (modulo), and
 * {@code ^} (power, right-associative) with standard operator precedence,
 * parentheses, named variables, and function calls with any number of
 * arguments. Numbers may be integer, decimal, or in scientific notation.
 * Whitespace between tokens is ignored.
 * <p>
 * Three entry points are available:
 * <ul>
 * <li>{@link #evalFast(String)} — pure arithmetic; no registry access at
 * all.</li>
 * <li>{@link #eval(String)} — arithmetic plus the global function and variable
 * registries (built-in functions are pre-registered there).</li>
 * <li>{@link #evaluate(String)} — same as {@code eval} but also includes any
 * functions and variables registered on this instance; instance-local entries
 * take priority over global ones.</li>
 * </ul>
 * <p>
 * The global registries are lazily initialized on the first call to
 * {@code eval}, {@code evaluate}, or any {@code registerGlobal*} method —
 * {@code evalFast} never allocates them. The registries are backed by
 * {@link java.util.concurrent.ConcurrentHashMap} and are safe for concurrent
 * use.
 */
public class PolynomialEvaluator {

	private static volatile Map<String, PolynomialFunction> GLOBAL_FUNCTIONS;
	private static volatile Map<String, Double> GLOBAL_VARIABLES;
	private static volatile ToDoubleFunction<String> GLOBAL_DOLLAR_GETTER;
	private static volatile ToDoubleFunction<String> GLOBAL_AT_GETTER;

	private final Map<String, PolynomialFunction> localFunctions = new HashMap<>();
	private final Map<String, Double> localVariables = new HashMap<>();
	private ToDoubleFunction<String> localDollarGetter;
	private ToDoubleFunction<String> localAtGetter;

	private static Map<String, PolynomialFunction> globalFunctions() {
		if (GLOBAL_FUNCTIONS == null) {
			synchronized (PolynomialEvaluator.class) {
				if (GLOBAL_FUNCTIONS == null) {
					Map<String, PolynomialFunction> m = new ConcurrentHashMap<>();
					m.put("floor", new FloorFunction());
					m.put("ceil", new CeilFunction());
					m.put("round", new RoundFunction());
					m.put("sqrt", new SqrtFunction());
					m.put("abs", new AbsFunction());
					m.put("min", new MinFunction());
					m.put("max", new MaxFunction());
					MeanFunction mean = new MeanFunction();
					m.put("mean", mean);
					m.put("avg", mean);
					m.put("log", new LogFunction());
					m.put("ln", new LnFunction());
					m.put("pow", new PowFunction());
					m.put("log2", new Log2Function());
					m.put("exp", new ExpFunction());
					m.put("sign", new SignFunction());
					m.put("clamp", new ClampFunction());
					m.put("lerp", new LerpFunction());
					m.put("sin", new SinFunction());
					m.put("cos", new CosFunction());
					m.put("tan", new TanFunction());
					GLOBAL_FUNCTIONS = m;
				}
			}
		}
		return GLOBAL_FUNCTIONS;
	}

	private static Map<String, Double> globalVariables() {
		if (GLOBAL_VARIABLES == null) {
			synchronized (PolynomialEvaluator.class) {
				if (GLOBAL_VARIABLES == null) {
					Map<String, Double> v = new ConcurrentHashMap<>();
					v.put("PI", Math.PI);
					v.put("E", Math.E);
					GLOBAL_VARIABLES = v;
				}
			}
		}
		return GLOBAL_VARIABLES;
	}

	/**
	 * Registers a function in the global registry, making it available to all
	 * subsequent {@link #eval} and {@link #evaluate} calls.
	 *
	 * @param name case-sensitive function name as it appears in expressions
	 * @param fn   the function implementation
	 */
	public static void registerGlobalFunction(String name, PolynomialFunction fn) {
		globalFunctions().put(name, fn);
	}

	/**
	 * Registers a variable in the global registry, making it available to all
	 * subsequent {@link #eval} and {@link #evaluate} calls.
	 *
	 * @param name  case-sensitive variable name as it appears in expressions
	 * @param value the variable value
	 */
	public static void registerGlobalVariable(String name, double value) {
		globalVariables().put(name, value);
	}

	/**
	 * Registers a function in this instance's local registry. Local functions
	 * override any global function with the same name for calls to
	 * {@link #evaluate}.
	 *
	 * @param name case-sensitive function name
	 * @param fn   the function implementation
	 */
	public void registerFunction(String name, PolynomialFunction fn) {
		localFunctions.put(name, fn);
	}

	/**
	 * Registers a variable in this instance's local registry. Local variables
	 * override any global variable with the same name for calls to
	 * {@link #evaluate}.
	 *
	 * @param name  case-sensitive variable name
	 * @param value the variable value
	 */
	public void registerVariable(String name, double value) {
		localVariables.put(name, value);
	}

	/**
	 * Registers a dynamic variable getter in the global registry for variables
	 * whose name starts with the given prefix. The getter is called with the full
	 * variable name (including the leading prefix character) and returns its value.
	 * Replaces any previously registered global getter for that prefix.
	 *
	 * @param prefix {@code '$'} or {@code '@'}
	 * @param getter the getter function; throw {@link IllegalArgumentException} for
	 *               unknown names
	 * @throws IllegalArgumentException if prefix is not {@code '$'} or {@code '@'}
	 */
	public static void registerGlobalVariableGetter(char prefix, ToDoubleFunction<String> getter) {
		if (prefix == '$')
			GLOBAL_DOLLAR_GETTER = getter;
		else if (prefix == '@')
			GLOBAL_AT_GETTER = getter;
		else
			throw new IllegalArgumentException("Unsupported variable prefix: '" + prefix + "' (use '$' or '@')");
	}

	/**
	 * Registers an instance-local dynamic variable getter for variables whose name
	 * starts with the given prefix. Instance-local getters take priority over
	 * global getters for calls to {@link #evaluate}.
	 *
	 * @param prefix {@code '$'} or {@code '@'}
	 * @param getter the getter function; throw {@link IllegalArgumentException} for
	 *               unknown names
	 * @throws IllegalArgumentException if prefix is not {@code '$'} or {@code '@'}
	 */
	public void registerVariableGetter(char prefix, ToDoubleFunction<String> getter) {
		if (prefix == '$')
			localDollarGetter = getter;
		else if (prefix == '@')
			localAtGetter = getter;
		else
			throw new IllegalArgumentException("Unsupported variable prefix: '" + prefix + "' (use '$' or '@')");
	}

	/**
	 * Evaluates {@code expression} using the global function and variable
	 * registries. Throws {@link IllegalArgumentException} on any parse or
	 * evaluation error.
	 *
	 * @param expression the arithmetic expression to evaluate
	 * @return the result as a {@code double}
	 */
	public static double eval(String expression) {
		return new Parser(expression, globalFunctions(), globalVariables(), GLOBAL_DOLLAR_GETTER, GLOBAL_AT_GETTER)
				.parse();
	}

	/**
	 * Evaluates {@code expression} as pure arithmetic — no function calls or
	 * variable lookups are supported and the global registries are never accessed.
	 * Use this when the expression is known to contain only numbers and operators
	 * and startup overhead must be minimized.
	 *
	 * @param expression the arithmetic expression to evaluate
	 * @return the result as a {@code double}
	 * @throws IllegalArgumentException if the expression contains an identifier
	 */
	public static double evalFast(String expression) {
		return new Parser(expression, null, null, null, null).parse();
	}

	/**
	 * Evaluates {@code expression} using the global registries combined with the
	 * functions and variables registered on this instance. Instance-local entries
	 * take priority over global entries with the same name.
	 *
	 * @param expression the arithmetic expression to evaluate
	 * @return the result as a {@code double}
	 */
	public double evaluate(String expression) {
		Map<String, PolynomialFunction> fns = new HashMap<>(globalFunctions());
		fns.putAll(localFunctions);
		Map<String, Double> vars = new HashMap<>(globalVariables());
		vars.putAll(localVariables);
		ToDoubleFunction<String> dg = localDollarGetter != null ? localDollarGetter : GLOBAL_DOLLAR_GETTER;
		ToDoubleFunction<String> ag = localAtGetter != null ? localAtGetter : GLOBAL_AT_GETTER;
		return new Parser(expression, fns, vars, dg, ag).parse();
	}

	private static class Parser {

		private final String input;
		private int pos;
		private final Map<String, PolynomialFunction> functions;
		private final Map<String, Double> variables;
		private final ToDoubleFunction<String> dollarGetter;
		private final ToDoubleFunction<String> atGetter;

		Parser(String input, Map<String, PolynomialFunction> functions, Map<String, Double> variables,
				ToDoubleFunction<String> dollarGetter, ToDoubleFunction<String> atGetter) {
			this.input = input;
			this.functions = functions;
			this.variables = variables;
			this.dollarGetter = dollarGetter;
			this.atGetter = atGetter;
		}

		double parse() {
			double result = parseExpression();
			skipWhitespace();
			if (pos < input.length())
				throw new IllegalArgumentException(
						"Unexpected character at position " + pos + ": '" + input.charAt(pos) + "'");
			return result;
		}

		private double parseExpression() {
			double result = parseTerm();
			while (true) {
				skipWhitespace();
				if (pos < input.length() && input.charAt(pos) == '+') {
					pos++;
					result += parseTerm();
				} else if (pos < input.length() && input.charAt(pos) == '-') {
					pos++;
					result -= parseTerm();
				} else {
					break;
				}
			}
			return result;
		}

		private double parseTerm() {
			double result = parsePower();
			while (true) {
				skipWhitespace();
				if (pos < input.length() && input.charAt(pos) == '*') {
					pos++;
					result *= parsePower();
				} else if (pos < input.length() && input.charAt(pos) == '/') {
					pos++;
					result /= parsePower();
				} else if (pos < input.length() && input.charAt(pos) == '%') {
					pos++;
					result %= parsePower();
				} else {
					break;
				}
			}
			return result;
		}

		private double parsePower() {
			double base = parseUnary();
			skipWhitespace();
			if (pos < input.length() && input.charAt(pos) == '^') {
				pos++;
				return Math.pow(base, parsePower()); // right-associative
			}
			return base;
		}

		private double parseUnary() {
			skipWhitespace();
			if (pos < input.length() && input.charAt(pos) == '-') {
				pos++;
				return -parseUnary();
			}
			if (pos < input.length() && input.charAt(pos) == '+') {
				pos++;
				return parseUnary();
			}
			return parsePrimary();
		}

		private double parsePrimary() {
			skipWhitespace();
			if (pos >= input.length())
				throw new IllegalArgumentException("Unexpected end of expression");
			char c = input.charAt(pos);
			if (c == '(') {
				pos++;
				double val = parseExpression();
				skipWhitespace();
				if (pos >= input.length() || input.charAt(pos) != ')')
					throw new IllegalArgumentException("Expected ')' at position " + pos);
				pos++;
				return val;
			}
			if (Character.isDigit(c) || c == '.')
				return parseNumber();
			if (Character.isLetter(c) || c == '_' || c == '$' || c == '@')
				return parseIdentifierOrCall();
			throw new IllegalArgumentException("Unexpected character at position " + pos + ": '" + c + "'");
		}

		private double parseNumber() {
			int start = pos;
			while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.'))
				pos++;
			if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
				pos++;
				if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-'))
					pos++;
				while (pos < input.length() && Character.isDigit(input.charAt(pos)))
					pos++;
			}
			try {
				return Double.parseDouble(input.substring(start, pos));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid number at position " + start);
			}
		}

		private double parseIdentifierOrCall() {
			if (functions == null && variables == null && dollarGetter == null && atGetter == null)
				throw new IllegalArgumentException("Identifiers not supported in fast mode at position " + pos);
			String name = parseIdentifier();
			char first = name.charAt(0);

			// Special-prefix variables ($, @): no function-call syntax, getter takes
			// priority when registered, otherwise fall through to variable lookup
			if (first == '$' || first == '@') {
				ToDoubleFunction<String> getter = first == '$' ? dollarGetter : atGetter;
				if (getter != null)
					return getter.applyAsDouble(name);
			} else {
				// Regular identifier — may be a function call
				skipWhitespace();
				if (pos < input.length() && input.charAt(pos) == '(') {
					pos++;
					List<Double> args = new ArrayList<>();
					skipWhitespace();
					if (pos < input.length() && input.charAt(pos) != ')') {
						args.add(parseExpression());
						while (pos < input.length() && input.charAt(pos) == ',') {
							pos++;
							args.add(parseExpression());
						}
					}
					skipWhitespace();
					if (pos >= input.length() || input.charAt(pos) != ')')
						throw new IllegalArgumentException("Expected ')' after function arguments at position " + pos);
					pos++;
					PolynomialFunction fn = functions != null ? functions.get(name) : null;
					if (fn == null)
						throw new IllegalArgumentException("Unknown function: " + name);
					double[] argArray = new double[args.size()];
					for (int i = 0; i < args.size(); i++)
						argArray[i] = args.get(i);
					return fn.apply(argArray);
				}
			}

			// Variable lookup — covers regular names and $ / @ when no getter registered
			Double val = variables != null ? variables.get(name) : null;
			if (val == null)
				throw new IllegalArgumentException("Unknown variable: " + name);
			return val;
		}

		private String parseIdentifier() {
			int start = pos++; // first char ($, @, letter, _) already validated by parsePrimary
			while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_'
					|| input.charAt(pos) == '$' || input.charAt(pos) == '@' || input.charAt(pos) == '.'))
				pos++;
			return input.substring(start, pos);
		}

		private void skipWhitespace() {
			while (pos < input.length() && Character.isWhitespace(input.charAt(pos)))
				pos++;
		}
	}

}
