package cz.bliksoft.javautils.streams.xml.adapters;

import jakarta.xml.bind.annotation.XmlAttribute;

class MapElements<K, V> {
	@XmlAttribute
	public K key;
	@XmlAttribute
	public V value;

	@SuppressWarnings("unused")
	private MapElements() {
	} // Required by JAXB

	public MapElements(K key, V value) {
		this.key = key;
		this.value = value;
	}
}
