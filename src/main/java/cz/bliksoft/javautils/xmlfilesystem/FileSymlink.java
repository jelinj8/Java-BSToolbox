package cz.bliksoft.javautils.xmlfilesystem;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;

public class FileSymlink extends FileObject {

	private String targetPath;
	private FileObject targetFile = null;

	public FileObject getTarget() {
		if (targetFile == null) {
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
		this.children = targetFile.children;
	}

	private boolean initializedAttributes = false;

	@Override
	protected void initAttributes() {
		super.initAttributes();
		if (initializedAttributes)
			return;
		initializedAttributes = true;
		
		getTarget();
		if (targetFile.getAttributes() != null) {
			if (attributes == null) {
				attributes = new HashMap<String, String>();
				attributes.putAll(targetFile.getAttributes());
			} else {
				Map<String, String> attNew = new HashMap<>(targetFile.getAttributes());
				attNew.putAll(attributes);
				attributes = attNew;
			}
		}
	}

	public FileSymlink(Node xmlDefinition, FileObject parent, String resourceId) {
		super(xmlDefinition, parent, resourceId);
		targetPath = attributes.get(targetPath);
	}

	@Override
	public String toString() {
		return "LNK:" + this.name + " (" + this.resourceId + ") -> " + getTarget().toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
