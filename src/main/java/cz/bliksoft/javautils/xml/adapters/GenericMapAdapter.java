package cz.bliksoft.javautils.xml.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * To be overriden with specific data types and use with @XmlJavaTypeAdapter
 *
 * @author jelinj8
 *
 * @param <K>
 * @param <V>
 */
public abstract class GenericMapAdapter<K, V> extends XmlAdapter<GenericMapAdapter.MapType<K, V>, Map<K, V>> {
	public static class MapType<K, V> {
		@XmlElement
		protected final List<MapTypeEntry<K, V>> entry = new ArrayList<MapTypeEntry<K, V>>();

		public static class MapTypeEntry<K, V> {
			@XmlElement
			protected K key;

			@XmlElement
			protected V value;

			private MapTypeEntry() {
			};

			public static <K, V> MapTypeEntry<K, V> of(final K k, final V v) {
				return new MapTypeEntry<K, V>() {
					{
						this.key = k;
						this.value = v;
					}
				};
			}
		}
	}

	@Override
	public Map<K, V> unmarshal(final GenericMapAdapter.MapType<K, V> v) throws Exception {
		return new HashMap<K, V>() {
			{
				for (GenericMapAdapter.MapType.MapTypeEntry<K, V> myEntryType : v.entry)
					this.put(myEntryType.key, myEntryType.value);
			}
		};
	}

	@Override
	public MapType<K, V> marshal(final Map<K, V> v) throws Exception {
		return new GenericMapAdapter.MapType<K, V>() {
			{
				for (K key : v.keySet())
					this.entry.add(MapTypeEntry.of(key, v.get(key)));
			}
		};
	}
}