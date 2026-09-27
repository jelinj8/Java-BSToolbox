package cz.bliksoft.javautils.freemarker.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

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

	static {
		FileSystem.addTranslation("test/templateParams/translated", "Translated");
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
}
