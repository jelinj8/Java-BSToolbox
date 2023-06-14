package cz.bliksoft.javautils.xml.adapters.types;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

@XmlRootElement(name = "integer")
@XmlAccessorType(XmlAccessType.NONE)
public class IntegerObject extends AbstractXmlObject {

	@XmlAttribute
	public Integer value;

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public AbstractXmlObject wrap(Object o) {
		IntegerObject res = new IntegerObject();
		res.value = (Integer) o;
		return res;
	}
}
