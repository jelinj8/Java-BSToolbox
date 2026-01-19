package cz.bliksoft.javautils.xmlfilesystem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.streams.xml.adapters.StringMapAdapter;
import cz.bliksoft.javautils.xml.XmlUtils;
import cz.bliksoft.javautils.xmlfilesystem.model.XAttribute;
import cz.bliksoft.javautils.xmlfilesystem.model.XFile;
import cz.bliksoft.javautils.xmlfilesystem.model.XFileObjectBase;
import cz.bliksoft.javautils.xmlfilesystem.model.XFolder;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = XFile.FILE_ELEMENT)
@XmlAccessorType(XmlAccessType.NONE)
public class FileObject implements Comparable<Object> {
	private static final Logger log = LogManager.getLogger();

	/**
	 * atribut specifikující velkou ikonu
	 */
	// public static final String ATTRIBUTE_LARGE_ICON = "large_icon"; //
	// $NON-NLS-1$

	/**
	 * atribut specifikující třídění souborů, pokud není definované pořadí nebo je
	 * "order" hodnota stejná (true třídit podle jména, false zachovat pořadí)
	 */
	public static final String ATTRIBUTE_SORTED = "sorted"; // $NON-NLS-1$

	/**
	 * název atributu pro specifikaci klíče pro překlad názvu souboru
	 */
	public static final String ATTRIBUTE_TRANSLATION_ID = "translation_id"; // $NON-NLS-1$

	@XmlJavaTypeAdapter(StringMapAdapter.class)
	@XmlElement(name = XAttribute.ATTRIBUTE_NODE)
	Map<String, String> attributes = null;

	String name;

	@XmlAttribute
	int order;

	boolean folder;

	FileObject parent;

	@XmlAttribute
	/**
	 * původ objektu
	 */
	String resourceId;

	@XmlElementWrapper(name = "children") // $NON-NLS-1$
	@XmlElement(name = XFile.FILE_ELEMENT)
	protected List<FileObject> children = null;

	/**
	 * call before first work with existing children
	 */
	protected void initChildren() {
	}

	/**
	 * called before first use of attributes
	 */
	protected void initAttributes() {
	}

	public FileObject() {
		this.folder = true;
		this.name = null;
		this.resourceId = "root"; //$NON-NLS-1$
	}

	public FileObject(Node xmlDefinition, FileObject parent, String resourceId) {
		assert ((xmlDefinition.getNodeName().equalsIgnoreCase(XFile.FILE_ELEMENT))
				|| (xmlDefinition.getNodeName().equalsIgnoreCase(XFolder.FOLDER_ELEMENT)));
		this.parent = parent;
		this.resourceId = resourceId;

		this.folder = xmlDefinition.getNodeName().equalsIgnoreCase(XFolder.FOLDER_ELEMENT);
		NamedNodeMap attribs = xmlDefinition.getAttributes();
		this.name = attribs.getNamedItem(XAttribute.ATTRIBUTE_NAME).getNodeValue();
		Node orderNode = attribs.getNamedItem(XFileObjectBase.ATTRIBUTE_ORDER);
		if (orderNode != null) {
			this.order = Integer.parseInt(orderNode.getNodeValue());
		}
		NodeList nList = xmlDefinition.getChildNodes();
		for (int i = 0; i < nList.getLength(); i++) {
			Node fNode = nList.item(i);
			if (fNode.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			} else if (fNode.getNodeName().equalsIgnoreCase(XAttribute.ATTRIBUTE_NODE)) {
				NamedNodeMap attrlist = fNode.getAttributes();
				Node attrNameAttr = attrlist.getNamedItem(XAttribute.ATTRIBUTE_NAME);
				if (attrNameAttr == null) {
					log.log(Level.WARN, StringUtils.format("FileAttribute without name!\n{0}", //$NON-NLS-1$
							XmlUtils.outerXml(xmlDefinition)));
					continue;
				}
				Node attrValAttr = attrlist.getNamedItem(XAttribute.ATTRIBUTE_VALUE);
				if (attrValAttr != null) {
					this.putAttribute(
							// this.attributes.put(
							attrNameAttr.getNodeValue(), (attrValAttr != null ? attrValAttr.getNodeValue() : null));
				} else {
					putAttribute(attrNameAttr.getNodeValue(), fNode.getTextContent());
				}
			} else if ((fNode.getNodeName().equalsIgnoreCase(XFile.FILE_ELEMENT))
					| (fNode.getNodeName().equalsIgnoreCase(XFolder.FOLDER_ELEMENT))) {
				FileObject fo = new FileObject(fNode, this, this.resourceId);
				addChild(fo);
			}
		}
		if (this.children != null)
			Collections.sort(this.children);
	}

