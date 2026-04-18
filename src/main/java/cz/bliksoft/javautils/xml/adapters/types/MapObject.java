package cz.bliksoft.javautils.xml.adapters.types;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "map")
@XmlAccessorType(XmlAccessType.NONE)
public class MapObject extends AbstractXmlObject {

	@XmlAccessorType(XmlAccessType.NONE)
	public static class MapEntry {
		@XmlAttribute
		public String key;

		@XmlElementRef
		public AbstractXmlObject value;
	}

	@XmlElement(name = "entry")
	public List<MapEntry> entry = new ArrayList<>();

	@Override
	public Object getValue() {
		Map<String, Object> map = new LinkedHashMap<>();
		for (MapEntry e : entry)
			map.put(e.key, e.value != null ? e.value.getValue() : null);
		return map;
	}

	@SuppressWarnings("unchecked")
	@Override
	public AbstractXmlObject wrap(Object o) {
		MapObject res = new MapObject();
		for (Map.Entry<String, Object> e : ((Map<String, Object>) o).entrySet()) {
			MapEntry me = new MapEntry();
			me.key = e.getKey();
			me.value = AbstractXmlObject.of(e.getValue());
			res.entry.add(me);
		}
		return res;
	}
}
