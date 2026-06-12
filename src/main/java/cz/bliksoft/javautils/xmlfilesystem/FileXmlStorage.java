package cz.bliksoft.javautils.xmlfilesystem;

import java.io.File;
import java.io.IOException;

import org.w3c.dom.Document;

import cz.bliksoft.javautils.xml.XmlUtils;

/**
 * Default {@link IWritableXmlStorage} saving the document to a
 * {@link java.io.File}.
 */
public class FileXmlStorage implements IWritableXmlStorage {

	@Override
	public void write(Document document, File source) throws IOException {
		XmlUtils.writeNode(document, source);
	}
}
