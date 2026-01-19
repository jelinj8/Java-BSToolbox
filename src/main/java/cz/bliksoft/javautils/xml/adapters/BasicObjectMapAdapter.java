package cz.bliksoft.javautils.xml.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.bliksoft.javautils.xml.adapters.types.AbstractXmlObject;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * To be overriden with specific data types and use with @XmlJavaTypeAdapter
 * 
 * @author jjelinek
 *
 * @param <K>
 * @param <V>
 */
public class BasicObjectMapAdapter extends XmlAdapter<BasicObjectMapAdapter.BasicObjectMapType, Map<String, Object>> {
	@XmlAccessorType(XmlAccessType.NONE)
	public static class BasicObjectMapType {

		@XmlElement
		protected final List<BasicObjectMapEntry> entry = new ArrayList<BasicObjectMapEntry>();

		@XmlAccessorType(XmlAccessType.NONE)
		public static class BasicObjectMapEntry {
			@XmlAttribute
			protected String key;

			@XmlElementRef
			protected AbstractXmlObject value;

			public static BasicObjectMapEntry of(final String k, final Object v) {
				return new BasicObjectMapEntry() {
					{
						this.key = k;
						this.value = AbstractXmlObject.of(v);
					}
				};
			}
		}
	}

	@Override
	public Map<String, Object> unmarshal(final BasicObjectMapAdapter.BasicObjectMapType v) throws Exception {
		if (v.entry.size() == 0)
			return null;
		return new HashMap<String, Object>() {
			{
				for (BasicObjectMapAdapter.BasicObjectMapType.BasicObjectMapEntry myEntryType : v.entry)
					this.put(myEntryType.key, myEntryType.value.getValue());
			}
		};
	}

	@Override
	public BasicObjectMapType marshal(final Map<String, Object> v) throws Exception {
		if (v == null)
			return null;
		return new BasicObjectMapAdapter.BasicObjectMapType() {
			{
				for (String key : v.keySet()) {
					this.entry.add(BasicObjectMapEntry.of(key, v.get(key)));
				}
			}
		};
	}
}