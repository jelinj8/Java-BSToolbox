package cz.bliksoft.javautils.freemarker.extensions.global;

import java.util.List;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class IdentifyObjectType implements TemplateMethodModelEx {

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arg0) throws TemplateModelException {
		Object o = arg0.get(0);
		if (o == null)
			return "NULL";
		else
			return o.getClass().getName();
	}

}
