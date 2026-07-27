package cz.bliksoft.javautils.app.permissions;

/**
 * Sentinel returned by {@code Permissions.getByName()} for unknown or
 * unregistered permission classes; no user ever holds it.
 */
public class NotAllowedPermission extends Permission {

	@Override
	public String getName() {
		return "NotAllowed";
	}

	@Override
	public String getAlias() {
		return "NotAllowed";
	}

}
