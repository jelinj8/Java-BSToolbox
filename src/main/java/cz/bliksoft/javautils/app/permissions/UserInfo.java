package cz.bliksoft.javautils.app.permissions;

import java.util.Set;

/** Describes the current user by the set of permissions they hold. */
public abstract class UserInfo {

	public abstract Set<Class<? extends Permission>> getCurrentPermissionSet();

	public boolean isAllowed(Class<? extends Permission> permission) {
		return getCurrentPermissionSet().contains(permission);
	}
}
