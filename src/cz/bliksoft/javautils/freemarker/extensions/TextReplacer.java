package cz.bliksoft.javautils.freemarker.extensions;

import java.util.List;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class TextReplacer implements TemplateMethodModelEx {

	private String[] params;

	public TextReplacer(String... args) {
		this.params = args;
	}

	@SuppressWarnings("rawtypes")
	public Object exec(List args) throws TemplateModelException {
		String result = null;
		if (args.size() == 1) {

			try {
				Object arg = args.get(0);
				if (arg == null)
					return "";
				String text;
				text = arg.toString();

				for (int i = 0; i < params.length; i = i + 2) {
					text = text.replaceAll(params[i], params[i + 1]);
				}

				result = text;
			} catch (Exception e) {
				result = "Error while replacing in string: " + e.getMessage();
				e.printStackTrace();
			}
		}
		return result;
	}

}
