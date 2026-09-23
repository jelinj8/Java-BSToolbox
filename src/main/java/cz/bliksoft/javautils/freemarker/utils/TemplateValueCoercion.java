package cz.bliksoft.javautils.freemarker.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

/**
 * Coerces a raw {@code String} value (e.g. read from XML config, a form
 * field, or JSON) into a typed value, either against an explicitly declared
 * target type or against an already-set value's own runtime type. Used when
 * merging printer advanced-properties/template-variable layers into a
 * template's variable map, so a later {@code String} override honors the
 * type an earlier layer already established for the same variable name.
 *
 * <p>
 * This intentionally does not touch {@link TemplateParameterUtils#extractDefaultVariables},
 * which has its own, differently-behaved (fallback-to-zero-on-failure)
 * INT/BOOLEAN coercion for template-declared {@code {var|...}} defaults.
 */
public final class TemplateValueCoercion {

	private TemplateValueCoercion() {
	}

	/**
	 * Coerces {@code raw} to {@code targetType}. Returns {@code null} if
	 * {@code raw} is {@code null} or cannot be parsed into a numeric/boolean
	 * target type. {@code Map.class} is decoded via {@link MapPropertyCodec}
	 * (never {@code null}, empty map on blank input). Any other target type
	 * (including {@code String.class}) returns {@code raw} unchanged.
	 *
	 * <p>
	 * Supported numeric target types: {@code Integer}/{@code int}, {@code Long}/
	 * {@code long}, {@link BigInteger}, {@code Float}/{@code float}, {@code Double}/
	 * {@code double}, {@link BigDecimal}.
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
				return Float.parseFloat(trimmed);
			if (targetType == Double.class || targetType == double.class)
				return Double.parseDouble(trimmed);
			if (targetType == BigDecimal.class)
				return new BigDecimal(trimmed);
		} catch (NumberFormatException e) {
			return null;
		}
		if (targetType == Boolean.class || targetType == boolean.class)
			return Boolean.parseBoolean(trimmed);
		return raw;
	}

	/**
	 * Coerces {@code raw} to match {@code existingValue}'s runtime type (any
	 * numeric type {@link #coerce} supports, plus {@code Boolean}/{@code Map}).
	 * Falls back to {@code raw} unchanged when {@code existingValue} is
	 * {@code null}, of some other type, or when coercion fails — an override
	 * always lands, it just stays untyped on failure rather than being dropped.
	 */
	public static Object coerceToMatch(String raw, Object existingValue) {
		if (existingValue == null)
			return raw;
		if (existingValue instanceof Map)
			return withFallback(coerce(raw, Map.class), raw);
		Class<?> existingType = existingValue.getClass();
		if (!isCoercibleType(existingType))
			return raw;
		return withFallback(coerce(raw, existingType), raw);
	}

	private static boolean isCoercibleType(Class<?> type) {
		return type == Integer.class || type == Long.class || type == BigInteger.class || type == Float.class
				|| type == Double.class || type == BigDecimal.class || type == Boolean.class;
	}

	private static Object withFallback(Object coerced, String raw) {
		return coerced != null ? coerced : raw;
	}
}
