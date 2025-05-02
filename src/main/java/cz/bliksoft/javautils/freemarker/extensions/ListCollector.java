package cz.bliksoft.javautils.freemarker.extensions;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class ListCollector implements TemplateMethodModelEx, Iterable<Object> {

	public ListCollector() {
		result = "";
	}

	public ListCollector(String result) {
		this.result = result;
	}

	private String result;
	private List<Object> values = new LinkedList<>();

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arg0) throws TemplateModelException {
		values.add(arg0.get(0));
		return result;
	}

	public void clear() {
		values.clear();
	}

	public List<Object> getValues() {
		return values;
	}

	@Override
	public Iterator<Object> iterator() {
		return values.iterator();
	}
}
