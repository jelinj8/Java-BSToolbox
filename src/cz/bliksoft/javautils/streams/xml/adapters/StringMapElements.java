package cz.bliksoft.javautils.streams.xml.adapters;

import javax.xml.bind.annotation.XmlAttribute;

class StringMapElements {
	@XmlAttribute
	public String key;
	@XmlAttribute
	public String value;

	@SuppressWarnings("unused")
	private StringMapElements() {
	} // Required by JAXB

	public StringMapElements(String key, String value) {
		this.key = key;
		this.value = value;
	}
}
