package cz.bliksoft.javautils.freemarker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import freemarker.template.Configuration;

class FreemarkerGeneratorSetVariableCoercedTest {

	private FreemarkerGenerator newGenerator() {
		return new FreemarkerGenerator(new Configuration(Configuration.VERSION_2_3_34));
	}

	@Test
	void retypesStringOverrideToMatchExistingInteger() {
		FreemarkerGenerator gen = newGenerator();
		gen.setVariable("dpi", 203);
		gen.setVariableCoerced("dpi", "300");
		assertEquals(300, gen.getVariable("dpi"));
	}

	@Test
	void passesThroughAlreadyTypedValueUnchanged() {
		FreemarkerGenerator gen = newGenerator();
		gen.setVariable("dpi", 203);
		gen.setVariableCoerced("dpi", 300);
		assertEquals(300, gen.getVariable("dpi"));
	}

	@Test
	void fallsBackToRawStringWhenNoExistingValue() {
		FreemarkerGenerator gen = newGenerator();
		gen.setVariableCoerced("printMode", "APPLICATOR");
		assertEquals("APPLICATOR", gen.getVariable("printMode"));
	}
}
