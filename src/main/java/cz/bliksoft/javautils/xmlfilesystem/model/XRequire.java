package cz.bliksoft.javautils.xmlfilesystem.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = XRequire.REQUIRE_ELEMENT)
@XmlAccessorType(XmlAccessType.NONE)
public class XRequire {

	public static final String REQUIRE_ELEMENT = "require"; //$NON-NLS-1$
	public static final String REQUIRE_FILE_PATH = "path"; //$NON-NLS-1$

	@XmlAttribute(name = REQUIRE_FILE_PATH, required = true)
	protected String path;
}
