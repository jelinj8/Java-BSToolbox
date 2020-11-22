package cz.bliksoft.javautils.freemarker.extensions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import freemarker.template.DefaultListAdapter;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class Regroup implements TemplateMethodModelEx {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object exec(List args) throws TemplateModelException {
		HashMap<String, Object> result = null;
		if (args.size() < 2)
			throw new TemplateModelException(
					"Incorrect parameter count for regrouping, expected iterable list of Map<String, ?>, groupcol1, [groupcol2]...");

		DefaultListAdapter wrapper = (DefaultListAdapter) args.get(0);

		Iterable<HashMap<String, Object>> listOfMaps = (Iterable<HashMap<String, Object>>) wrapper.getWrappedObject();

		ArrayList<String> mappingColumns = new ArrayList<>();
		for (int i = 1; i < args.size(); i++)
			mappingColumns.add(args.get(i).toString());

		result = new HashMap<String, Object>();

		for (HashMap<String, Object> _hashMap : listOfMaps) {
			HashMap<String, Object> target = result;
			String targetKey = null;
			for (String keyColumn : mappingColumns) {
				targetKey = Objects.toString(_hashMap.get(keyColumn), null);
				if (targetKey == null)
					targetKey = "_NULL_";
				if (target.containsKey(targetKey))
					target = (HashMap<String, Object>) target.get(targetKey);
				else {
					HashMap<String, Object> newMap = new HashMap<>();
					target.put(targetKey, newMap);
					target = newMap;
				}
			}
			target.putAll(_hashMap);
		}

		return result;
	}
}
