package cz.bliksoft.javautils.freemarker.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import cz.bliksoft.javautils.freemarker.utils.TemplateParameterUtils.TemplateParameter;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;

class TemplateParameterUtilsTest {

	private static final String SOURCE = String.join("\n", //
			"<#--", //
			"{var|comment|-|Literal comment", //
			"on two lines}", //
			"{var|comment|-|test/templateParams/translated|fallback A}", //
			"{var|comment|-|test/templateParams/missing|fallback B}", //
			"{var|csvfile|list|list||:.csv}", //
			"{var|hint|-|test/templateParams/translated|x}", //
			"{var|hint|-|-|literal hint}", //
			"{var|string|txt|text||40}", //
			"-->");

	@BeforeAll
	static void translations() throws Exception {
		FileSystem.addTranslation("test/templateParams/translated", "Translated");
		String xml = "<root xmlns=\"http://bliksoft.cz/XmlFilesystem\">\n" //
				+ "<file name=\"translations\"><file name=\"test\"><file name=\"localized\">\n" //
				+ "  <file name=\"text\" translation=\"test/localized/text\">\n" //
				+ "    <attribute name=\"default\" value=\"A\"/>\n" //
				+ "    <attribute name=\"cs\" value=\"B\"/>\n" //
				+ "    <attribute name=\"cs_CZ\" value=\"C\"/>\n" //
				+ "  </file>\n" //
				+ "</file></file></file>\n" //
				+ "<file name=\"testLocalized\">\n" //
				+ "  <attribute name=\"label\" translation=\"test/localized/text\" value=\"raw\"/>\n" //
				+ "</file>\n" //
				+ "</root>\n";
		FileSystem.getDefault().importXml(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
				"TemplateParameterUtilsTest");
	}

	@Test
	void translatedTitles() {
		List<TemplateParameter> params = TemplateParameterUtils.parseFormParameters(String.join("\n", //
				"{var|int|a|:test/localized/text:fallback A|1|1:9}", //
				"{var|int|b|:test/localized/missing:fallback: with colon|1}", //
				"{var|int|c|:test/localized/missing|1}", //
				"{var|info|-|:test/localized/text|name}", //
				"{var|string|d|plain: not a key|}", //
				"{var|comment|-|:test/localized/text:comment}"), new Locale("cs"));
		assertEquals("B", params.get(0).getTitle());
		assertEquals("1:9", params.get(0).getParameters());
		assertEquals("fallback: with colon", params.get(1).getTitle());
		assertEquals("test/localized/missing", params.get(2).getTitle());
		assertEquals("B", params.get(3).getTitle());
		assertEquals("name", params.get(3).getDefaultValue());
		assertEquals("plain: not a key", params.get(4).getTitle());
		assertEquals("B", params.get(5).getTitle());
		// raw parsing keeps the declaration as written
		assertEquals(":test/localized/text:fallback A", TemplateParameterUtils
				.parseParameters("{var|int|a|:test/localized/text:fallback A|1}").get(0).getTitle());
	}

	@Test
	void translationsByLocale() {
		assertEquals("A", FileSystem.getTranslation("test/localized/text", Locale.ENGLISH));
		assertEquals("B", FileSystem.getTranslation("test/localized/text", new Locale("cs")));
		assertEquals("C", FileSystem.getTranslation("test/localized/text", new Locale("cs", "CZ")));
		assertNull(FileSystem.getTranslation("test/localized/missing", Locale.ENGLISH));
		// no file: the in-code translations
		assertEquals("Translated", FileSystem.getTranslation("test/templateParams/translated", Locale.ENGLISH));

		assertEquals("B", TemplateParameterUtils.resolveText("test/localized/text", "f", new Locale("cs")));
		assertEquals("f", TemplateParameterUtils.resolveText("test/localized/missing", "f", new Locale("cs")));
		List<TemplateParameter> params = TemplateParameterUtils.parseFormParameters(
				"{var|csvfile|list|list||:.csv}\n{var|hint|-|test/localized/text|x}", Locale.ENGLISH);
		assertEquals("A", params.get(0).getHint());

		assertEquals("C",
				FileSystem.getFile("testLocalized").getLocalizedAttribute("label", null, new Locale("cs", "CZ")));
	}

	@Test
	void formParametersResolveCommentsAndFoldHints() {
		List<TemplateParameter> params = TemplateParameterUtils.parseFormParameters(SOURCE);
		assertEquals(5, params.size());

		assertEquals("Literal comment\non two lines", params.get(0).getTitle());
		assertEquals("Translated", params.get(1).getTitle());
		assertNull(params.get(1).getDefaultValue());
		assertEquals("fallback B", params.get(2).getTitle());

		TemplateParameter csv = params.get(3);
		assertEquals("list", csv.getName());
		assertEquals("Translated\nliteral hint", csv.getHint());
		assertEquals(":.csv", csv.getParameters());

		assertNull(params.get(4).getHint());
	}

	@Test
	void leadingHintIsDropped() {
		List<TemplateParameter> params = TemplateParameterUtils
				.parseFormParameters("{var|hint|-|-|orphan}\n{var|boolean|debug|debug|false}");
		assertEquals(1, params.size());
		assertNull(params.get(0).getHint());
	}

	@Test
	void resolveTextFallsBack() {
		assertEquals("Translated", TemplateParameterUtils.resolveText("test/templateParams/translated", "f"));
		assertEquals("f", TemplateParameterUtils.resolveText("test/templateParams/missing", "f"));
		assertEquals("f", TemplateParameterUtils.resolveText("-", "f"));
		assertEquals("f", TemplateParameterUtils.resolveText(null, "f"));
	}

	@Test
	void defaultsSkipCommentsAndHints() {
		Map<String, Object> defaults = TemplateParameterUtils.extractDefaultVariables(SOURCE);
		assertEquals(1, defaults.size());
		assertEquals("", defaults.get("txt"));
		assertFalse(defaults.containsKey("-"));
	}

	@Test
	void dateDefaults() {
		Map<String, Object> defaults = TemplateParameterUtils.extractDefaultVariables(
				"{var|date|d|Date|2026-10-02|today..}\n{var|DATETIME|t|Time|2026-10-02 08:30}\n"
						+ "{var|date|e|Empty|}\n{var|date|r|Relative|today+7}");
		assertEquals(java.time.LocalDate.of(2026, 10, 2), defaults.get("d"));
		assertEquals(java.time.LocalDateTime.of(2026, 10, 2, 8, 30), defaults.get("t"));
		assertTrue(defaults.containsKey("e"));
		assertNull(defaults.get("e"));
		assertEquals(java.time.LocalDate.now().plusDays(7), defaults.get("r"));
		assertEquals("today..",
				TemplateParameterUtils.parseParameters("{var|date|d|Date|2026-10-02|today..}").get(0).getParameters());
		assertEquals("DATE", TemplateParameterUtils.parseParameters("{var|date|d|Date}").get(0).getNormalizedType());
	}
}
