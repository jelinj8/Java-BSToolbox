package cz.bliksoft.javautils.xmlfilesystem.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = XFile.FILE_ELEMENT)
public class XFile extends XFileObjectBase {

	public static final String FILE_ELEMENT = "file"; // $NON-NLS-1$
}
