package cz.bliksoft.javautils.xml.adapters.types;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "string")
@XmlAccessorType(XmlAccessType.NONE)
public class StringObject extends AbstractXmlObject {

	@XmlElement
	public String value;

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public AbstractXmlObject wrap(Object o) {
		StringObject res = new StringObject();
		res.value = o.toString();
		return res;
	}
}
