package cz.bliksoft.javautils.freemarker.utils;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import cz.bliksoft.javautils.freemarker.utils.TemplateParameterUtils.TemplateParameter;

/**
 * UI-independent logic of the forms built from template parameter declarations
 * ({@link TemplateParameterUtils}), shared by the web and desktop form
 * generators:
 * <ul>
 * <li>COMBO/FONT options - declared, or replaced by a model's
 * {@code <name>_options} ({@link #resolveOptions}),</li>
 * <li>the initial value of a field - entered by the user, else the
 * application's model value, else the template default
 * ({@link #initialValue}),</li>
 * <li>the conversion of a field's text to a typed value
 * ({@link #typedValue}).</li>
 * </ul>
 * A field's value in a form is text: dates ISO ({@link TemplateDateValues}),
 * decimals with a dot, booleans {@code true}/{@code false}, option values
 * {@code String.valueOf} of the model's keys.
 */
public final class TemplateFormSupport {

	private static final Logger log = Logger.getLogger(TemplateFormSupport.class.getName());

	private TemplateFormSupport() {
	}

	/** A selectable value of a COMBO/FONT field and its displayed label. */
	public static final class Option {
		private final String value;
		private final String label;

		public Option(String value, String label) {
			this.value = value;
			this.label = label != null ? label : value;
		}

		public String getValue() {
			return value;
		}

		public String getLabel() {
			return label;
		}

