package cz.bliksoft.javautils.streams.xml.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class MapAdapter<K, V> extends XmlAdapter<ArrayList<MapElements<K, V>>, Map<K, V>> {
	public ArrayList<MapElements<K, V>> marshal(Map<K, V> arg0) throws Exception {
		if (arg0 == null || arg0.isEmpty())
			return null;

		ArrayList<MapElements<K, V>> mapElements = new ArrayList<>(arg0.size());
		for (Map.Entry<K, V> entry : arg0.entrySet())
			mapElements.add(new MapElements<K, V>(entry.getKey(), entry.getValue()));
		return mapElements;
	}

	public Map<K, V> unmarshal(ArrayList<MapElements<K, V>> arg0) throws Exception {
		if (arg0 == null || arg0.isEmpty())
			return null;
		Map<K, V> r = new HashMap<K, V>();
		for (MapElements<K, V> mapelement : arg0)
			r.put(mapelement.key, mapelement.value);
		return r;
	}
}