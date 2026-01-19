package cz.bliksoft.javautils.xmlfilesystem.model;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class XFileObjectBase {

	/**
	 * specifikace pořadí souboru
	 */
	public static final String ATTRIBUTE_ORDER = "position"; // $NON-NLS-1$
	public static final String ATTR_FILE_NAME = "name"; // $NON-NLS-1$

	public boolean isFolder() {
		return false;
	}

	@XmlAttribute(name = ATTR_FILE_NAME, required = true)
	protected String name;

	@XmlAttribute(name = ATTRIBUTE_ORDER)
	protected Integer position = 0;

	@XmlElement(name = XAttribute.ATTRIBUTE_NODE)
	protected List<XAttribute> attributes;

}
