package cz.bliksoft.javautils.freemarker.extensions.global;

import java.util.List;

import cz.bliksoft.javautils.dialogs.ValueDLG;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class GUIPrompt implements TemplateMethodModelEx {
	public GUIPrompt() {
	}

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {
		String prompt = String.valueOf(arguments.get(0));
		String title = null;
		String currentValue = null;
		String cancelValue = null;

		if (arguments.size() > 1)
			title = String.valueOf(arguments.get(1));
		if (arguments.size() > 2)
			currentValue = String.valueOf(arguments.get(2));
		if (arguments.size() > 3)
			cancelValue = String.valueOf(arguments.get(3));

		try {
			String result = ValueDLG.prompt(title, prompt, currentValue, cancelValue);
			return result;
		} catch (Exception e) {
			throw new TemplateModelException("Failed to execute prompt for value", e);
		}
	}

}
