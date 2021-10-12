package cz.bliksoft.javautils.freemarker.extensions.local;

import java.util.List;

import cz.bliksoft.javautils.freemarker.FreemarkerGenerator;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class VariableRegistrator implements TemplateMethodModelEx {

	private FreemarkerGenerator generator;

	public VariableRegistrator(FreemarkerGenerator generator) {
		this.generator = generator;
	}

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arg0) throws TemplateModelException {
		generator.setVariable(arg0.get(0).toString(), arg0.get(1));
		return "";
	}
}
