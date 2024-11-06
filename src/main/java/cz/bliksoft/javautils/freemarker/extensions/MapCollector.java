package cz.bliksoft.javautils.freemarker.extensions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class MapCollector implements TemplateMethodModelEx {

	public MapCollector() {
		result = "";
	}

	public MapCollector(String result) {
		this.result = result;
	}

	private String result;
	private Map<String, String> values = new HashMap<>();

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arg0) throws TemplateModelException {
		values.put(String.valueOf(arg0.get(0)), String.valueOf(arg0.get(1)));
		return result;
	}

	public void clear() {
		values.clear();
	}

	public Map<String, String> getValues() {
		return values;
	}

	public String getValue(String key) {
		return values.get(key);
	}
}
