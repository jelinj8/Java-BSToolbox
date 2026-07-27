package cz.bliksoft.javautils.app.permissions;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides the current user ({@link UserInfo}) and arbitrary session
 * properties. Install once via {@code BSApp.setSessionManager()} before
 * {@code BSApp.init()}.
 */
public abstract class SessionManager {

	public abstract UserInfo getUserInfo();

	protected Map<String, Object> sessionProperties = new HashMap<>();

	public Object getSessionProperty(String name) {
		return sessionProperties.get(name);
	}
}
