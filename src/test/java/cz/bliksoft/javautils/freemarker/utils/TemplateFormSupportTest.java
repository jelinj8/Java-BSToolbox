package cz.bliksoft.javautils.freemarker.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import cz.bliksoft.javautils.freemarker.utils.TemplateFormSupport.Option;

class TemplateFormSupportTest {

	private static List<Option> opts(String... valueLabel) {
		Option[] result = new Option[valueLabel.length / 2];
		for (int i = 0; i < result.length; i++)
			result[i] = new Option(valueLabel[2 * i], valueLabel[2 * i + 1]);
		return Arrays.asList(result);
	}

	@Test
	void declaredOptions() {
		assertEquals(opts("a", "a", "b c", "b c"), TemplateFormSupport.declaredOptions(" a ; b c ;"));
		assertEquals(opts("a", "a", "b", "b"), TemplateFormSupport.declaredOptions("a,b"));
		// ';' wins - a comma stays in the value
		assertEquals(opts("1,5", "1,5", "2", "2"), TemplateFormSupport.declaredOptions("1,5;2"));
		// declared options are never key=label
		assertEquals(opts("x=1", "x=1"), TemplateFormSupport.declaredOptions("x=1"));
		assertEquals(Collections.emptyList(), TemplateFormSupport.declaredOptions(null));
	}

	@Test
	void optionsFromText() {
		assertEquals(opts("S", "S", "M", "M"), TemplateFormSupport.parseOptionsString("S;M"));
		assertEquals(opts("r", "Red", "b", "Blue"), TemplateFormSupport.parseOptionsString("r=Red; b = Blue"));
		// not all pairs - plain values
		assertEquals(opts("r=Red", "r=Red", "b", "b"), TemplateFormSupport.parseOptionsString("r=Red;b"));
	}

	@Test
	void optionsFromModelObjects() {
		Map<Object, Object> map = new LinkedHashMap<>();
		map.put(3, "three");
		map.put(1, "one");
		map.put(2, null);
		assertEquals(opts("3", "three", "1", "one", "2", "2"), TemplateFormSupport.toOptions(map));
		assertEquals(opts("x", "x", "1", "1"), TemplateFormSupport.toOptions(Arrays.asList("x", 1)));
		assertEquals(opts("x", "x", "y", "y"), TemplateFormSupport.toOptions(new String[] { "x", "y" }));
		assertEquals(opts("x", "X"), TemplateFormSupport.toOptions("x=X"));
		assertNull(TemplateFormSupport.toOptions(null));

		Map<String, Object> model = new HashMap<>();
		model.put("size_options", "S;M");
		assertEquals(opts("S", "S", "M", "M"), TemplateFormSupport.modelOptions(model, "size"));
		assertNull(TemplateFormSupport.modelOptions(model, "color"));
		assertNull(TemplateFormSupport.modelOptions(null, "size"));
	}

	@Test
	void resolvedOptions() {
		Map<String, Object> model = new HashMap<>();
		model.put("c_options", Arrays.asList("m1", "m2"));
		List<String> fonts = Arrays.asList("0", "A");
		// COMBO: model > resolver > declared
		assertEquals(opts("m1", "m1", "m2", "m2"),
				TemplateFormSupport.resolveOptions("combo", "c", "a;b", fonts, model));
		assertEquals(opts("0", "0", "A", "A"), TemplateFormSupport.resolveOptions("COMBO", "c", "a;b", fonts, null));
		assertEquals(opts("a", "a", "b", "b"), TemplateFormSupport.resolveOptions("COMBO", "c", "a;b", null, null));
		// FONT: fonts > model > declared
		assertEquals(opts("0", "0", "A", "A"), TemplateFormSupport.resolveOptions("FONT", "c", "a", fonts, model));
		assertEquals(opts("m1", "m1", "m2", "m2"),
				TemplateFormSupport.resolveOptions("FONT", "c", "a", Collections.<String>emptyList(), model));
		assertEquals(opts("a", "a"), TemplateFormSupport.resolveOptions("FONT", "c", "a", null, null));
		assertEquals(Collections.emptyList(), TemplateFormSupport.resolveOptions("STRING", "c", "a", fonts, model));
	}

