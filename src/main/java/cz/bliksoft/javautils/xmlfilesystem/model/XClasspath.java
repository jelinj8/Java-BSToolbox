package cz.bliksoft.javautils.xmlfilesystem.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = XClasspath.CLASSPATH_ELEMENT)
@XmlAccessorType(XmlAccessType.NONE)
public class XClasspath {

	public static final String CLASSPATH_ELEMENT = "classpath";
	public static final String ATTR_CLASSPATH_PATH = "path";

	@XmlAttribute(name = ATTR_CLASSPATH_PATH, required = true)
	protected String path;
}
