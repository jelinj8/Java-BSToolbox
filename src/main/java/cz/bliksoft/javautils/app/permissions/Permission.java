package cz.bliksoft.javautils.app.permissions;

import cz.bliksoft.javautils.app.BSAppMessages;

/**
 * A single named application permission. Subclass and register via the XML
 * filesystem ({@code core/permissions} folder) or Java SPI
 * ({@code META-INF/services}); permissions are identified by their class.
 */
public abstract class Permission {
	public abstract String getName();

	public abstract String getAlias();

	public String getCategory() {
		return BSAppMessages.getString("Permissions.UNCATEGORIZED");
	}

	public String getShortDescription() {
		return null;
	}

	public String getDescription() {
		return null;
	}
}
