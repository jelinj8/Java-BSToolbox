package cz.bliksoft.javautils.freemarker.extensions;

import java.util.ArrayList;
import java.util.List;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class StringCollector implements TemplateMethodModelEx {

	public StringCollector() {
		result = "";
	}

	public StringCollector(String result) {
		this.result = result;
	}

	private String result;
	private List<String> values = new ArrayList<>();

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arg0) throws TemplateModelException {
		for (Object o : arg0) {
			values.add(String.valueOf(o));
		}
		return result;
	}

	public void clear() {
		values.clear();
	}

	public List<String> getValues() {
		return values;
	}

	public String getValue() {
		return values.get(0);
	}
}
