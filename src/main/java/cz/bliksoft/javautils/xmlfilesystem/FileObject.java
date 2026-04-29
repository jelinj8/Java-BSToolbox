package cz.bliksoft.javautils.xmlfilesystem;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.exceptions.InitializationException;
import cz.bliksoft.javautils.streams.xml.adapters.StringMapAdapter;
import cz.bliksoft.javautils.xml.XmlUtils;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = FileObject.FILE_ELEMENT)
@XmlAccessorType(XmlAccessType.NONE)
public class FileObject implements Comparable<Object> {
	private static final Logger log = LogManager.getLogger();

	class FileAttribute {
		public String value;
		public String translationID;
	}

	/**
	 * XML element souboru
	 */
	public static final String FILE_ELEMENT = "file"; // $NON-NLS-1$

	/**
	 * pořadí souboru
	 */
	public static final String ATTRIBUTE_FILE_POSITION = "position"; // $NON-NLS-1$

	/**
	 * název souboru
	 */
	public static final String ATTRIBUTE_FILE_NAME = "name"; // $NON-NLS-1$

	/**
	 * typ souboru
	 */
	public static final String ATTRIBUTE_FILE_TYPE = "type"; // $NON-NLS-1$

	/**
	 * ID souboru, musí být jedinečné
	 */
	public static final String ATTRIBUTE_FILE_ID = "id"; // $NON-NLS-1$

	/**
	 * cíl pro import souboru (importuje do registrovaného ID souboru bez ohledu
	 * jeho aktuální cestu)
	 */
	public static final String ATTRIBUTE_FILE_TARGET = "target"; // $NON-NLS-1$

	/**
	 * atribut specifikující třídění souborů, pokud není definované pořadí nebo je
	 * "order" hodnota stejná (true třídit podle jména, false zachovat pořadí)
	 */
	public static final String ATTRIBUTE_FILE_SORTED = "sorted"; // $NON-NLS-1$

	/**
	 * při importu odebere existující soubor, potomky ignoruje
	 */
	public static final String ATTRIBUTE_FILE_REMOVE = "remove"; // $NON-NLS-1$

	/**
	 * při importu odebere existující soubor, potomky ignoruje
	 */
	public static final String ATTRIBUTE_FILE_REPLACE = "replace"; // $NON-NLS-1$

	/**
	 * na uzamčený soubor nejde v dalším importu sahat (jinak než s odemčením)
	 */
	public static final String ATTRIBUTE_FILE_LOCKED = "locked"; // $NON-NLS-1$

	/**
	 * dodatečný řetězec k libovolnému použití
	 */
	public static final String ATTRIBUTE_FILE_MARK = "mark"; // $NON-NLS-1$

	/**
	 * název atributu pro specifikaci klíče pro překlad názvu souboru
	 */
	public static final String ATTRIBUTE_FILE_TRANSLATION = "translation"; // $NON-NLS-1$

	/**
	 * název XML elementu atributu
	 */
	public static final String ATTRIBUTE_NODE = "attribute"; // $NON-NLS-1$

	/**
	 * XML názvový atribut atributového elementu
	 */
	public static final String ATTRIBUTE_NAME = ATTRIBUTE_FILE_NAME; // $NON-NLS-1$

	/**
	 * XML hodnotový atribut atributového elementu
	 */
	public static final String ATTRIBUTE_VALUE = "value"; // $NON-NLS-1$

	/**
	 * název atributu pro specifikaci klíče pro překlad hodnoty atributu
	 */
	public static final String ATTRIBUTE_TRANSLATION = ATTRIBUTE_FILE_TRANSLATION; // $NON-NLS-1$

	/**
	 * import z classpath
	 */
	public static final String CLASSPATH_ELEMENT = "classpath"; //$NON-NLS-1$
	public static final String CLASSPATH_PATH = "path"; //$NON-NLS-1$

