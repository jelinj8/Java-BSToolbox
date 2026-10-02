package cz.bliksoft.javautils.freemarker.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

/**
 * Coerces a raw {@code String} value (e.g. read from XML config, a form field,
 * or JSON) into a typed value, either against an explicitly declared target
 * type or against an already-set value's own runtime type. Used when merging
 * printer advanced-properties/template-variable layers into a template's
 * variable map, so a later {@code String} override honors the type an earlier
 * layer already established for the same variable name.
 *
 * <p>
 * This intentionally does not touch
 * {@link TemplateParameterUtils#extractDefaultVariables}, which has its own,
 * differently-behaved (fallback-to-zero-on-failure) INT/DECIMAL/BOOLEAN
 * coercion for template-declared {@code {var|...}} defaults.
 */
public final class TemplateValueCoercion {

	private TemplateValueCoercion() {
	}

	/**
	 * Coerces {@code raw} to {@code targetType}. Returns {@code null} if
	 * {@code raw} is {@code null} or cannot be parsed into a numeric/boolean/enum
	 * target type. An enum target type matches a constant name ignoring case, with
	 * {@code -} for {@code _}. {@code Map.class} is decoded via
	 * {@link MapPropertyCodec} (never {@code null}, empty map on blank input).
	 * {@code LocalDate}/ {@code LocalDateTime} accept an ISO value or a date
	 * expression ({@link TemplateDateValues}, e.g. {@code today+7}). Any other
	 * target type (including {@code String.class}) returns {@code raw} unchanged.
	 *
	 * <p>
	 * Supported numeric target types: {@code Integer}/{@code int}, {@code Long}/
	 * {@code long}, {@link BigInteger}, {@code Float}/{@code float},
	 * {@code Double}/ {@code double}, {@link BigDecimal}.
	 */
	public static Object coerce(String raw, Class<?> targetType) {
		if (targetType == Map.class)
			return MapPropertyCodec.decode(raw);
		if (raw == null)
			return null;
		String trimmed = raw.trim();
		try {
			if (targetType == Integer.class || targetType == int.class)
				return Integer.parseInt(trimmed);
			if (targetType == Long.class || targetType == long.class)
				return Long.parseLong(trimmed);
			if (targetType == BigInteger.class)
				return new BigInteger(trimmed);
			if (targetType == Float.class || targetType == float.class)
				return Float.parseFloat(decimalDot(trimmed));
			if (targetType == Double.class || targetType == double.class)
				return Double.parseDouble(decimalDot(trimmed));
			if (targetType == BigDecimal.class)
				return new BigDecimal(decimalDot(trimmed));
		} catch (NumberFormatException e) {
			return null;
		}
		if (targetType == Boolean.class || targetType == boolean.class)
			return Boolean.parseBoolean(trimmed);
		if (targetType.isEnum())
			return enumConstant(trimmed, targetType);
		if (targetType == LocalDate.class)
			return TemplateDateValues.tryParseDate(trimmed);
		if (targetType == LocalDateTime.class)
			return TemplateDateValues.tryParseDateTime(trimmed);
		return raw;
	}

	/**
	 * A decimal number typed in any locale: a decimal dot or comma ({@code "1.5"},
	 * {@code "1,5"}); {@code null} when blank or not a number.
	 */
	public static Double parseDecimal(String raw) {
		if (raw == null || raw.trim().isEmpty())
			return null;
		try {
			return Double.parseDouble(decimalDot(raw.trim()));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String decimalDot(String s) {
		return s.replace(',', '.');
	}

	private static Object enumConstant(String name, Class<?> enumType) {
		String normalized = name.replace('-', '_').toUpperCase(Locale.ROOT);
		for (Object constant : enumType.getEnumConstants()) {
			if (((Enum<?>) constant).name().toUpperCase(Locale.ROOT).equals(normalized))
				return constant;
		}
		return null;
	}

	/**
	 * Coerces {@code raw} to match {@code existingValue}'s runtime type (any
	 * numeric type {@link #coerce} supports, plus {@code Boolean}/{@code Map},
	 * {@code LocalDate}/{@code LocalDateTime} and enums). Falls back to {@code raw}
	 * unchanged when {@code existingValue} is {@code null}, of some other type, or
	 * when coercion fails — an override always lands, it just stays untyped on
	 * failure rather than being dropped.
	 */
	public static Object coerceToMatch(String raw, Object existingValue) {
		if (existingValue == null)
			return raw;
		if (existingValue instanceof Map)
			return withFallback(coerce(raw, Map.class), raw);
		if (existingValue instanceof Enum)
			return withFallback(coerce(raw, ((Enum<?>) existingValue).getDeclaringClass()), raw);
		Class<?> existingType = existingValue.getClass();
		if (!isCoercibleType(existingType))
			return raw;
		return withFallback(coerce(raw, existingType), raw);
	}

	private static boolean isCoercibleType(Class<?> type) {
		return type == Integer.class || type == Long.class || type == BigInteger.class || type == Float.class
				|| type == Double.class || type == BigDecimal.class || type == Boolean.class || type == LocalDate.class
				|| type == LocalDateTime.class;
	}

	private static Object withFallback(Object coerced, String raw) {
		return coerced != null ? coerced : raw;
	}
}
