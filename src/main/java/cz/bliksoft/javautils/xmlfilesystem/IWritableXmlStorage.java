package cz.bliksoft.javautils.xmlfilesystem;

import java.io.File;
import java.io.IOException;

import org.w3c.dom.Document;

/**
 * Strategy for writing (and possibly reading) the {@link WritableXmlFile}
 * document to/from its source. The default implementation
 * {@link FileXmlStorage} works with {@link java.io.File}; other implementations
 * may target e.g. a database storage.
 */
public interface IWritableXmlStorage {

	/**
	 * saves the document to its source
	 */
	void write(Document document, File source) throws IOException;
}