	public static final String INCLUDE_ELEMENT = "include"; //$NON-NLS-1$
	public static final String INCLUDE_FILE_PATH = "path"; //$NON-NLS-1$

	public static final String REQUIRE_ELEMENT = "require"; //$NON-NLS-1$
	public static final String REQUIRE_FILE_PATH = "path"; //$NON-NLS-1$

	public static final String SYMLINK_ELEMENT = "symlink"; //$NON-NLS-1$
	public static final String SYMLINK_FILE_PATH = "path"; //$NON-NLS-1$

	/**
	 * atribut specifikující velkou ikonu
	 */
	// public static final String ATTRIBUTE_LARGE_ICON = "large_icon"; //
	// $NON-NLS-1$

//	public static final String ATTRIBUTE_SORTED = "sorted"; // $NON-NLS-1$

//	public static final String ATTRIBUTE_TRANSLATION_ID = "translation_id"; // $NON-NLS-1$

	@XmlJavaTypeAdapter(StringMapAdapter.class)
	@XmlElement(name = ATTRIBUTE_NODE)
	protected Map<String, FileAttribute> attributes = new HashMap<>();

	@XmlAttribute
	String name;

	@XmlAttribute
	String type;

	@XmlAttribute
	String id;

	@XmlAttribute
	String target;

	@XmlAttribute
	int order;

	boolean folder;

	FileObject parent;

	@XmlAttribute
	/**
	 * původ objektu
	 */
	String resourceId;

	@XmlAttribute
	boolean sorted = false;

	@XmlAttribute
	private Boolean locked = null;

	public Boolean getLocked() {
		return locked;
	}

	boolean remove = false;

	boolean replace = false;

	@XmlAttribute
	String translation = null;

