package cz.bliksoft.javautils.freemarker.extensions;

import java.util.List;

import cz.bliksoft.javautils.StringUtils;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class SwitchTemplate implements TemplateMethodModelEx {

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List args) throws TemplateModelException {
		return StringUtils.format("<a href=\"COMMAND:SET_TEMPLATE:{0}\">{1}</a>", args.get(0), args.get(1)); //$NON-NLS-1$
	}

}
