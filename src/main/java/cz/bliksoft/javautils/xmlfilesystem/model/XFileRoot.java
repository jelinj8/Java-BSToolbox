package cz.bliksoft.javautils.xmlfilesystem.model;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class XFileRoot {
	@XmlElements({ @XmlElement(name = XFile.FILE_ELEMENT, type = XFile.class),
			@XmlElement(name = XFolder.FOLDER_ELEMENT, type = XFolder.class),
			@XmlElement(name = XImport.IMPORT_ELEMENT, type = XImport.class),
			@XmlElement(name = XInclude.INCLUDE_ELEMENT, type = XInclude.class),
			@XmlElement(name = XRequire.REQUIRE_ELEMENT, type = XRequire.class),
			@XmlElement(name = XClasspath.CLASSPATH_ELEMENT, type = XClasspath.class),
			@XmlElement(name = XSymlink.SYMLINK_ELEMENT, type = XSymlink.class)
	})
	protected List<XFileObjectBase> items;
}
