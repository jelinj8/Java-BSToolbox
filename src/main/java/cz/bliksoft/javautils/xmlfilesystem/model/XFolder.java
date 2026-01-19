package cz.bliksoft.javautils.xmlfilesystem.model;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = XFolder.FOLDER_ELEMENT)
public class XFolder extends XFileObjectBase {

	public static final String FOLDER_ELEMENT = "folder"; //$NON-NLS-1$

	@Override
	public boolean isFolder() {
		return true;
	}

	@XmlElements({ @XmlElement(name = XFile.FILE_ELEMENT, type = XFile.class),
			@XmlElement(name = FOLDER_ELEMENT, type = XFolder.class) })
	protected List<XFileObjectBase> children;
}
