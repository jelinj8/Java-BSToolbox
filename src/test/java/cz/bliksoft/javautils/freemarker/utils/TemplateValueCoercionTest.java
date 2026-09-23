package cz.bliksoft.javautils.freemarker.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TemplateValueCoercionTest {

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
}
