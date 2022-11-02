package cz.bliksoft.javautils.streams.xml.adapters;

import java.util.Map;
import java.util.TreeMap;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class StringMapAdapter extends XmlAdapter<StringMapElements[], Map<String, String>> {
	public StringMapAdapter() {
	}

	public StringMapElements[] marshal(Map<String, String> arg0) throws Exception {
		if (arg0 == null || arg0.isEmpty())
			return null;
		StringMapElements[] mapElements = new StringMapElements[arg0.size()];
		int i = 0;
		for (Map.Entry<String, String> entry : arg0.entrySet())
			mapElements[i++] = new StringMapElements(entry.getKey(), entry.getValue());

		return mapElements;
	}

	public Map<String, String> unmarshal(StringMapElements[] arg0) throws Exception {
		if (arg0 == null || arg0.length == 0)
			return null;
		Map<String, String> r = new TreeMap<String, String>();
		for (StringMapElements mapelement : arg0)
			r.put(mapelement.key, mapelement.value);
		return r;
	}
}