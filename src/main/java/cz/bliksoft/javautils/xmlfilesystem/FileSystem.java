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

/**
 * základ FileSystému a práce s ním
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
		if (f == null) {
			log.log(Level.WARN, "Prázdný stream! ({})", resourceId);
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
			NodeList nList = doc.getDocumentElement().getChildNodes();// ElementsByTagName("include");
			for (int i = 0; i < nList.getLength(); i++) {
				Node n = nList.item(i);

				switch (n.getNodeName()) {
				case FileObject.IMPORT_ELEMENT: {
					NamedNodeMap attribs = n.getAttributes();
					Node path = attribs.getNamedItem(FileObject.IMPORT_FILE_PATH);
					String pathString = path.getNodeValue();
					// doc.getDocumentElement().removeChild(path);
					InputStream stream = ClassLoader.getSystemResourceAsStream(pathString);
					if (stream != null) {
						importXml(stream, pathString);
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
					Node path = attribs
							.getNamedItem(FileObject.REQUIRE_ELEMENT.equals(n.getNodeName()) ? FileObject.REQUIRE_FILE_PATH
									: FileObject.INCLUDE_FILE_PATH);
					String pathString = path.getNodeValue();
					pathString = EnvironmentUtils.pathReplace(pathString);

					File incFile = new File(pathString);
					if (incFile.exists()) {
						log.log(Level.INFO,
								StringUtils.format("Importing XML file {0} for {1}", pathString, resourceId));
						// doc.getDocumentElement().removeChild(path);
						InputStream stream = new FileInputStream(incFile);

						// ClassLoader.getSystemResourceAsStream(pathString);
						if (stream != null) {
							importXml(stream, pathString);
						}
					} else {
						if (FileObject.REQUIRE_ELEMENT.equals(n.getNodeName())) {
							log.log(Level.ERROR,
									StringUtils.format("Required file {0} not found ({1})!", pathString, resourceId));
							throw new FileNotFoundException(pathString);
						} else {
							log.log(Level.WARN, StringUtils.format("Included file {0} ({1}) not found, skipping.",
									pathString, resourceId));
						}
					}
					break;
				}
				case FileObject.FILE_ELEMENT:
				case FileObject.FOLDER_ELEMENT:
					FileObject fo = new FileObject(n, null, resourceId);
					try {
						root.importFile(fo);
					} catch (Exception e) {
						log.error("Importing file object " + fo.getFullPath(), e);
					}
					break;
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
		translations.putAll(translations);
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
			if (translationsFolder.getChildren() != null) {
				for (FileObject f : translationsFolder.getChildren()) {
					importTranslationFile(f);
				}
			}
	}

	private static void importTranslationFile(FileObject trn) {
		if (trn.isDirectory()) {
			if (trn.getChildren() != null) {
				for (FileObject f : trn.getChildren()) {
					importTranslationFile(f);
				}
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
