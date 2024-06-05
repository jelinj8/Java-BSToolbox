package cz.bliksoft.javautils.freemarker.extensions.global;

import java.util.List;
import java.util.logging.Logger;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class CMDWrite implements TemplateMethodModelEx {
	Logger log = Logger.getLogger(CMDWrite.class.getName());

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {
		String prompt = String.valueOf(arguments.get(0));
		System.out.println(prompt);
		return "";
	}

}
