package cz.bliksoft.javautils.app.permissions;

import java.util.Set;

/** User that holds every registered permission. */
public class DefaultAllmightyUserInfo extends UserInfo {

	@Override
	public Set<Class<? extends Permission>> getCurrentPermissionSet() {
		return Permissions.getFullSet();
	}

}
