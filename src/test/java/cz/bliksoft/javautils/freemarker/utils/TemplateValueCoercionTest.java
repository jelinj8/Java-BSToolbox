package cz.bliksoft.javautils.freemarker.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TemplateValueCoercionTest {

	enum Mode {
		DISABLED, WHEN_NEEDED, ALWAYS
	}

	@Test
	void parsesDecimalWithDotOrComma() {
		assertEquals(1.5, TemplateValueCoercion.parseDecimal("1.5"));
		assertEquals(1.5, TemplateValueCoercion.parseDecimal("1,5"));
		assertEquals(-2.0, TemplateValueCoercion.parseDecimal(" -2 "));
		assertNull(TemplateValueCoercion.parseDecimal("x"));
		assertNull(TemplateValueCoercion.parseDecimal(" "));
		assertNull(TemplateValueCoercion.parseDecimal(null));
		assertEquals(84.5, TemplateValueCoercion.coerce("84,5", Double.class));
	}

	@Test
	void decimalTemplateDefault() {
		Map<String, Object> defaults = TemplateParameterUtils
				.extractDefaultVariables("{var|DECIMAL|w|Width|60,5}\n{var|DECIMAL|h|Height}");
		assertEquals(60.5, defaults.get("w"));
		assertEquals(0.0, defaults.get("h"));
	}

	@Test
	void coercesEnumTarget() {
		assertEquals(Mode.ALWAYS, TemplateValueCoercion.coerce("ALWAYS", Mode.class));
		assertEquals(Mode.ALWAYS, TemplateValueCoercion.coerce(" always ", Mode.class));
		assertEquals(Mode.WHEN_NEEDED, TemplateValueCoercion.coerce("when-needed", Mode.class));
		assertNull(TemplateValueCoercion.coerce("sometimes", Mode.class));
		assertNull(TemplateValueCoercion.coerce("", Mode.class));
		assertNull(TemplateValueCoercion.coerce(null, Mode.class));
	}

	@Test
	void coercesToMatchEnum() {
		assertEquals(Mode.DISABLED, TemplateValueCoercion.coerceToMatch("disabled", Mode.ALWAYS));
		assertEquals("sometimes", TemplateValueCoercion.coerceToMatch("sometimes", Mode.ALWAYS));
	}

	@Test
	void coercesIntegerTarget() {
		assertEquals(300, TemplateValueCoercion.coerce("300", Integer.class));
	}

	@Test
	void coercesLongTarget() {
		assertEquals(3_000_000_000L, TemplateValueCoercion.coerce("3000000000", Long.class));
	}

	@Test
	void coercesBigIntegerTarget() {
		assertEquals(new BigInteger("123456789012345678901234567890"),
				TemplateValueCoercion.coerce("123456789012345678901234567890", BigInteger.class));
	}

	@Test
	void coercesFloatTarget() {
		assertEquals(84.5f, TemplateValueCoercion.coerce("84.5", Float.class));
	}

	@Test
	void coercesDoubleTarget() {
		assertEquals(84.5, TemplateValueCoercion.coerce("84.5", Double.class));
	}

	@Test
	void coercesBigDecimalTarget() {
		assertEquals(new BigDecimal("84.500000000000000001"),
				TemplateValueCoercion.coerce("84.500000000000000001", BigDecimal.class));
	}

	@Test
	void coercesBooleanTarget() {
		assertEquals(true, TemplateValueCoercion.coerce("true", Boolean.class));
		assertEquals(false, TemplateValueCoercion.coerce("nope", Boolean.class));
	}

	@Test
	void coercesMapTargetPreservingOrder() {
		Object result = TemplateValueCoercion.coerce("a=1;b=2", Map.class);
		assertTrue(result instanceof Map);
		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String, String>) result;
		assertEquals("[a, b]", map.keySet().toString());
		assertEquals("1", map.get("a"));
	}

	@Test
	void unparsableNumericTargetReturnsNull() {
		assertNull(TemplateValueCoercion.coerce("abc", Integer.class));
		assertNull(TemplateValueCoercion.coerce("abc", Long.class));
		assertNull(TemplateValueCoercion.coerce("abc", BigInteger.class));
		assertNull(TemplateValueCoercion.coerce("abc", Float.class));
		assertNull(TemplateValueCoercion.coerce("abc", Double.class));
		assertNull(TemplateValueCoercion.coerce("abc", BigDecimal.class));
	}

	@Test
	void unhandledTargetTypeReturnsRawString() {
		assertEquals("hello", TemplateValueCoercion.coerce("hello", String.class));
	}

	@Test
	void coerceToMatchRetypesAgainstExistingInteger() {
		assertEquals(400, TemplateValueCoercion.coerceToMatch("400", 300));
	}

	@Test
	void coerceToMatchRetypesAgainstExistingDouble() {
		assertEquals(12.5, TemplateValueCoercion.coerceToMatch("12.5", 40.0));
	}

	@Test
	void coerceToMatchRetypesAgainstExistingLong() {
		assertEquals(4_000_000_000L, TemplateValueCoercion.coerceToMatch("4000000000", 1L));
	}

	@Test
	void coerceToMatchRetypesAgainstExistingBigInteger() {
		assertEquals(new BigInteger("999999999999999999999"),
				TemplateValueCoercion.coerceToMatch("999999999999999999999", BigInteger.ONE));
	}

	@Test
	void coerceToMatchRetypesAgainstExistingFloat() {
		assertEquals(12.5f, TemplateValueCoercion.coerceToMatch("12.5", 1.0f));
	}

	@Test
	void coerceToMatchRetypesAgainstExistingBigDecimal() {
		assertEquals(new BigDecimal("12.50"), TemplateValueCoercion.coerceToMatch("12.50", BigDecimal.ONE));
	}

	@Test
	void coerceToMatchRetypesAgainstExistingBoolean() {
		assertEquals(true, TemplateValueCoercion.coerceToMatch("true", false));
	}

	@Test
	void coerceToMatchFallsBackToRawOnNullExistingValue() {
		assertEquals("300", TemplateValueCoercion.coerceToMatch("300", null));
	}

	@Test
	void coerceToMatchFallsBackToRawOnUnhandledExistingType() {
		assertEquals("300", TemplateValueCoercion.coerceToMatch("300", "already a string"));
	}

	@Test
	void coerceToMatchFallsBackToRawOnParseFailure() {
		assertEquals("abc", TemplateValueCoercion.coerceToMatch("abc", 300));
	}

	@Test
	void coercesDates() {
		assertEquals(LocalDate.of(2026, 10, 2), TemplateValueCoercion.coerce(" 2026-10-02 ", LocalDate.class));
		assertEquals(LocalDate.now(), TemplateValueCoercion.coerce("today", LocalDate.class));
		assertEquals(LocalDateTime.of(2026, 10, 2, 10, 0),
				TemplateValueCoercion.coerce("2026-10-02 10:00", LocalDateTime.class));
		assertNull(TemplateValueCoercion.coerce("soon", LocalDate.class));
		assertEquals(LocalDateTime.of(2026, 10, 2, 10, 0),
				TemplateValueCoercion.coerceToMatch("2026-10-02T10:00", LocalDateTime.of(2000, 1, 1, 0, 0)));
		assertEquals("soon", TemplateValueCoercion.coerceToMatch("soon", LocalDate.of(2000, 1, 1)));
	}
}
