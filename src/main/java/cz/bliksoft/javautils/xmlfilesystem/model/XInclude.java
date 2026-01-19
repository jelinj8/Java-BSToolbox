package cz.bliksoft.javautils.xmlfilesystem.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = XInclude.INCLUDE_ELEMENT)
@XmlAccessorType(XmlAccessType.NONE)
public class XInclude {

	public static final String INCLUDE_ELEMENT = "include"; //$NON-NLS-1$
	public static final String INCLUDE_FILE_PATH = "path"; //$NON-NLS-1$

	@XmlAttribute(name = INCLUDE_FILE_PATH, required = true)
	protected String path;
}
