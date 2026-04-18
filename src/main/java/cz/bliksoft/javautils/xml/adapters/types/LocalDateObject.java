package cz.bliksoft.javautils.xml.adapters.types;

import java.time.LocalDate;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "local-date")
@XmlAccessorType(XmlAccessType.NONE)
public class LocalDateObject extends AbstractXmlObject {

	@XmlAttribute
	public String value;

	@Override
	public Object getValue() {
		return value != null ? LocalDate.parse(value) : null;
	}

	@Override
	public AbstractXmlObject wrap(Object o) {
		LocalDateObject res = new LocalDateObject();
		res.value = o != null ? o.toString() : null;
		return res;
	}
}
