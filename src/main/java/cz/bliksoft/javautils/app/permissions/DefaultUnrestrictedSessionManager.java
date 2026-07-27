package cz.bliksoft.javautils.app.permissions;

/**
 * Session manager granting all permissions to a single anonymous user.
 * Installed automatically by {@code BSApp.init()} when no session manager was
 * set.
 */
public class DefaultUnrestrictedSessionManager extends SessionManager {

	UserInfo ui = new DefaultAllmightyUserInfo();

	@Override
	public UserInfo getUserInfo() {
		return ui;
	}

	@Override
	public String toString() {
		return "DefaultUnrestrictedSessionManager";
	}
}
