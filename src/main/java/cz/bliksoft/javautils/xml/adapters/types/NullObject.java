package cz.bliksoft.javautils.xml.adapters.types;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "null")
@XmlAccessorType(XmlAccessType.NONE)
public class NullObject extends AbstractXmlObject {
	@Override
	public Object getValue() {
		return null;
	}

	@Override
	public AbstractXmlObject wrap(Object o) {
		return new NullObject();
	}
}
