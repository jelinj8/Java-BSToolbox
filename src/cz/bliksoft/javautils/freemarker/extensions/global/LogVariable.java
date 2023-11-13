package cz.bliksoft.javautils.freemarker.extensions.global;

import java.util.List;
import java.util.logging.Logger;

import cz.bliksoft.javautils.ObjectUtils;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class LogVariable implements TemplateMethodModelEx {
	Logger log = Logger.getLogger(LogVariable.class.getName());

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arg0) throws TemplateModelException {
		if (arg0.size() < 1)
			return "";
		freemarker.ext.util.WrapperTemplateModel a = (WrapperTemplateModel) arg0.get(0);
		Object o = a.getWrappedObject();
		log.info("Describe variable:\n" + ObjectUtils.describe(o, null));
		return "";
	}
}