	private void addChild(FileObject ch) {
		if (children == null)
			children = new ArrayList<>();
		children.add(ch);
	}

	private void putAttribute(String key, String value) {
		if (this.attributes == null)
			this.attributes = new HashMap<>();
		this.attributes.put(key, value);
	}

	/**
	 * @return je objekt složka?
	 */
	public boolean isDirectory() {
		initChildren();
		return this.folder;
	}

	/**
	 * @return všichni potomci včetně skrytých (.*)
	 */
	public List<FileObject> getAllChildren() {
		initChildren();
		return new ArrayList<>(this.children);
	}

	/**
	 * @return potomci bez skrytých (.*)
	 */
	public List<FileObject> getChildren() {
		initChildren();
		if (children == null)
			return null;
		ArrayList<FileObject> result = new ArrayList<>();
		for (FileObject f : this.children) {
			if (!f.getName().startsWith(".")) { //$NON-NLS-1$
				result.add(f);
			}
		}
		return result;
	}

	/**
	 * @return potomci (jen soubory) bez skrytých (.*)
	 */
	public List<FileObject> getChildFiles() {
		initChildren();
		if (children == null)
			return null;
		ArrayList<FileObject> result = new ArrayList<>();
		for (FileObject f : this.children) {
			if (!f.getName().startsWith(".") && !f.isDirectory()) { //$NON-NLS-1$
				result.add(f);
			}
		}
		return result;
	}

	/**
	 * @return seznam podsložek bez skrytých
	 */
	public List<FileObject> getDirectories() {
		initChildren();
		if (children == null)
			return null;
		ArrayList<FileObject> result = new ArrayList<>();
		for (FileObject f : this.children) {
			if ((f.isDirectory()) && (!f.getName().startsWith("."))) { //$NON-NLS-1$
				result.add(f);
			}
		}
		return result;
	}

	/**
	 * @return jen listy bez skrytých
	 */
	public List<FileObject> getWithoutDirectories() {
		initChildren();
		if (children == null)
			return null;
		ArrayList<FileObject> result = new ArrayList<>();
		for (FileObject f : this.children) {
			if ((!f.isDirectory()) && (!f.getName().startsWith("."))) { //$NON-NLS-1$
				result.add(f);
			}
		}
		return result;
	}

	/**
	 * @return nadřazený objekt
	 */
	public FileObject getParent() {
		return this.parent;
	}

	/**
	 * vyhledá atribut podle jména
	 * 
	 * @param name
	 * @param def
	 * @return
	 */
	public String getAttribute(String name, String def) {
		initAttributes();
		if (this.attributes == null)
			return def;
		if (this.attributes.containsKey(name))
			return this.attributes.get(name);
		else
			return def;
	}

	/**
	 * vyhledá atribut podle jména, vrátí překlad pokud existuje, pokud ne, vrátí
	 * default, pokud ani to ne, vrátí identifikátor
	 * 
	 * @param name
	 * @param def
	 * @return
	 */
	public String getLocalizedAttribute(String name, String def) {
		initAttributes();
		if (this.attributes == null)
			return def;
		if (this.attributes.containsKey(name)) {
			String key = this.attributes.get(name);
			String res = FileSystem.getTranslation(key);
			if (res == null)
				return key;
			else
				return res;
		} else {
			return def;
		}
	}

