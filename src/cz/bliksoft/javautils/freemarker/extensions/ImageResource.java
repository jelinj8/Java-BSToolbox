package cz.bliksoft.javautils.freemarker.extensions;

import java.util.List;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class ImageResource implements TemplateMethodModelEx {

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List args) throws TemplateModelException {
		if (args.size() == 2) {
			return "resource:base/" + args.get(0) + "_" + args.get(1) + ".png";
		} else
			return "resource:" + args.get(0);
		//		return ImageUtils.getIconUrl(String.valueOf(args.get(0)));
	}

}
