package cz.bliksoft.javautils.xml.adapters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Serializes Map&lt;String, Object&gt; as typed elements with name/value
 * attributes: {@code <integer name="dpi" value="8"/>},
 * {@code <string name="mode" value="D"/>}
 */
public class SimpleObjectMapAdapter extends XmlAdapter<SimpleObjectMapAdapter.SimpleMapType, Map<String, Object>> {

	@XmlAccessorType(XmlAccessType.NONE)
	public static class SimpleMapType {
		@XmlElementRefs({ @XmlElementRef(type = NamedIntegerObject.class),
				@XmlElementRef(type = NamedStringObject.class), @XmlElementRef(type = NamedBooleanObject.class),
				@XmlElementRef(type = NamedFloatObject.class), @XmlElementRef(type = NamedLocalDateObject.class),
				@XmlElementRef(type = NamedLocalDateTimeObject.class),
				@XmlElementRef(type = NamedLocalTimeObject.class), @XmlElementRef(type = NamedMapObject.class) })
		public List<NamedObject> entries = new ArrayList<>();
	}

	@XmlSeeAlso({ NamedIntegerObject.class, NamedStringObject.class, NamedBooleanObject.class, NamedFloatObject.class,
			NamedLocalDateObject.class, NamedLocalDateTimeObject.class, NamedLocalTimeObject.class,
			NamedMapObject.class })
	@XmlAccessorType(XmlAccessType.NONE)
	public abstract static class NamedObject {
		@XmlAttribute
		public String name;

		public abstract Object getValue();
	}

	@XmlRootElement(name = "integer")
	@XmlAccessorType(XmlAccessType.NONE)
	public static class NamedIntegerObject extends NamedObject {
		@XmlAttribute
		public Integer value;

		@Override
		public Object getValue() {
			return value;
		}
	}

	@XmlRootElement(name = "string")
	@XmlAccessorType(XmlAccessType.NONE)
	public static class NamedStringObject extends NamedObject {
		@XmlAttribute
		public String value;

		@Override
		public Object getValue() {
			return value;
		}
	}

	@XmlRootElement(name = "boolean")
	@XmlAccessorType(XmlAccessType.NONE)
	public static class NamedBooleanObject extends NamedObject {
		@XmlAttribute
		public Boolean value;

		@Override
		public Object getValue() {
			return value;
		}
	}

	@XmlRootElement(name = "float")
	@XmlAccessorType(XmlAccessType.NONE)
	public static class NamedFloatObject extends NamedObject {
		@XmlAttribute
		public Float value;

		@Override
		public Object getValue() {
			return value;
		}
	}

	@XmlRootElement(name = "local-date")
	@XmlAccessorType(XmlAccessType.NONE)
	public static class NamedLocalDateObject extends NamedObject {
		@XmlAttribute
		public String value;

		@Override
		public Object getValue() {
			return value != null ? LocalDate.parse(value) : null;
		}
	}

	@XmlRootElement(name = "local-date-time")
	@XmlAccessorType(XmlAccessType.NONE)
	public static class NamedLocalDateTimeObject extends NamedObject {
		@XmlAttribute
		public String value;

		@Override
		public Object getValue() {
			return value != null ? LocalDateTime.parse(value) : null;
		}
	}

	@XmlRootElement(name = "local-time")
	@XmlAccessorType(XmlAccessType.NONE)
	public static class NamedLocalTimeObject extends NamedObject {
		@XmlAttribute
		public String value;

		@Override
		public Object getValue() {
			return value != null ? LocalTime.parse(value) : null;
		}
	}

	@XmlRootElement(name = "map")
	@XmlAccessorType(XmlAccessType.NONE)
	public static class NamedMapObject extends NamedObject {
		@XmlElementRefs({ @XmlElementRef(type = NamedIntegerObject.class),
				@XmlElementRef(type = NamedStringObject.class), @XmlElementRef(type = NamedBooleanObject.class),
				@XmlElementRef(type = NamedFloatObject.class), @XmlElementRef(type = NamedLocalDateObject.class),
				@XmlElementRef(type = NamedLocalDateTimeObject.class),
				@XmlElementRef(type = NamedLocalTimeObject.class), @XmlElementRef(type = NamedMapObject.class) })
		public List<NamedObject> entries = new ArrayList<>();

		@Override
		public Object getValue() {
			Map<String, Object> map = new LinkedHashMap<>();
			for (NamedObject o : entries)
				map.put(o.name, o.getValue());
			return map;
		}
	}

	@Override
	public Map<String, Object> unmarshal(SimpleMapType v) throws Exception {
		if (v == null || v.entries.isEmpty())
			return null;
		Map<String, Object> map = new LinkedHashMap<>();
		for (NamedObject o : v.entries)
			map.put(o.name, o.getValue());
		return map;
	}

	@Override
	public SimpleMapType marshal(Map<String, Object> v) throws Exception {
		if (v == null)
			return null;
		SimpleMapType t = new SimpleMapType();
		for (Map.Entry<String, Object> e : v.entrySet())
			t.entries.add(wrap(e.getKey(), e.getValue()));
		return t;
	}

	private static NamedObject wrap(String name, Object value) {
		if (value instanceof Integer) {
			NamedIntegerObject o = new NamedIntegerObject();
			o.name = name;
			o.value = (Integer) value;
			return o;
		} else if (value instanceof Boolean) {
			NamedBooleanObject o = new NamedBooleanObject();
			o.name = name;
			o.value = (Boolean) value;
			return o;
		} else if (value instanceof Float) {
			NamedFloatObject o = new NamedFloatObject();
			o.name = name;
			o.value = (Float) value;
			return o;
		} else if (value instanceof LocalDate) {
			NamedLocalDateObject o = new NamedLocalDateObject();
			o.name = name;
			o.value = value.toString();
			return o;
		} else if (value instanceof LocalDateTime) {
			NamedLocalDateTimeObject o = new NamedLocalDateTimeObject();
			o.name = name;
			o.value = value.toString();
			return o;
		} else if (value instanceof LocalTime) {
			NamedLocalTimeObject o = new NamedLocalTimeObject();
			o.name = name;
			o.value = value.toString();
			return o;
		} else if (value instanceof Map) {
			NamedMapObject o = new NamedMapObject();
			o.name = name;
			for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet())
				o.entries.add(wrap(String.valueOf(e.getKey()), e.getValue()));
			return o;
		}
		NamedStringObject o = new NamedStringObject();
		o.name = name;
		o.value = value != null ? value.toString() : null;
		return o;
	}
}
