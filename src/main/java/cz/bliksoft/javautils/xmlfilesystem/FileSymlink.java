package cz.bliksoft.javautils.xmlfilesystem;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;

public class FileSymlink extends FileObject {

	private String targetPath;
	private FileObject targetFile = null;

	private boolean targetInitialized = false;

	public FileObject getTarget() {
		if (!targetInitialized) {
			targetInitialized = true;
			targetFile = getFile(targetPath);
		}
		return targetFile;
	}

	private boolean initializedChildren = false;

	@Override
	protected void initChildren() {
		super.initChildren();
		if (initializedChildren)
			return;
		initializedChildren = true;
		getTarget();
		if (targetFile != null)
			this.children = targetFile.children;
	}

	private boolean initializedAttributes = false;

	@Override
	public Boolean getLocked() {
		getTarget();
		if (targetFile != null)
			return targetFile.getLocked();
		else
			return true;
	}

	@Override
	protected void initAttributes() {
		super.initAttributes();
		if (initializedAttributes)
			return;
		initializedAttributes = true;

		getTarget();
		if (targetFile != null && targetFile.getAttributes() != null) {
			if (attributes == null) {
				attributes = new HashMap<String, FileObject.FileAttribute>();
				attributes.putAll(targetFile.getAttributes());
			} else {
				Map<String, FileObject.FileAttribute> attNew = new HashMap<>(targetFile.getAttributes());
				attNew.putAll(attributes);
				attributes = attNew;
			}
		}
	}

	public FileSymlink(Node xmlDefinition, FileObject parent, String resourceId) {
		super(xmlDefinition, parent, resourceId);
		Node targetNode = xmlDefinition.getAttributes().getNamedItem(FileObject.SYMLINK_FILE_PATH);
		targetPath = targetNode.getTextContent();
	}

	@Override
	public String toString() {
		if (getTarget() != null)
			return "LINK:" + this.name + " (" + this.resourceId + ") -> " + getTarget().getFullPath(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		else
			return "BROKEN LINK:" + this.name + " (" + this.resourceId + ") -> !" + targetPath; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
