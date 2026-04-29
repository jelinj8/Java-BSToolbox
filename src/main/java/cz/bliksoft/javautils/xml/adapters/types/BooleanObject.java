package cz.bliksoft.javautils.xml.adapters.types;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "boolean")
@XmlAccessorType(XmlAccessType.NONE)
public class BooleanObject extends AbstractXmlObject {

	@XmlAttribute
	public Boolean value;

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public AbstractXmlObject wrap(Object o) {
		BooleanObject res = new BooleanObject();
		res.value = (Boolean) o;
		return res;
	}

}
