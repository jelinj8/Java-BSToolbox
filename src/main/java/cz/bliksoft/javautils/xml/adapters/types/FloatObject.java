package cz.bliksoft.javautils.xml.adapters.types;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "float")
@XmlAccessorType(XmlAccessType.NONE)
public class FloatObject extends AbstractXmlObject {
	
	@XmlAttribute
	Float value;
	
	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public AbstractXmlObject wrap(Object o) {
		FloatObject res = new FloatObject();
		res.value = (Float) o;
		return res;
	}
}
