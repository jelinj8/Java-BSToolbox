package cz.bliksoft.javautils.freemarker.extensions.local;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * Used to translate strings into unique numbers (e.g. for anchors in HTML
 * document)
 */
public class AnchorNumberer implements TemplateMethodModelEx {

	private Map<String, Integer> dictionary;
	Integer counter = 0;

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arg0) throws TemplateModelException {
		if (dictionary == null)
			dictionary = new HashMap<>();
		if (arg0.size() != 1)
			throw new TemplateModelException("Incorrect number of arguments!");

		String str = String.valueOf(arg0.get(0));
		Integer result = dictionary.putIfAbsent(str, counter);
		if (result == null) {
			result = counter++;
		}

		return result;
	}
}