	@Override
	public int compareTo(Object o) {
		if (o instanceof FileObject) {
			FileObject fo = (FileObject) o;
			if (this.order < fo.order)
				return -1;
			if (this.order > fo.order)
				return 1;
			if (this.getAttribute(ATTRIBUTE_SORTED, "false").equals("true"))
				return this.name.compareTo(fo.name);
			else
				return 0;
		} else if (o instanceof String) {
			return this.name.compareTo((String) o);
		} else {
			throw new IllegalArgumentException("FileObject can be compared just with another FileObject or String!"); //$NON-NLS-1$
		}
	}

	/**
	 * @return vyhledá nejvýše nadřazený objekt
	 */
	public FileObject getRoot() {
		if (this.parent == null)
			return this;
		else
			return this.parent.getRoot();
	}

	/**
	 * vyhledá objekt podle cesty složené z názvů v hierarchii
	 * 
	 * @param fileName
	 * @return
	 */
	public FileObject getFile(String fileName) {
		initChildren();
		File f = new File(fileName);
		File parentFile = f.getParentFile();
		if (parentFile == null) {
			if (this.children != null)
				for (FileObject ch : this.children) {
					if (f.getName().startsWith("*") && ch.name.endsWith(f.getName().substring(1))) //$NON-NLS-1$
						return ch;

					if (f.getName().endsWith("*") //$NON-NLS-1$
							&& ch.name.startsWith(f.getName().substring(0, f.getName().length() - 2)))
						return ch;

					if (ch.name.equals(f.getName()))
						return ch;
				}
		} else {
			Deque<String> lifo = new LinkedList<>();
			while (f != null) {
				if (StringUtils.hasText(f.getName())) { // $NON-NLS-1$
					lifo.push(f.getName());
				}
				f = f.getParentFile();
			}
			FileObject result = fileName.startsWith("/") ? this.getRoot() : this; //$NON-NLS-1$
			while ((result != null) && (!lifo.isEmpty())) {
				result = result.getFile(lifo.pop());
			}
			return result;
		}
		return null;
	}

	/**
	 * vyhledá objekt podle cesty složené z názvů v hierarchii
	 * 
	 * @param fileNname
	 * @return
	 */
	public FileObject getFile(String fileNname, String... subpaths) {
		StringBuilder path = new StringBuilder(fileNname);
		for (String sp : subpaths) {
			path.append("/");
			path.append(sp);
		}
		return getFile(path.toString());
	}

	protected void importFile(FileObject fo) {
		FileObject existing = this.getFile(fo.name);
		if (existing != null) {
			if (existing.getFullPath().equals(fo.getFullPath())) {
				log.warn("File object {} duplicated.", fo.getFullPath());
			}
			if (existing.isDirectory()) {
				if (fo.order != 0) {
					existing.order = fo.order;
				}
				if (existing.attributes == null && fo.attributes != null)
					existing.attributes = new HashMap<>();
				if (fo.attributes != null)
					existing.attributes.putAll(fo.attributes);
				if (fo.children != null)
					for (FileObject internal : fo.children) {
						existing.importFile(internal);
					}
				if (this.children != null)
					Collections.sort(this.children);
			} else {
				if (fo.isDirectory()) {
					// pokus o přepsání souboru adresářem
				} else {
					// úprava atributů souboru
					if (existing.attributes == null && fo.attributes != null)
						existing.attributes = new HashMap<>();
					if (fo.attributes != null)
						existing.attributes.putAll(fo.attributes);
					if ((fo.order != 0) && (fo.order != existing.order)) {
						existing.order = fo.order;
						// Collections.sort(children);
					}
				}
			}
		} else {
			addChild(fo);
			fo.parent = this;
			Collections.sort(this.children);
		}
	}

