package cz.bliksoft.javautils.xmlfilesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import cz.bliksoft.javautils.EnvironmentUtils;
import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.exceptions.InitializationException;

/**
 * core of the FileSystem and working with it
 *
 */
public final class FileSystem {
	private static final Logger log = LogManager.getLogger();

	public static final String DEFAULT_TRANSLATION_ATTR_NAME = "default"; //$NON-NLS-1$
	public static final String TRANSLATION_ROOT_NAME = "translations"; //$NON-NLS-1$

	private static HashMap<String, String> translations = new HashMap<>();
	// private Document rootDoc;
	private FileObject root;

	private static String localeCode = Locale.getDefault().getLanguage();

	private FileSystem() {
		try {
			root = new FileObject();
		} catch (Exception ex) {
			log.log(Level.ERROR, (String) null, ex);
		}
	}

	// public void importFile(File classpathRoot, String resourceId) {
	//
	// }

	public void importXml(InputStream f, String resourceId) {
		importXml(f, resourceId, false, null, root);
	}

	/**
	 * imports the elements of the root document {@code f} into {@code target} (used
	 * for {@code <include>}/{@code <require>}/{@code <classpath>} elements nested
	 * inside a {@code <file>} - their content is imported into the given file, not
	 * the filesystem root)
	 */
	void importXml(InputStream f, String resourceId, boolean writable, FileObject target) {
		importXml(f, resourceId, writable, null, target);
	}

