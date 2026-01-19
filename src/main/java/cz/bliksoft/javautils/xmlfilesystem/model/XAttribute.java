package cz.bliksoft.javautils.xmlfilesystem.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = XAttribute.ATTRIBUTE_NODE)
@XmlAccessorType(XmlAccessType.NONE)
public class XAttribute {
	/**
	 * název XML elementu atributu
	 */
	public static final String ATTRIBUTE_NODE = "attribute"; // $NON-NLS-1$

	/**
	 * XML názvový atribut atributového elementu
	 */
	public static final String ATTRIBUTE_NAME = "name"; // $NON-NLS-1$

	/**
	 * XML hodnotový atribut atributového elementu
	 */
	public static final String ATTRIBUTE_VALUE = "value"; // $NON-NLS-1$

	@XmlAttribute(name = ATTRIBUTE_NAME)
	protected String name;

	@XmlAttribute(name = ATTRIBUTE_VALUE)
	protected String value;
}
