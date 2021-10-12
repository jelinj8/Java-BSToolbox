package cz.bliksoft.javautils.freemarker.extensions.global;

import java.util.List;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class ImageResource implements TemplateMethodModelEx {

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List args) throws TemplateModelException {
		return "resource:" + args.get(0);
	}

}
