package cz.bliksoft.javautils.freemarker.extensions.global;

import java.util.List;

import com.sun.istack.logging.Logger;

import cz.bliksoft.javautils.ObjectUtils;
import cz.bliksoft.javautils.xml.XmlUtils;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import jakarta.xml.bind.JAXBException;

public class LogVariable implements TemplateMethodModelEx {
	Logger log = Logger.getLogger(LogVariable.class);

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
