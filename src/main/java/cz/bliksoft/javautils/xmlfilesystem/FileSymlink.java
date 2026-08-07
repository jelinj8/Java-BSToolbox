package cz.bliksoft.javautils.xmlfilesystem;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.exceptions.InitializationException;

public class FileSymlink extends FileObject {

	private String targetPath;
	private String targetId;
	private FileObject targetFile = null;

	private boolean targetInitialized = false;

	/**
	 * @return the unresolved path to the target file
	 */
	public String getTargetPath() {
		return targetPath;
	}

	/**
	 * @return the id of the target file, resolved via
	 *         {@link FileObject#getFileByID(String)}
	 */
	public String getSymlinkTargetId() {
		return targetId;
	}

	public FileObject getTargetFile() {
		if (!targetInitialized) {
			targetInitialized = true;
			if (targetPath != null)
				targetFile = getFile(targetPath);
			else
				targetFile = FileObject.getFileByID(targetId);
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
		getTargetFile();
		if (targetFile != null)
			this.children = targetFile.children;
	}

	private boolean initializedAttributes = false;

	@Override
	public Boolean getLocked() {
		getTargetFile();
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

		getTargetFile();
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

		Node pathNode = xmlDefinition.getAttributes().getNamedItem(FileObject.SYMLINK_FILE_PATH);
		targetPath = pathNode != null ? pathNode.getTextContent() : null;
		Node targetIdNode = xmlDefinition.getAttributes().getNamedItem(FileObject.SYMLINK_TARGET_ID);
		targetId = targetIdNode != null ? targetIdNode.getTextContent() : null;

		if (targetPath == null && targetId == null) {
			throw new InitializationException(StringUtils
					.format("<symlink> requires a 'path' or 'target-id' attribute (resource {0})", resourceId));
		}

		if (this.name == null) {
			// no 'name' given: resolve the target eagerly (unlike getTargetFile(), which
			// stays
			// lazy for attribute/children inheritance) so this symlink can be indexed by
			// name
			// immediately, like every other FileObject
			FileObject resolved = targetPath != null ? getFile(targetPath) : FileObject.getFileByID(targetId);
			if (resolved == null) {
				throw new InitializationException(StringUtils.format(
						"Cannot infer <symlink> name: target not yet resolvable ({0}, resource {1}). "
								+ "Declare the target earlier, or specify a 'name' attribute explicitly.",
						targetPath != null ? "path=" + targetPath : "target-id=" + targetId, resourceId));
			}
			this.name = resolved.getName();
		}
	}

	@Override
	public String toString() {
		String targetDescription = targetPath != null ? targetPath : "target-id:" + targetId; //$NON-NLS-1$
		if (getTargetFile() != null)
			return "LINK:" + this.name + " (" + this.resourceId + ") -> " + getTargetFile().getFullPath(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		else
			return "BROKEN LINK:" + this.name + " (" + this.resourceId + ") -> !" + targetDescription; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
