package cz.bliksoft.javautils.freemarker.extensions;

import java.util.List;

import cz.bliksoft.javautils.barcodes.Code128;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class Code128Encode implements TemplateMethodModelEx {

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List args) throws TemplateModelException {
		if (args.size() == 1) {
			return Code128.codeIt(args.get(0).toString());
		}
		return "";
	}

}
