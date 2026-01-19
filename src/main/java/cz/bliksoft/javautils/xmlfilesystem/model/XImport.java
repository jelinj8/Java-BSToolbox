package cz.bliksoft.javautils.xmlfilesystem.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = XImport.IMPORT_ELEMENT)
@XmlAccessorType(XmlAccessType.NONE)
public class XImport {

	public static final String IMPORT_ELEMENT = "import"; //$NON-NLS-1$
	public static final String IMPORT_FILE_PATH = "path"; //$NON-NLS-1$

	@XmlAttribute(name = IMPORT_FILE_PATH, required = true)
	protected String path;
}
