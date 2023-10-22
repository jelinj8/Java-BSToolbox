package cz.bliksoft.javautils.freemarker.extensions.global;

import java.util.List;

import com.sun.istack.logging.Logger;

import cz.bliksoft.javautils.ObjectUtils;
import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.xml.XmlUtils;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import jakarta.xml.bind.JAXBException;

public class DescribeVariable implements TemplateMethodModelEx {
	Logger log = Logger.getLogger(DescribeVariable.class);

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arg0) throws TemplateModelException {
		if (arg0.size() < 1)
			throw new TemplateModelException("Expecting a value to describe");
		String prefixFirst = null;
		String prefixNext = null;
		String prefixLast = null;

		if (arg0.size() > 1) {
			prefixFirst = arg0.get(1).toString();
			prefixNext = prefixFirst;
			prefixLast = prefixFirst;
		}
		if (arg0.size() > 2) {
			prefixNext = arg0.get(2).toString();
		}
		if (arg0.size() > 3) {
			prefixLast = arg0.get(3).toString();
		}
		Object arg = arg0.get(0);
		if (arg instanceof WrapperTemplateModel) {
			WrapperTemplateModel a = (WrapperTemplateModel) arg;
			Object o = a.getWrappedObject();
			if (prefixFirst != null)
				return StringUtils.prependLines(ObjectUtils.describe(o, ""), prefixFirst, prefixNext, prefixLast);
			else
				return ObjectUtils.describe(o, "");
		} else {
			if (prefixFirst != null)
				return StringUtils.prependLines(ObjectUtils.describe(arg, ""), prefixFirst, prefixNext, prefixLast);
			else
				return ObjectUtils.describe(arg, "");

		}
	}
}
