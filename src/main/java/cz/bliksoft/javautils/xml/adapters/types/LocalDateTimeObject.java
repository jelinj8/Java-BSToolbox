package cz.bliksoft.javautils.xml.adapters.types;

import java.time.LocalDateTime;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "local-date-time")
@XmlAccessorType(XmlAccessType.NONE)
public class LocalDateTimeObject extends AbstractXmlObject {

	@XmlAttribute
	public String value;

	@Override
	public Object getValue() {
		return value != null ? LocalDateTime.parse(value) : null;
	}

	@Override
	public AbstractXmlObject wrap(Object o) {
		LocalDateTimeObject res = new LocalDateTimeObject();
		res.value = o != null ? o.toString() : null;
		return res;
	}
}
