package cz.bliksoft.javautils.xmlfilesystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cz.bliksoft.javautils.xml.XmlUtils;

/**
 * Represents one source XML document of the writable part of the XML filesystem
 * - remembers the source file, its original DOM (to preserve the root element's
 * namespace/attributes and any unrelated content), and the root
 * {@link FileObject}s loaded from it. {@link #save()} writes the current state
 * of these objects back to the source.
 *
 * <p>
 * Can be used either within {@link FileSystem} (REQUIRE/INCLUDE with
 * {@code mode="rw"}), or standalone via {@link #load(File)}.
 */
public class WritableXmlFile {

	private final File sourceFile;

	private final Document templateDocument;

	private final IWritableXmlStorage storage;

	private final List<FileObject> roots = new ArrayList<>();

	private boolean dirty = false;

	public WritableXmlFile(File sourceFile, Document templateDocument, IWritableXmlStorage storage) {
		this.sourceFile = sourceFile;
		this.templateDocument = templateDocument;
		this.storage = storage != null ? storage : new FileXmlStorage();
	}

	/**
	 * standalone (independent of {@link FileSystem}) loading of an XML file as a
	 * writable {@link FileObject} tree
	 */
	public static WritableXmlFile load(File source) throws ParserConfigurationException, SAXException, IOException {
		Document doc = XmlUtils.createDocument(source);
		WritableXmlFile wxf = new WritableXmlFile(source, doc, new FileXmlStorage());

		NodeList children = doc.getDocumentElement().getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equalsIgnoreCase(FileObject.FILE_ELEMENT)) {
				FileObject fo = FileObject.createChild(n, null, source.getPath(), true);
				wxf.addRoot(fo);
			}
		}
		return wxf;
	}

	/**
	 * registers a root {@link FileObject} coming from this document and links it
	 * (and its writable children) to this document
	 */
	public void addRoot(FileObject fo) {
		roots.add(fo);
		fo.streamDFAllChildren(true).forEach(f -> {
			if (f instanceof WritableFileObject)
				((WritableFileObject) f).setDocument(this);
		});
	}

	public List<FileObject> getRoots() {
		return roots;
	}

	public File getSourceFile() {
		return sourceFile;
	}

	public void markDirty() {
		this.dirty = true;
	}

	public boolean isDirty() {
		return dirty;
	}

	/**
	 * saves the current state of {@link #getRoots()} back to
	 * {@link #getSourceFile()}, while preserving the rest of the original
	 * document's content (the root element's namespace, any sibling
	 * {@code <include>} elements, etc.)
	 */
	public void save() throws IOException {
		Document clone = (Document) templateDocument.cloneNode(true);
		Element docElement = clone.getDocumentElement();

		List<Node> existingFiles = new ArrayList<>();
		NodeList children = docElement.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE && (n.getNodeName().equalsIgnoreCase(FileObject.FILE_ELEMENT)
					|| n.getNodeName().equalsIgnoreCase(FileObject.SYMLINK_ELEMENT)))
				existingFiles.add(n);
		}
		for (Node n : existingFiles)
			docElement.removeChild(n);

		for (FileObject fo : roots)
			docElement.appendChild(buildElement(clone, fo));

		storage.write(clone, sourceFile);
		dirty = false;
	}

	/**
	 * creates a {@code <file>} (or {@code <symlink>}) element corresponding to the
	 * current state of {@code fo} and its children
	 */
	private Element buildElement(Document doc, FileObject fo) {
		if (fo instanceof FileSymlink) {
			Element el = doc.createElement(FileObject.SYMLINK_ELEMENT);
			el.setAttribute(FileObject.ATTRIBUTE_FILE_NAME, fo.getName());
			el.setAttribute(FileObject.SYMLINK_FILE_PATH, ((FileSymlink) fo).getTargetPath());
			return el;
		}

		Element el = doc.createElement(FileObject.FILE_ELEMENT);
		el.setAttribute(FileObject.ATTRIBUTE_FILE_NAME, fo.getName());
		if (fo.getType() != null)
			el.setAttribute(FileObject.ATTRIBUTE_FILE_TYPE, fo.getType());
		if (fo.getId() != null)
			el.setAttribute(FileObject.ATTRIBUTE_FILE_ID, fo.getId());
		if (fo.getTargetId() != null)
			el.setAttribute(FileObject.ATTRIBUTE_FILE_TARGET, fo.getTargetId());
		if (fo.getOrder() != 0)
			el.setAttribute(FileObject.ATTRIBUTE_FILE_POSITION, String.valueOf(fo.getOrder()));
		if (fo.isSorted())
			el.setAttribute(FileObject.ATTRIBUTE_FILE_SORTED, "true");
		if (fo.getLocked() != null)
			el.setAttribute(FileObject.ATTRIBUTE_FILE_LOCKED, String.valueOf(fo.getLocked()));
		if (fo.getTranslationKey() != null)
			el.setAttribute(FileObject.ATTRIBUTE_FILE_TRANSLATION, fo.getTranslationKey());

		Map<String, FileObject.FileAttribute> attrs = fo.getAttributes();
		if (attrs != null) {
			for (Map.Entry<String, FileObject.FileAttribute> e : attrs.entrySet()) {
				Element a = doc.createElement(FileObject.ATTRIBUTE_NODE);
				a.setAttribute(FileObject.ATTRIBUTE_NAME, e.getKey());
				a.setAttribute(FileObject.ATTRIBUTE_VALUE, e.getValue().value);
				if (e.getValue().translationID != null)
					a.setAttribute(FileObject.ATTRIBUTE_TRANSLATION, e.getValue().translationID);
				el.appendChild(a);
			}
		}

		for (FileObject child : fo.getAllChildren())
			if (child instanceof WritableFileObject)
				el.appendChild(buildElement(doc, child));

		return el;
	}
}
