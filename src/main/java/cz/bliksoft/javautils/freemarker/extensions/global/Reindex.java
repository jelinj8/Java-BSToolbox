package cz.bliksoft.javautils.freemarker.extensions.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import freemarker.template.DefaultListAdapter;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class Reindex implements TemplateMethodModelEx {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object exec(List args) throws TemplateModelException {
		HashMap<String, Object> result = null;
		if (args.size() < 2)
			throw new TemplateModelException(
					"Incorrect parameter count for reindexing, expected iterable list of Map<String, ?>, groupcol1, [groupcol2]...");

		DefaultListAdapter wrapper = (DefaultListAdapter) args.get(0);

		Iterable<Map<String, Object>> listOfMaps = (Iterable<Map<String, Object>>) wrapper.getWrappedObject();

		ArrayList<String> mappingColumns = new ArrayList<>();
		for (int i = 1; i < args.size(); i++)
			mappingColumns.add(args.get(i).toString());

		result = new HashMap<String, Object>();
		for (Map<String, Object> _hashMap : listOfMaps) {
			Map<String, Object> target = result;
//			List<Map<String, Object>> targetList;
			String targetKey = null;
			int level = 0;
			for (String keyColumn : mappingColumns) {
				level++;

				targetKey = Objects.toString(_hashMap.get(keyColumn), null);
				if (targetKey == null)
					targetKey = "_NULL_";
				if (target.containsKey(targetKey))
					if (level == mappingColumns.size()) {
						StringBuilder sb = new StringBuilder();
						for (String kc : mappingColumns) {
							sb.append(" ");
							sb.append(kc);
							sb.append("='");
							sb.append(_hashMap.get(kc));
							sb.append("'");
						}
						throw new TemplateModelException("Duplicate keys:" + sb.toString());
					} else {
						target = (Map<String, Object>) target.get(targetKey);
					}
				else {
					if (level == mappingColumns.size()) {
						target.put(targetKey, _hashMap);
					} else {
						Map<String, Object> newMap = new HashMap<>();
						target.put(targetKey, newMap);
						target = newMap;
					}
				}
			}
		}

		return result;
	}
}
