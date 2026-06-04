# Math utilities

## PolynomialEvaluator

`cz.bliksoft.javautils.math.polynomial.PolynomialEvaluator`

Evaluates arithmetic expressions given as strings. Supports the four basic operators with standard precedence, parentheses, named variables, and function calls with any number of arguments. A set of common math functions is built in; additional functions and variables can be registered globally (process-wide) or per-evaluator instance.

---

### Quick start

```java
// Pure arithmetic — fastest path, no registry overhead
double r = PolynomialEvaluator.evalFast("24 * 1.2");   // 28.8
double r = PolynomialEvaluator.evalFast("24 / 3 * 2"); // 16.0
double r = PolynomialEvaluator.evalFast("10 % 3");     // 1.0
double r = PolynomialEvaluator.evalFast("2^10");        // 1024.0
double r = PolynomialEvaluator.evalFast("2^2^3");       // 256.0 (right-assoc: 2^8)

// With built-in functions and constants — uses global registries
double r = PolynomialEvaluator.eval("floor(24.7)");          // 24.0
double r = PolynomialEvaluator.eval("round(sqrt(2) * 10)");  // 14.0
double r = PolynomialEvaluator.eval("min(8, 3, 5)");         // 3.0
double r = PolynomialEvaluator.eval("avg(1, 2, 3, 4)");      // 2.5
double r = PolynomialEvaluator.eval("log(100)");             // 2.0
double r = PolynomialEvaluator.eval("log(8, 2)");            // 3.0
double r = PolynomialEvaluator.eval("ln(E)");                // 1.0
double r = PolynomialEvaluator.eval("sin(PI / 2)");          // 1.0
double r = PolynomialEvaluator.eval("clamp(150, 0, 100)");   // 100.0
double r = PolynomialEvaluator.eval("lerp(0, 100, 0.25)");   // 25.0

// Instance evaluator — local variables and functions
PolynomialEvaluator ev = new PolynomialEvaluator();
ev.registerVariable("size", 24);
ev.registerVariable("scale", 1.5);
ev.registerFunction("px", args -> Math.round(args[0]) + 0.0); // custom function
double r = ev.evaluate("size * scale");  // 36.0
double r = ev.evaluate("px(size * scale)");  // 36.0

// Register a global variable used by all eval() calls from this point on
PolynomialEvaluator.registerGlobalVariable("DPI", 96.0);
double r = PolynomialEvaluator.eval("16 * DPI / 96");  // 16.0
```

---

### Expression syntax

| Construct | Syntax | Notes |
|---|---|---|
| Number literal | integer or decimal, optional scientific notation | `24`, `1.5`, `1.2e3` |
| Add / subtract | `+` `-` | lowest precedence, left-to-right |
| Multiply / divide / modulo | `*` `/` `%` | left-to-right |
| Power | `^` | **right-associative**, highest binary precedence — `2^2^3` = `2^8` = 256 |
| Unary sign | leading `+` or `-` | `-size`, `+1`; applied before `^` |
| Parentheses | `(…)` | `(size + 4) * 2` |
| Variable | identifier `[A-Za-z_][A-Za-z0-9_]*` | `size`, `scale_x`, `PI` |
| Function call | `name(arg, …)` — any number of arguments | `min(a, b, c)`, `log(x, 2)` |

Whitespace between tokens is ignored.

---

### Built-in functions and constants

Registered in the global registries on first use of `eval()` or `evaluate()`.

**Rounding / comparison**

| Name | Arity | Behaviour |
|---|---|---|
| `floor` | 1 | `Math.floor(x)` |
| `ceil` | 1 | `Math.ceil(x)` |
| `round` | 1 | `Math.round(x)` as double |
| `abs` | 1 | `Math.abs(x)` |
| `sign` | 1 | `Math.signum(x)` — returns -1, 0, or 1 |
| `min` | 1+ | minimum of all arguments |
| `max` | 1+ | maximum of all arguments |
| `mean` / `avg` | 1+ | arithmetic mean of all arguments |
| `clamp` | 3 | `clamp(value, min, max)` — clamps to \[min, max\] |

**Powers and roots**

| Name | Arity | Behaviour |
|---|---|---|
| `sqrt` | 1 | `Math.sqrt(x)` |
| `pow` | 2 | `pow(base, exp)` — function alias for `^` |
| `exp` | 1 | `Math.exp(x)` — e^x |

**Logarithms**

