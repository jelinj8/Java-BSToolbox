package cz.bliksoft.javautils.freemarker.extensions;

import java.util.List;

import cz.bliksoft.javautils.StringUtils;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class HtmlPreformat implements TemplateMethodModelEx {

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arg0) throws TemplateModelException {
		return StringUtils.preformatForHTML(String.valueOf(arg0.get(0)));
	}

}