	@Test
	void formValues() {
		assertEquals("5", TemplateFormSupport.formValue("INT", 5L));
		assertEquals("10", TemplateFormSupport.formValue("INT", 10.0));
		assertNull(TemplateFormSupport.formValue("INT", 1.5));
		assertNull(TemplateFormSupport.formValue("INT", "x"));
		assertEquals("1.50", TemplateFormSupport.formValue("DECIMAL", new BigDecimal("1.50")));
		assertEquals("10000000", TemplateFormSupport.formValue("DECIMAL", 1e7));
		assertEquals("0.5", TemplateFormSupport.formValue("DECIMAL", 0.5f));
		assertEquals("1,5", TemplateFormSupport.formValue("DECIMAL", "1,5"));
		assertNull(TemplateFormSupport.formValue("DECIMAL", "abc"));
		assertEquals("true", TemplateFormSupport.formValue("BOOLEAN", "1"));
		assertEquals("false", TemplateFormSupport.formValue("BOOLEAN", false));
		assertEquals("2026-10-02", TemplateFormSupport.formValue("DATE", LocalDateTime.of(2026, 10, 2, 8, 0)));
		assertEquals("2026-10-02T00:00", TemplateFormSupport.formValue("DATETIME", LocalDate.of(2026, 10, 2)));
		assertEquals(TemplateDateValues.formatDate(LocalDate.now().plusDays(7)),
				TemplateFormSupport.formValue("date", "today+7"));
		assertNull(TemplateFormSupport.formValue("DATE", "soon"));
		assertNull(TemplateFormSupport.formValue("CSVFILE", "x"));
		assertEquals("SECONDS", TemplateFormSupport.formValue("COMBO", java.util.concurrent.TimeUnit.SECONDS));
		assertEquals("", TemplateFormSupport.formValue("INT", " "));
		assertNull(TemplateFormSupport.formValue("STRING", null));
	}

	@Test
	void initialValuePrecedence() {
		Map<String, Object> model = new HashMap<>();
		model.put("n", 7);
		model.put("empty", null);
		// entered > model > default
		assertEquals("3", TemplateFormSupport.initialValue("INT", "n", "1", model, "3", null));
		assertEquals("7", TemplateFormSupport.initialValue("INT", "n", "1", model, null, null));
		assertEquals("1", TemplateFormSupport.initialValue("INT", "n", "1", null, null, null));
		assertEquals("1", TemplateFormSupport.initialValue("INT", "other", "1", model, null, null));
		// unusable candidates are skipped
		assertEquals("7", TemplateFormSupport.initialValue("INT", "n", "1", model, "x", null));
		// null in the model is empty
		assertEquals("", TemplateFormSupport.initialValue("STRING", "empty", "d", model, null, null));
		assertNull(TemplateFormSupport.initialValue("STRING", "x", null, null, null, null));
		// a date default is evaluated
		assertEquals(TemplateDateValues.formatDate(LocalDate.now()),
				TemplateFormSupport.initialValue("DATE", "d", "today", null, null, null));
	}

	@Test
	void initialComboValue() {
		List<String> values = Arrays.asList("1", "2", "3");
		Map<String, Object> model = new HashMap<>();
		model.put("c", 2);
		assertEquals("2", TemplateFormSupport.initialValue("COMBO", "c", "3", model, null, values));
		assertEquals("3", TemplateFormSupport.initialValue("COMBO", "c", "3", model, "3", values));
		// not an option - next candidate
		assertEquals("2", TemplateFormSupport.initialValue("COMBO", "c", "3", model, "9", values));
		model.put("c", 9);
		assertEquals("3", TemplateFormSupport.initialValue("COMBO", "c", "3", model, null, values));
		// nothing fits - the first option
		assertEquals("1", TemplateFormSupport.initialValue("COMBO", "c", "x", model, null, values));
	}

	@Test
	void typedValues() {
		assertEquals(5, TemplateFormSupport.typedValue("INT", " 5 "));
		assertEquals(1.5, TemplateFormSupport.typedValue("DECIMAL", "1,5"));
		assertEquals(Boolean.TRUE, TemplateFormSupport.typedValue("BOOLEAN", "true"));
		assertEquals(Boolean.FALSE, TemplateFormSupport.typedValue("BOOLEAN", "false"));
		assertEquals(LocalDate.of(2026, 1, 2), TemplateFormSupport.typedValue("DATE", "2026-01-02"));
		assertEquals(LocalDateTime.of(2026, 1, 2, 3, 4),
				TemplateFormSupport.typedValue("DATETIME", "2026-01-02T03:04"));
		assertEquals("x", TemplateFormSupport.typedValue("COMBO", "x"));
		assertNull(TemplateFormSupport.typedValue("INT", ""));
		assertThrows(IllegalArgumentException.class, () -> TemplateFormSupport.typedValue("INT", "x"));
		assertThrows(IllegalArgumentException.class, () -> TemplateFormSupport.typedValue("DECIMAL", "x"));
		assertThrows(IllegalArgumentException.class, () -> TemplateFormSupport.typedValue("DATE", "x"));
	}

	@Test
	void coercesToDeclaredTypes() {
		Map<String, Object> vars = new HashMap<>();
		vars.put("n", "5");
		vars.put("d", "2026-01-02");
		vars.put("bad", "x");
		vars.put("s", "5");
		vars.put("typed", 3);
		TemplateFormSupport.coerceToDeclaredTypes(
				TemplateParameterUtils.parseParameters(
						"{var|int|n|N}\n{var|date|d|D}\n{var|int|bad|B}\n" + "{var|string|s|S}\n{var|int|typed|T}"),
				vars);
		assertEquals(5, vars.get("n"));
		assertEquals(LocalDate.of(2026, 1, 2), vars.get("d"));
		assertEquals("x", vars.get("bad"));
		assertEquals("5", vars.get("s"));
		assertEquals(3, vars.get("typed"));
	}
}