		/** The values of the options, in order. */
		public static List<String> values(List<Option> options) {
			List<String> result = new ArrayList<>();
			if (options != null)
				for (Option o : options)
					result.add(o.value);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Option))
				return false;
			Option o = (Option) obj;
			return value.equals(o.value) && label.equals(o.label);
		}

		@Override
		public int hashCode() {
			return value.hashCode() * 31 + label.hashCode();
		}

		@Override
		public String toString() {
			return value.equals(label) ? value : value + "=" + label;
		}
	}

	/** The model key of a field's options: {@code <name>_options}. */
	public static String optionsKey(String name) {
		return name + TemplateParameterUtils.OPTIONS_SUFFIX;
	}

	/**
	 * Options declared in a template (the parameters part): separated by {@code ;},
	 * or by {@code ,} when there is no {@code ;}; trimmed, empty ones dropped, the
	 * value is the label.
	 */
	public static List<Option> declaredOptions(String parameters) {
		List<Option> result = new ArrayList<>();
		for (String part : split(parameters))
			result.add(new Option(part, part));
		return result;
	}

	private static List<String> split(String s) {
		List<String> result = new ArrayList<>();
		if (s == null)
			return result;
		for (String part : s.split(s.indexOf(';') >= 0 ? ";" : ",")) {
			String trimmed = part.trim();
			if (!trimmed.isEmpty())
				result.add(trimmed);
		}
		return result;
	}

	/**
	 * Options written as text: {@code a;b;c} (or {@code a,b,c}) - values that are
	 * their own labels, or {@code key=Label;key2=Label2} when every item has a
	 * {@code =}.
	 */
	public static List<Option> parseOptionsString(String s) {
		List<String> parts = split(s);
		boolean pairs = !parts.isEmpty();
		for (String part : parts)
			pairs &= part.indexOf('=') > 0;
		List<Option> result = new ArrayList<>();
		for (String part : parts) {
			if (pairs) {
				int eq = part.indexOf('=');
				result.add(new Option(part.substring(0, eq).trim(), part.substring(eq + 1).trim()));
			} else {
				result.add(new Option(part, part));
			}
		}
		return result;
	}

	/**
	 * Options given by an application: a {@code Map} - its keys are the values, its
	 * values the labels, in the map's order; a {@code Collection} or an array -
	 * values that are their own labels; text - {@link #parseOptionsString};
	 * {@code null} for {@code null}. Values are {@code String.valueOf} of the keys
	 * or items.
	 */
	public static List<Option> toOptions(Object spec) {
		if (spec == null)
			return null;
		List<Option> result = new ArrayList<>();
		if (spec instanceof Map) {
			for (Map.Entry<?, ?> e : ((Map<?, ?>) spec).entrySet()) {
				if (e.getKey() == null)
					continue;
				String value = text(e.getKey());
				result.add(new Option(value, e.getValue() != null ? String.valueOf(e.getValue()) : value));
			}
		} else if (spec instanceof Collection) {
			for (Object item : (Collection<?>) spec)
				addItem(result, item);
		} else if (spec.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(spec); i++)
				addItem(result, Array.get(spec, i));
		} else {
			return parseOptionsString(String.valueOf(spec));
		}
		return result;
	}

	private static void addItem(List<Option> result, Object item) {
		if (item instanceof Option)
			result.add((Option) item);
		else if (item != null)
			result.add(new Option(text(item), text(item)));
	}

	private static String text(Object value) {
		return value instanceof Enum ? ((Enum<?>) value).name() : String.valueOf(value);
	}

	/**
	 * The options of a field given by the model ({@code <name>_options},
	 * {@link #toOptions}), {@code null} when the model (may be {@code null}) has
	 * none.
	 */
	public static List<Option> modelOptions(Map<String, ?> model, String name) {
		if (model == null || name == null)
			return null;
		return toOptions(model.get(optionsKey(name)));
	}

	/**
	 * The options of a COMBO/FONT field (empty for other types):
	 * <ul>
	 * <li>COMBO - the model's {@code <name>_options}, else {@code resolverOptions}
	 * (an application's options source, may be {@code null}), else the declared
	 * ones;</li>
	 * <li>FONT - {@code resolverOptions} (the printer's fonts - it can only use
	 * those) when not empty, else the model's {@code <name>_options}, else the
	 * declared ones.</li>
	 * </ul>
	 */
	public static List<Option> resolveOptions(String type, String name, String parameters, List<String> resolverOptions,
			Map<String, ?> model) {
		String t = TemplateParameterUtils.normalizeType(type);
		boolean font = TemplateParameterUtils.TYPE_FONT.equals(t);
		if (!font && !TemplateParameterUtils.TYPE_COMBO.equals(t))
			return new ArrayList<>();
		if (font && resolverOptions != null && !resolverOptions.isEmpty())
			return toOptions(resolverOptions);
		List<Option> fromModel = modelOptions(model, name);
		if (fromModel != null)
			return fromModel;
		if (!font && resolverOptions != null)
			return toOptions(resolverOptions);
		return declaredOptions(parameters);
	}

	/**
	 * A value as the text of a field of the given type, {@code null} when it cannot
	 * be one (or is {@code null}):
	 * <ul>
	 * <li>INT - a whole number,</li>
	 * <li>DECIMAL - a number, with a dot (text with a comma is kept),</li>
	 * <li>BOOLEAN - {@code true}/{@code false} ({@code 1}/{@code yes}/{@code on}
	 * are true),</li>
	 * <li>DATE/DATETIME - ISO ({@link TemplateDateValues#formatDate},
	 * {@link TemplateDateValues#formatDateTime}) of a date-like value or of a date
	 * expression ({@code today+7}),</li>
	 * <li>CSVFILE - never,</li>
	 * <li>others - the text ({@code name()} of an enum).</li>
	 * </ul>
	 * Empty text stays empty ("no value").
	 */
	public static String formValue(String type, Object value) {
		if (value == null)
			return null;
		if (value instanceof CharSequence && value.toString().trim().isEmpty())
			return "";
		String t = TemplateParameterUtils.normalizeType(type);
		switch (t) {
		case TemplateParameterUtils.TYPE_INT:
			return intText(value);
		case TemplateParameterUtils.TYPE_DECIMAL:
			return decimalText(value);
		case TemplateParameterUtils.TYPE_BOOLEAN:
			if (value instanceof Boolean)
				return value.toString();
			String b = value.toString().trim();
			return Boolean.toString("true".equalsIgnoreCase(b) || "1".equals(b) || "yes".equalsIgnoreCase(b)
					|| "on".equalsIgnoreCase(b));
		case TemplateParameterUtils.TYPE_DATE:
			return TemplateDateValues.formatDate(TemplateDateValues.toLocalDate(value));
		case TemplateParameterUtils.TYPE_DATETIME:
			return TemplateDateValues.formatDateTime(TemplateDateValues.toLocalDateTime(value));
		case TemplateParameterUtils.TYPE_CSVFILE:
			return null;
		default:
			return text(value);
		}
	}

	private static String intText(Object value) {
		try {
			if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte
					|| value instanceof BigInteger)
				return value.toString();
			BigDecimal d = value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString().trim());
			return d.toBigIntegerExact().toString();
		} catch (ArithmeticException | NumberFormatException e) {
			return null;
		}
	}

	private static String decimalText(Object value) {
		if (value instanceof BigDecimal)
			return ((BigDecimal) value).toPlainString();
		if (value instanceof Double || value instanceof Float) {
			double d = ((Number) value).doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d))
				return null;
			return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
		}
		if (value instanceof Number)
			return value.toString();
		String s = value.toString().trim();
		return TemplateValueCoercion.parseDecimal(s) != null ? s : null;
	}

	/**
	 * The initial text of a field: the first usable of
	 * <ol>
	 * <li>{@code entered} - the user's text (from a previous form, {@code null}
	 * when none),</li>
	 * <li>the model's value - when the model (may be {@code null}) has the
	 * {@code name} key; {@code null} there means empty,</li>
	 * <li>{@code defaultValue} - the template default (a date expression is
	 * evaluated).</li>
	 * </ol>
	 * Each is converted by {@link #formValue}; one that cannot be converted, or is
	 * not one of {@code optionValues} (when not empty), is skipped. When none fits,
	 * the first option, else {@code null}.
	 */
	public static String initialValue(String type, String name, String defaultValue, Map<String, ?> model,
			String entered, List<String> optionValues) {
		String result = usable(type, entered, optionValues);
		if (result == null && model != null && name != null && model.containsKey(name)) {
			Object value = model.get(name);
			result = usable(type, value != null ? value : "", optionValues);
		}
		if (result == null)
			result = usable(type, defaultValue, optionValues);
		if (result == null && optionValues != null && !optionValues.isEmpty())
			result = optionValues.get(0);
		return result;
	}

	private static String usable(String type, Object candidate, List<String> optionValues) {
		String text = formValue(type, candidate);
		if (text == null)
			return null;
		if (optionValues != null && !optionValues.isEmpty() && !optionValues.contains(text))
			return null;
		return text;
	}

	/**
	 * A field's text as the template value: INT {@code Integer}, DECIMAL
	 * {@code Double} (dot or comma), BOOLEAN {@code Boolean}, DATE
	 * {@code LocalDate}, DATETIME {@code LocalDateTime} (ISO or a date expression),
	 * others the text; {@code null} when blank.
	 *
	 * @throws IllegalArgumentException when not a value of the type
	 */
	public static Object typedValue(String type, String raw) {
		if (raw == null || raw.trim().isEmpty())
			return null;
		String s = raw.trim();
		switch (TemplateParameterUtils.normalizeType(type)) {
		case TemplateParameterUtils.TYPE_INT:
			return Integer.valueOf(s);
		case TemplateParameterUtils.TYPE_DECIMAL:
			Double d = TemplateValueCoercion.parseDecimal(s);
			if (d == null)
				throw new NumberFormatException("Not a decimal number: " + raw);
			return d;
		case TemplateParameterUtils.TYPE_BOOLEAN:
			return Boolean.valueOf(formValue(TemplateParameterUtils.TYPE_BOOLEAN, s));
		case TemplateParameterUtils.TYPE_DATE:
			return TemplateDateValues.parseDate(s);
		case TemplateParameterUtils.TYPE_DATETIME:
			return TemplateDateValues.parseDateTime(s);
		default:
			return raw;
		}
	}

	/**
	 * Replaces text values of INT, DECIMAL, BOOLEAN, DATE and DATETIME parameters
	 * in {@code variables} with typed ones ({@link #typedValue}); text that is not
	 * a value of the type is kept (and logged). For values that did not come from a
	 * form (an AI tool, configuration).
	 *
	 * @return {@code variables}
	 */
	public static Map<String, Object> coerceToDeclaredTypes(List<TemplateParameter> parameters,
			Map<String, Object> variables) {
		if (parameters == null || variables == null)
			return variables;
		for (TemplateParameter p : parameters) {
			Object value = variables.get(p.getName());
			if (!(value instanceof String))
				continue;
			String type = p.getNormalizedType();
			switch (type) {
			case TemplateParameterUtils.TYPE_INT:
			case TemplateParameterUtils.TYPE_DECIMAL:
			case TemplateParameterUtils.TYPE_BOOLEAN:
			case TemplateParameterUtils.TYPE_DATE:
			case TemplateParameterUtils.TYPE_DATETIME:
				try {
					variables.put(p.getName(), typedValue(type, (String) value));
				} catch (IllegalArgumentException e) {
					log.warning("Value of " + p.getName() + " is not " + type + ": " + value);
				}
				break;
			default:
				break;
			}
		}
		return variables;
	}
}
