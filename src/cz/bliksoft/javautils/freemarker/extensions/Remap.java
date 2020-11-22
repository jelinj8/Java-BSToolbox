package cz.bliksoft.javautils.freemarker.extensions;

import java.util.HashMap;
import java.util.List;

import freemarker.template.DefaultListAdapter;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class Remap implements TemplateMethodModelEx {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object exec(List args) throws TemplateModelException {
		HashMap<String, HashMap<String, Object>> result = null;
		if (args.size() != 2)
			throw new TemplateModelException(
					"Incorrect parameter count for remapping, expected iterable, Map<String, ?>");

		DefaultListAdapter wrapper = (DefaultListAdapter) args.get(0);

		Iterable<HashMap<String, Object>> listOfMaps = (Iterable<HashMap<String, Object>>) wrapper.getWrappedObject();
		String mappingColumn = args.get(1).toString();

		result = new HashMap<String, HashMap<String, Object>>();

		for (HashMap<String, Object> _hashMap : listOfMaps) {
			Object key = _hashMap.get(mappingColumn);

			if (key == null)
				key = "_NULL_";

			result.put(key.toString(), _hashMap);
		}

		return result;
	}
}