	@Override
	public String toString() {
		return this.name + " (" + this.resourceId + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@XmlAttribute
	/**
	 * vrátí název objektu
	 * 
	 * @return
	 */
	public String getName() {
		return this.name;
	}

	private String path = null;

	public String getPath() {
		if (path == null) {
			StringBuilder sb = new StringBuilder();
			sb.append(getName());
			FileObject fo = this;
			while (fo != null) {
				if (fo.getParent() == null) {
					fo = null;
				} else {
					fo = fo.getParent();
					if (fo.getName() != null)
						sb.insert(0, fo.getName() + '/');
				}
			}
			path = sb.toString();
		}
		return path;
	}

	public String getFullPath() {
		return '[' + getResourceId() + ']' + getPath();
	}

	/**
	 * vrátí název objektu lokalizovaný podle registrovaného překladového klíče
	 * 
	 * @return
	 */
	public String getLocalizedName() {
		String id = this.getAttribute(ATTRIBUTE_TRANSLATION_ID, ""); //$NON-NLS-1$
		if (!StringUtils.hasText(id))
			return this.name;
		else {
			String res = FileSystem.getTranslation(id);
			if (StringUtils.hasText(res))
				return res;
			else
				return "<$" + id + "$>"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public Map<String, String> getAttributes() {
		initAttributes();
		return this.attributes;
	}

	/**
	 * @return původ objektu
	 */
	public String getResourceId() {
		return this.resourceId;
	}

	/**
	 * @return poslední část názvu oddělená tečkou
	 */
	public String getExtension() {
		String[] parts = this.getName().split("\\."); //$NON-NLS-1$
		if (parts.length == 0)
			return null;
		return parts[parts.length - 1];
	}

	public FileObject searchByName(String search) {
		initChildren();
		if (search.equals(name))
			return this;
		if ((children != null) && (!children.isEmpty())) {
			FileObject result = null;
			for (FileObject fo : children) {
				result = fo.searchByName(search);
				if (result != null)
					return result;
			}
		}
		return null;
	}

	public static String convertGlobToRegEx(String line) {
		log.debug("got line [{}]", line);
		line = line.trim();
		int strLen = line.length();
		StringBuilder sb = new StringBuilder(strLen);
		// Remove beginning and ending * globs because they're useless
		if (line.startsWith("*")) {
			line = line.substring(1);
			strLen--;
		}
		if (line.endsWith("*")) {
			line = line.substring(0, strLen - 1);
			strLen--;
		}
		boolean escaping = false;
		int inCurlies = 0;
		for (char currentChar : line.toCharArray()) {
			switch (currentChar) {
			case '*':
				if (escaping)
					sb.append("\\*");
				else
					sb.append(".*");
				escaping = false;
				break;
			case '?':
				if (escaping)
					sb.append("\\?");
				else
					sb.append('.');
				escaping = false;
				break;
			case '.':
			case '(':
			case ')':
			case '+':
			case '|':
			case '^':
			case '$':
			case '@':
			case '%':
				sb.append('\\');
				sb.append(currentChar);
				escaping = false;
				break;
			case '\\':
				if (escaping) {
					sb.append("\\\\");
					escaping = false;
				} else
					escaping = true;
				break;
			case '{':
				if (escaping) {
					sb.append("\\{");
				} else {
					sb.append('(');
					inCurlies++;
				}
				escaping = false;
				break;
			case '}':
				if (inCurlies > 0 && !escaping) {
					sb.append(')');
					inCurlies--;
				} else if (escaping)
					sb.append("\\}");
				else
					sb.append("}");
				escaping = false;
				break;
			case ',':
				if (inCurlies > 0 && !escaping) {
					sb.append('|');
				} else if (escaping)
					sb.append("\\,");
				else
					sb.append(",");
				break;
			default:
				escaping = false;
				sb.append(currentChar);
			}
		}
		return sb.toString();
	}
}
