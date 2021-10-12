package cz.bliksoft.javautils.freemarker.extensions.global;

import java.util.List;

import cz.bliksoft.javautils.barcodes.Code128;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class Code128Width implements TemplateMethodModelEx {

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List args) throws TemplateModelException {
		if (args.size() == 1) {
			return Code128.getWidth(args.get(0).toString());
		}
		return "";
	}

}