	@XmlElement(name = FILE_ELEMENT)
	protected List<FileObject> children = new ArrayList<>();

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
		this.type = null;
		this.resourceId = "root"; //$NON-NLS-1$
	}

	public FileObject(Node xmlDefinition, FileObject parent, String resourceId) {
		assert ((xmlDefinition.getNodeName().equalsIgnoreCase(FILE_ELEMENT))
				|| (xmlDefinition.getNodeName().equalsIgnoreCase(SYMLINK_ELEMENT)));

		this.parent = parent;
		this.resourceId = resourceId;

		this.folder = false;
		NamedNodeMap attribs = xmlDefinition.getAttributes();
		this.name = attribs.getNamedItem(ATTRIBUTE_NAME).getNodeValue();

		Node typeNode = attribs.getNamedItem(ATTRIBUTE_FILE_TYPE);
		if (typeNode != null) {
			String typeString = typeNode.getNodeValue();
			if (StringUtils.hasLength(typeString))
				this.type = typeString;
		}

		Node idNode = attribs.getNamedItem(ATTRIBUTE_FILE_ID);
		if (idNode != null) {
			String typeString = idNode.getNodeValue();
			if (StringUtils.hasLength(typeString))
				this.id = typeString;
		}

		Node targetNode = attribs.getNamedItem(ATTRIBUTE_FILE_TARGET);
		if (targetNode != null) {
			String typeString = targetNode.getNodeValue();
			if (StringUtils.hasLength(typeString))
				this.target = typeString;
		}

		Node orderNode = attribs.getNamedItem(ATTRIBUTE_FILE_POSITION);
		if (orderNode != null) {
			this.order = Integer.parseInt(orderNode.getNodeValue());
		}

		Node sortedNode = attribs.getNamedItem(ATTRIBUTE_FILE_SORTED);
		if (sortedNode != null) {
			this.sorted = Boolean.parseBoolean(sortedNode.getNodeValue());
		}

		Node removeNode = attribs.getNamedItem(ATTRIBUTE_FILE_REMOVE);
		if (removeNode != null) {
			this.remove = Boolean.parseBoolean(removeNode.getNodeValue());
		}

		Node replaceNode = attribs.getNamedItem(ATTRIBUTE_FILE_REPLACE);
		if (replaceNode != null) {
			this.replace = Boolean.parseBoolean(replaceNode.getNodeValue());
		}

		Node lockedNode = attribs.getNamedItem(ATTRIBUTE_FILE_LOCKED);
		if (lockedNode != null) {
			this.locked = Boolean.parseBoolean(lockedNode.getNodeValue());
		}

		// only if not removing from original file and not symlink
		if (!remove && xmlDefinition.getNodeName().equalsIgnoreCase(FILE_ELEMENT)) {

			Node translationNode = attribs.getNamedItem(ATTRIBUTE_FILE_TRANSLATION);
			if (translationNode != null) {
				this.translation = translationNode.getNodeValue();
			}

			NodeList nList = xmlDefinition.getChildNodes();
			for (int i = 0; i < nList.getLength(); i++) {
				Node fNode = nList.item(i);
				if (fNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				} else if (fNode.getNodeName().equalsIgnoreCase(ATTRIBUTE_NODE)) {
					NamedNodeMap attrlist = fNode.getAttributes();
					Node attrNameAttr = attrlist.getNamedItem(ATTRIBUTE_NAME);
					if (attrNameAttr == null) {
						log.log(Level.WARN, StringUtils.format("FileAttribute without name!\n{0}", //$NON-NLS-1$
								XmlUtils.outerXml(xmlDefinition)));
						continue;
					}
					Node attrTranslationIdAttr = attrlist.getNamedItem(ATTRIBUTE_TRANSLATION);

					Node attrValAttr = attrlist.getNamedItem(ATTRIBUTE_VALUE);
					if (attrValAttr != null) {
						this.putAttribute(
								// this.attributes.put(
								attrNameAttr.getNodeValue(), attrValAttr.getNodeValue(),
								(attrTranslationIdAttr != null ? attrTranslationIdAttr.getNodeValue() : null));
					} else {
						putAttribute(attrNameAttr.getNodeValue(), fNode.getTextContent(),
								(attrTranslationIdAttr != null ? attrTranslationIdAttr.getNodeValue() : null));
					}
				} else if ((fNode.getNodeName().equalsIgnoreCase(FILE_ELEMENT))) {
					this.folder = true;
					FileObject fo = new FileObject(fNode, this, this.resourceId);
//					if (!fo.remove)
					addChild(fo);
				} else if ((fNode.getNodeName().equalsIgnoreCase(SYMLINK_ELEMENT))) {
					this.folder = true;
					FileSymlink fo = new FileSymlink(fNode, this, this.resourceId);
					addChild(fo);
				}
			}
			if (this.children != null)
				Collections.sort(this.children);
		}
	}

	private void addChild(FileObject ch) {
		children.add(ch);
	}

	private void putAttribute(String key, String value, String translationId) {
		FileAttribute a = new FileAttribute();
		a.value = value;
		a.translationID = translationId;
		this.attributes.put(key, a);
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
		ArrayList<FileObject> result = new ArrayList<>();
		if (children == null)
			return result;
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
		ArrayList<FileObject> result = new ArrayList<>();
		if (children == null)
			return result;
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
		ArrayList<FileObject> result = new ArrayList<>();
		if (children == null)
			return result;
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
		ArrayList<FileObject> result = new ArrayList<>();
		if (children == null)
			return result;
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
			return this.attributes.get(name).value;
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
			FileAttribute a = this.attributes.get(name);
			if (a.translationID == null)
				return a.value;
			else {
				String res = FileSystem.getTranslation(a.translationID);
				if (res == null)
					return a.value;
				else
					return res;
			}
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
			if (this.parent != null && this.parent.sorted)
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
//		if (skip == this)
//			return null;
		File f = new File(fileName);
		File parentFile = f.getParentFile();
		if (parentFile == null) {
			initChildren();
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
	 * @param fileName
	 * @return
	 */
	public FileObject getFile(String fileName, String... subpaths) {
		StringBuilder path = new StringBuilder("/".equals(fileName) ? "" : fileName);

		if (subpaths == null || subpaths.length < 1)
			return getFile(fileName);

		for (String sp : subpaths) {
			path.append("/");
			path.append(sp);
		}
		return getFile(path.toString());
	}

	private static HashMap<String, FileObject> namedFiles = new HashMap<>();

	public static FileObject getFileByID(String id) {
		return namedFiles.get(id);
	}

	protected void importFile(FileObject fo) {
		FileObject existing = null;
		if (fo.target != null) {
			existing = getFileByID(fo.target);
			if (existing == null)
				throw new InitializationException(
						"Target FileObject with ID=" + fo.target + " not yet seen, unable to import.");

			if (fo.parent != null)
				fo.parent.children.remove(fo);
			fo.parent = existing.parent;

		} else {
			existing = this.getFile(fo.name);
		}
		if (existing != null) {
			if (Boolean.TRUE.equals(existing.locked)) {
				if (fo.locked == null) {
					// overwriting without unlocking or override
					throw new InitializationException(
							MessageFormat.format("Overwriting a locked file {0} with {1} without unlocking",
									existing.getFullPath(), fo.getFullPath()));
				} else {
					existing.locked = fo.locked;
				}
			}

			if (fo.remove) {
				children.remove(existing);
				if (fo.id != null)
					namedFiles.remove(fo.id);
				existing.parent = null;
			} else if (replace) {
				children.remove(existing);
				existing.parent = null;
				addChild(fo);
				if (fo.id != null)
					namedFiles.put(fo.id, fo);

				fo.parent = this;
				Collections.sort(this.children);
			} else { // merge
				if (existing.getFullPath().equals(fo.getFullPath())) {
					log.warn("File object {} duplicated in the same resource.", fo.getFullPath());
				}
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
			}
		} else {
			if (!fo.remove) {
				fo.streamDFAllChildren(true).forEach(fo2 -> {
					if (fo2.id != null) {
						if (namedFiles.containsKey(fo2.id) && !fo.replace)
							throw new InitializationException("File with ID=" + fo2.id
									+ " already registered and not force-replacing it by " + fo2);

						namedFiles.put(fo2.id, fo2);
					}

					if (fo2.target != null) {
						fo.importFile(fo2);
					}
				});
				addChild(fo);
				fo.parent = this;
				Collections.sort(this.children);
			}
		}
	}

	@Override
	public String toString() {
		return MessageFormat.format("{0} ({1}){2}{3}", this.name, this.resourceId,
				Boolean.TRUE.equals(locked) ? " [LCK]" : "", this.id != null ? " {" + this.id + "}" : "");
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
		if (!StringUtils.hasText(translation))
			return this.name;
		else {
			String res = FileSystem.getTranslation(translation);
			if (StringUtils.hasText(res))
				return res;
			else
				return "<$" + translation + "$>"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public Map<String, FileAttribute> getAttributes() {
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
	public String getType() {
		return type;
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

	public boolean getBool(String key, boolean def) {
		String v = getAttribute(key, null);
		if (v == null)
			return def;
		v = v.trim();
		if (v.isEmpty())
			return def;
		return Boolean.parseBoolean(v);
	}

	public Boolean getBool(String key) {
		return getBool(key, null);
	}

	public Boolean getBool(String key, Boolean def) {
		String v = getAttribute(key, null);
		if (v == null)
			return def;
		v = v.trim();
		if (v.isEmpty())
			return def;
		return Boolean.parseBoolean(v);
	}

	public int getInt(String key, int def) {
		String v = getAttribute(key, null);
		if (v == null)
			return def;
		try {
			return Integer.parseInt(v.trim());
		} catch (Exception e) {
			return def;
		}
	}

	public Integer getInteger(String key) {
		return getInteger(key, null);
	}

	public Long getLong(String key, Long def) {
		String v = getAttribute(key, null);
		if (v == null)
			return def;
		try {
			return Long.parseLong(v.trim());
		} catch (Exception e) {
			return def;
		}
	}

	public Long getLong(String key) {
		return getLong(key, null);
	}

	public Integer getInteger(String key, Integer def) {
		String v = getAttribute(key, null);
		if (v == null)
			return def;
		try {
			return Integer.parseInt(v.trim());
		} catch (Exception e) {
			return def;
		}
	}

	public double getDouble(String key, double def) {
		String v = getAttribute(key, null);
		if (v == null)
			return def;
		try {
			return Double.parseDouble(v.trim());
		} catch (Exception e) {
			return def;
		}
	}

	public Double getDouble(String key) {
		return getDouble(key, null);
	}

	public Double getDouble(String key, Double def) {
		String v = getAttribute(key, null);
		if (v == null)
			return def;
		try {
			return Double.parseDouble(v.trim());
		} catch (Exception e) {
			return def;
		}
	}

	public String getAttribute(String key) {
		return getAttribute(key, null);
	}

	public String dump() {
		StringBuilder sb = new StringBuilder();
		dump(sb, "");
		return sb.toString();
	}

	public void dump(StringBuilder sb, String prefix) {
		sb.append(MessageFormat.format("{0}{1}{2}{3}{4}{5}{6}\n", prefix, this, type != null ? " [" + type + "]" : "",
				this.order != 0 ? (" order: " + this.order) : "",
				this.locked != null ? (this.locked ? " [R/O]" : " [R/W]") : "", this.sorted ? " [sorted]" : "",
				this.translation != null ? ("Translation: " + this.translation) : ""));
		String currentPrefix = prefix + "\t";
		initAttributes();
		if (attributes != null) {
			attributes.entrySet().stream().sorted(Entry.comparingByKey()).forEach(a -> sb
					.append(MessageFormat.format("{0}[{1}]={2}\n", currentPrefix, a.getKey(), a.getValue().value)));
		}
		initChildren();
		if (children != null)
			for (FileObject f : children) {
				f.dump(sb, currentPrefix);
			}
	}

	/**
	 * Streams whole FileObject subtree (depth first)
	 *
	 * @return
	 */
	public Stream<FileObject> streamDFAllChildren(boolean includeSelf) {
		Deque<FileObject> stack = new ArrayDeque<>();
		if (includeSelf)
			stack.push(this);
		else {
			List<? extends FileObject> ch = getAllChildren();
			for (int i = ch.size() - 1; i >= 0; i--)
				stack.push(ch.get(i));
		}

		Iterator<FileObject> it = new Iterator<FileObject>() {
			@Override
			public boolean hasNext() {
				return !stack.isEmpty();
			}

			@Override
			public FileObject next() {
				FileObject n = stack.pop();

				List<? extends FileObject> ch = n.getAllChildren();
				for (int i = ch.size() - 1; i >= 0; i--)
					stack.push(ch.get(i));

				return n;
			}
		};

		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED | Spliterator.NONNULL),
				false);
	}

	/**
	 * Streams whole FileObject subtree (breadth first)
	 */
	public Stream<FileObject> streamBFAllChildren(boolean includeSelf) {
		Deque<FileObject> queue = new ArrayDeque<>();
		if (includeSelf)
			queue.add(this);
		else {
			List<? extends FileObject> ch = getAllChildren();
			queue.addAll(ch);
		}

		Iterator<FileObject> it = new Iterator<FileObject>() {
			@Override
			public boolean hasNext() {
				return !queue.isEmpty();
			}

			@Override
			public FileObject next() {
				FileObject n = queue.remove();

				List<? extends FileObject> ch = n.getAllChildren();
				queue.addAll(ch);

				return n;
			}
		};

		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED | Spliterator.NONNULL),
				false);
	}
}
