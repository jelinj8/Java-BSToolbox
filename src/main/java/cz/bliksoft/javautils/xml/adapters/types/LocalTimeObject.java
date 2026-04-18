package cz.bliksoft.javautils.xml.adapters.types;

import java.time.LocalTime;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "local-time")
@XmlAccessorType(XmlAccessType.NONE)
public class LocalTimeObject extends AbstractXmlObject {

	@XmlAttribute
	public String value;

	@Override
	public Object getValue() {
		return value != null ? LocalTime.parse(value) : null;
	}

	@Override
	public AbstractXmlObject wrap(Object o) {
		LocalTimeObject res = new LocalTimeObject();
		res.value = o != null ? o.toString() : null;
		return res;
	}
}
