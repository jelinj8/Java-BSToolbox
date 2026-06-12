package cz.bliksoft.javautils.xmlfilesystem;

import java.io.File;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;

import org.w3c.dom.Node;

import cz.bliksoft.javautils.StringUtils;

/**
 * Writable variant of {@link FileObject}, allowing attributes and children to
 * be modified and the changes saved to the source XML via the assigned
 * {@link WritableXmlFile}.
 */
public class WritableFileObject extends FileObject {

	private WritableXmlFile document;

	public WritableFileObject(Node xmlDefinition, FileObject parent, String resourceId, boolean writable) {
		super(xmlDefinition, parent, resourceId, writable);
	}

	/**
	 * creates a new (empty) writable object that can be added as a child of another
	 * writable object
	 */
	public WritableFileObject(String name, boolean folder, FileObject parent) {
		super();
		this.name = name;
		this.folder = folder;
		this.parent = parent;
		this.writable = true;
		this.resourceId = parent != null ? parent.getResourceId() : null;
	}

	@Override
	public boolean isWritable() {
		return true;
	}

	/**
	 * the document this object comes from, used by {@link #save()}
	 */
	public WritableXmlFile getDocument() {
		return document;
	}

	void setDocument(WritableXmlFile document) {
		this.document = document;
	}

	private void markDirty() {
		if (document != null)
			document.markDirty();
	}

	public void setAttribute(String key, String value) {
		setAttribute(key, value, null);
	}

	public void setAttribute(String key, String value, String translationId) {
		initAttributes();
		if (attributes == null)
			attributes = new HashMap<>();
		FileAttribute a = this.new FileAttribute();
		a.value = value;
		a.translationID = translationId;
		attributes.put(key, a);
		markDirty();
	}

	public void removeAttribute(String key) {
		initAttributes();
		if (attributes != null)
			attributes.remove(key);
		markDirty();
	}

	/**
	 * adds a child, linking it to the current document for later saving
	 */
	public void addChild(WritableFileObject child) {
		initChildren();
		if (children == null)
			children = new java.util.ArrayList<>();
		child.parent = this;
		child.document = this.document;
		this.folder = true;
		children.add(child);
		Collections.sort(children);
		markDirty();
	}

	public void removeChild(FileObject child) {
		initChildren();
		if (children != null)
			children.remove(child);
		markDirty();
	}

	public void setOrder(int order) {
		this.order = order;
		markDirty();
	}

	public void setSorted(boolean sorted) {
		this.sorted = sorted;
		markDirty();
	}

	public void setTranslation(String translation) {
		this.translation = translation;
		markDirty();
	}

	public void setType(String type) {
		this.type = type;
		markDirty();
	}

	public void setName(String name) {
		this.name = name;
		markDirty();
	}

	/**
	 * saves changes to the source document (if the object is attached to one)
	 */
	public void save() throws java.io.IOException {
		if (document != null)
			document.save();
	}

	/**
	 * analogous to {@link FileObject#getFile(String)}, but missing path segments
	 * (including the target file) are created as new empty
	 * {@link WritableFileObject} children
	 *
	 * @param fileName path composed of names in the hierarchy, separated by "/"
	 * @return the existing or newly created {@link WritableFileObject}
	 * @throws IllegalStateException if the path passes through an existing
	 *                               non-writable {@link FileObject}
	 */
	public WritableFileObject getCreateFile(String fileName) {
		File f = new File(fileName);
		Deque<String> lifo = new LinkedList<>();
		while (f != null) {
			if (StringUtils.hasText(f.getName()))
				lifo.push(f.getName());
			f = f.getParentFile();
		}

		FileObject start = fileName.startsWith("/") ? this.getRoot() : this; //$NON-NLS-1$
		if (!(start instanceof WritableFileObject))
			throw new IllegalStateException("File " + start.getFullPath() + " is not writable"); //$NON-NLS-1$ //$NON-NLS-2$
		WritableFileObject result = (WritableFileObject) start;

		while (!lifo.isEmpty()) {
			String segment = lifo.pop();
			FileObject existing = result.getFile(segment);
			if (existing != null) {
				if (!(existing instanceof WritableFileObject))
					throw new IllegalStateException("File " + existing.getFullPath() + " is not writable"); //$NON-NLS-1$ //$NON-NLS-2$
				result = (WritableFileObject) existing;
			} else {
				WritableFileObject child = new WritableFileObject(segment, false, result);
				result.addChild(child);
				result = child;
			}
		}
		return result;
	}

	/**
	 * analogous to {@link #getCreateFile(String)} for a path composed of several
	 * individual segments
	 */
	public WritableFileObject getCreateFile(String fileName, String... subpaths) {
		StringBuilder path = new StringBuilder("/".equals(fileName) ? "" : fileName); //$NON-NLS-1$ //$NON-NLS-2$

		if (subpaths == null || subpaths.length < 1)
			return getCreateFile(fileName);

		for (String sp : subpaths) {
			path.append("/"); //$NON-NLS-1$
			path.append(sp);
		}
		return getCreateFile(path.toString());
	}
}