| Name | Arity | Behaviour |
|---|---|---|
| `log` | 1 | `Math.log10(x)` — base 10 |
| `log` | 2 | `log(x, base)` — arbitrary base |
| `log2` | 1 | base-2 logarithm |
| `ln` | 1 | `Math.log(x)` — natural logarithm |

**Trigonometry** (arguments in radians)

| Name | Arity | Behaviour |
|---|---|---|
| `sin` | 1 | `Math.sin(x)` |
| `cos` | 1 | `Math.cos(x)` |
| `tan` | 1 | `Math.tan(x)` |

**Interpolation**

| Name | Arity | Behaviour |
|---|---|---|
| `lerp` | 3 | `lerp(a, b, t)` = `a + (b-a)*t` |

**Constants** (global variables)

| Name | Value |
|---|---|
| `PI` | `Math.PI` ≈ 3.14159… |
| `E` | `Math.E` ≈ 2.71828… |

---

### Registration API

#### Global (process-wide)

```java
PolynomialEvaluator.registerGlobalFunction("deg2rad", args -> Math.toRadians(args[0]));
PolynomialEvaluator.registerGlobalVariable("PI", Math.PI);
```

Global registries are thread-safe (`ConcurrentHashMap`). They are lazily initialized — no allocations occur unless `eval()`, `evaluate()`, or a register method is called. `evalFast()` never touches them.

#### Instance-local

```java
PolynomialEvaluator ev = new PolynomialEvaluator();
ev.registerFunction("clamp", args -> Math.max(args[0], Math.min(args[1], args[2])));
ev.registerVariable("maxSize", 64);
double r = ev.evaluate("clamp(0, size, maxSize)");
```

Local registrations override globals with the same name. Instance registries are not thread-safe — set them up before sharing the instance across threads.

---

### Dynamic variable getters (`$` and `@` prefixes)

Variables whose name starts with `$` or `@` can be resolved by a pluggable getter
function registered for that prefix. The getter receives the full variable name
(including the prefix) and returns its value as a `double`.

Until a getter is registered, `$name` and `@name` fall back to the normal variable
registry. Once a getter is registered it takes full responsibility for all variables
with that prefix.

The getter type is `java.util.function.ToDoubleFunction<String>`, so lambdas work
directly with no extra import:

```java
// Global getter — applies to all eval() calls
PolynomialEvaluator.registerGlobalVariableGetter('$', name -> switch (name) {
    case "$size"  -> 24.0;
    case "$scale" -> 1.5;
    default -> throw new IllegalArgumentException("Unknown variable: " + name);
});

double r = PolynomialEvaluator.eval("$size * $scale");  // 36.0

// Instance getter — overrides the global getter for '@' on this evaluator only
PolynomialEvaluator ev = new PolynomialEvaluator();
ev.registerVariableGetter('@', name -> themeMap.getDouble(name.substring(1)));
double r = ev.evaluate("@padding * 2 + @margin");
```

**Resolution order** (highest priority first):

1. Instance getter for the matching prefix (if registered on this evaluator)
2. Global getter for the matching prefix
3. Normal variable registry (local then global)

**Notes:**
- `$`/`@` variables do not support function-call syntax (`$foo()` is a parse error).
- Throw `IllegalArgumentException` from the getter for unknown names — this surfaces as a normal evaluation error.
- The two prefixes are fully independent; registering a `$` getter does not affect `@` variables.

---

### Choosing the right entry point

| Method | Registries used | Use when |
|---|---|---|
| `evalFast(expr)` | none | Pure arithmetic, no functions or variables needed |
| `eval(expr)` | global only | Standard use; built-in functions available |
| `evaluate(expr)` | global + instance-local | Per-call context (variables, custom functions) |

---

### Error handling

All three entry points throw `IllegalArgumentException` on any parse or evaluation error: unknown function or variable, mismatched parentheses, unexpected character, or wrong argument count for a built-in function. Callers that need a fallback value should catch it:

```java
try {
    return PolynomialEvaluator.eval(expr);
} catch (IllegalArgumentException e) {
    return defaultValue;
}
```

---

### Custom functions via PolynomialFunction

`PolynomialFunction` is a `@FunctionalInterface`:

```java
@FunctionalInterface
public interface PolynomialFunction {
    double apply(double... args);
}
```

Implementations should validate arity and throw `IllegalArgumentException` for wrong argument counts, consistent with the built-in functions.

```java
PolynomialEvaluator.registerGlobalFunction("lerp", args -> {
    if (args.length != 3)
        throw new IllegalArgumentException("lerp() requires exactly 3 arguments");
    return args[0] + (args[1] - args[0]) * args[2];
});
```