	private void importXml(InputStream f, String resourceId, boolean writable, WritableXmlFile owner,
			FileObject target) {
		if (f == null) {
			log.log(Level.WARN, "Empty stream! ({})", resourceId);
			return;
		}
		Document doc;
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			dBuilder.setErrorHandler(new ErrorHandler() {

				@Override
				public void warning(SAXParseException arg0) throws SAXException {
					log.log(Level.WARN, StringUtils.format("Warning in filesystem {0}[{2}:{3}]: {1}", resourceId,
							arg0.getMessage(), arg0.getLineNumber(), arg0.getColumnNumber()));
				}

				@Override
				public void fatalError(SAXParseException arg0) throws SAXException {
					log.log(Level.ERROR, StringUtils.format("Fatal error in filesystem {0}[{2}:{3}]: {1}", resourceId,
							arg0.getMessage(), arg0.getLineNumber(), arg0.getColumnNumber()));
				}

				@Override
				public void error(SAXParseException arg0) throws SAXException {
					log.log(Level.ERROR, StringUtils.format("Error in filesystem {0}[{2}:{3}]: {1}", resourceId,
							arg0.getMessage(), arg0.getLineNumber(), arg0.getColumnNumber()));
				}
			});
			doc = dBuilder.parse(f);
			doc.getDocumentElement().normalize();

			if (writable && owner == null) {
				owner = new WritableXmlFile(new File(resourceId), doc, new FileXmlStorage());
			}

			NodeList nList = doc.getDocumentElement().getChildNodes();// ElementsByTagName("include");
			for (int i = 0; i < nList.getLength(); i++) {
				Node n = nList.item(i);

				switch (n.getNodeName()) {
				case FileObject.CLASSPATH_ELEMENT: {
					NamedNodeMap attribs = n.getAttributes();
					Node path = attribs.getNamedItem(FileObject.CLASSPATH_PATH);
					String pathString = path.getNodeValue();
					Node modeNode = attribs.getNamedItem(FileObject.ATTRIBUTE_MODE);
					if (modeNode != null && FileObject.MODE_READWRITE.equalsIgnoreCase(modeNode.getNodeValue())) {
						log.log(Level.WARN, StringUtils.format(
								"mode=\"rw\" is not supported for <classpath> includes ({0} for {1}), ignoring.",
								pathString, resourceId));
					}
					InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(pathString);
					if (stream != null) {
						importXml(stream, pathString, false, null, target);
					} else {
						log.log(Level.INFO,
								StringUtils.format("XML resource not found ({0} for {1})", pathString, resourceId));
					}
					break;
				}
				case FileObject.INCLUDE_ELEMENT: // $NON-NLS-1$
				case FileObject.REQUIRE_ELEMENT: // $NON-NLS-1$
				{
					NamedNodeMap attribs = n.getAttributes();
					Node path = attribs.getNamedItem(
							FileObject.REQUIRE_ELEMENT.equals(n.getNodeName()) ? FileObject.REQUIRE_FILE_PATH
									: FileObject.INCLUDE_FILE_PATH);
					String pathString = path.getNodeValue();
					pathString = EnvironmentUtils.pathReplace(pathString);

					Node modeNode = attribs.getNamedItem(FileObject.ATTRIBUTE_MODE);
					boolean childWritable = modeNode != null
							&& FileObject.MODE_READWRITE.equalsIgnoreCase(modeNode.getNodeValue());

					File incFile = new File(pathString);
					if (incFile.exists()) {
						log.log(Level.INFO,
								StringUtils.format("Importing XML file {0} for {1}", pathString, resourceId));
						// doc.getDocumentElement().removeChild(path);
						try (InputStream stream = new FileInputStream(incFile)) {
							// ClassLoader.getSystemResourceAsStream(pathString);
							if (stream != null) {
								importXml(stream, pathString, childWritable, null, target);
							}
						}
					} else {
						if (FileObject.REQUIRE_ELEMENT.equals(n.getNodeName())) {
							log.log(Level.ERROR,
									StringUtils.format("Required file {0} not found ({1})!", pathString, resourceId));
							throw new FileNotFoundException(incFile.getAbsolutePath());
						} else {
							log.log(Level.WARN, StringUtils.format("Included file {0} ({1}) not found, skipping.",
									pathString, resourceId));
						}
					}
					break;
				}
				case FileObject.FILE_ELEMENT:
				case FileObject.SYMLINK_ELEMENT: {
					FileObject fo = FileObject.createChild(n, null, resourceId, writable);
					if (writable && (target.getFile(fo.getName()) != null
							|| (fo.getTargetId() != null && FileObject.getFileByID(fo.getTargetId()) != null))) {
						throw new InitializationException(StringUtils.format(
								"<{0} mode=\"rw\"> root \"{1}\" collides with an existing node ({2})", n.getNodeName(),
								fo.getName(), resourceId));
					}
					try {
						target.importFile(fo);
					} catch (Exception e) {
						throw new InitializationException("Importing file object " + fo.getFullPath(), e);
					}
					if (writable && owner != null) {
						owner.addRoot(fo);
					}
				}
					break;
				case "#text":
				case "#comment":
					break;
				default:
					log.error("Importing unknown element type " + n.getNodeName());
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException | DOMException ex) {
			log.log(Level.ERROR, "Error while processing filesystem", ex);
		}
	}

	private static FileSystem instance;

	public static FileObject getRoot() {
		return getDefault().root;
	}

	public static FileSystem getDefault() {
		if (instance == null) {
			instance = new FileSystem();
		}
		return instance;
	}

	public static FileObject getFile(String name) {
		return getRoot().getFile(name);
	}

	public static FileObject getFile(String fileNname, String... subpaths) {
		return getRoot().getFile(fileNname, subpaths);
	}

	public static FileObject getFileByAttrib(String path, String attribName, String attribValue) {
		FileObject searchPath = getFile(path);
		for (FileObject fo : searchPath.getChildren()) {
			if (fo.getAttribute(attribName, "").equals(attribValue)) { //$NON-NLS-1$
				return fo;
			}
		}
		return null;
	}

	public static String getTranslation(String id) {
		return translations.get(id);
	}

	public static void addTranslations(Map<String, String> translations) {
		FileSystem.translations.putAll(translations);
	}

	public static void addTranslation(String key, String value) {
		if (translations.containsKey(key)) {
			log.log(Level.ERROR, StringUtils.format("Duplicate FileSystem translation key: {0}", key)); //$NON-NLS-1$
		} else {
			translations.put(key, value);
		}
	}

	public static void loadTranslations() {
		FileObject translationsFolder = FileSystem.getFile(TRANSLATION_ROOT_NAME);
		if (translationsFolder == null)
			return;
		if (translationsFolder.isDirectory())
			for (FileObject f : translationsFolder.getChildren()) {
				importTranslationFile(f);
			}
	}

	private static void importTranslationFile(FileObject trn) {
		if (trn.isDirectory()) {
			for (FileObject f : trn.getChildren()) {
				importTranslationFile(f);
			}
		} else {
			String path = trn.getPath().substring(TRANSLATION_ROOT_NAME.length() + 1);
			String val = trn.getAttribute(localeCode, (String) null);
			if (!StringUtils.hasText(val))
				val = trn.getAttribute(DEFAULT_TRANSLATION_ATTR_NAME, (String) null);
			if (StringUtils.hasText(val))
				translations.put(path, val);
		}
	}

}
