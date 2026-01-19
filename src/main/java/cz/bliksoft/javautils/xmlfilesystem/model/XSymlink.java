package cz.bliksoft.javautils.xmlfilesystem.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = XSymlink.SYMLINK_ELEMENT)
@XmlAccessorType(XmlAccessType.NONE)
public class XSymlink {

	public static final String SYMLINK_ELEMENT = "symlink"; //$NON-NLS-1$
	public static final String SYMLINK_FILE_PATH = "path"; //$NON-NLS-1$

	@XmlAttribute(name = SYMLINK_FILE_PATH, required = true)
	protected String path;
}
