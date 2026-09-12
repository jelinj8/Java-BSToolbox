package cz.bliksoft.javautils.app;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Provides access to localized application messages from
 * {@code BSAppMessages.properties}. Returns {@code !key!} when a key is missing
 * so that UI problems are immediately visible without throwing exceptions.
 */
public class BSAppMessages {
	private static final String BUNDLE_NAME = "cz.bliksoft.javautils.app.BSAppMessages"; //$NON-NLS-1$

	private static ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private BSAppMessages() {
	}

	/**
	 * Returns the localized string for the given key, or {@code !key!} if the key
	 * is missing.
	 *
	 * @param key the message key
	 *
	 * @return the localized string; never {@code null}
	 */
	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	/**
	 * Returns the localized string for the given key, substituting {@code {}}
	 * placeholders (in encounter order, log4j {@code ParameterizedMessage} style -
	 * this bundle's entries are written for that, not for
	 * {@link java.text.MessageFormat}'s {@code {0}} style) with {@code params}.
	 *
	 * @param key    the message key
	 * @param params substituted for each {@code {}} placeholder, in order
	 *
	 * @return the formatted string; never {@code null}
	 */
	public static String getString(String key, Object... params) {
		try {
			String pattern = RESOURCE_BUNDLE.getString(key);
			StringBuilder sb = new StringBuilder(pattern.length());
			int argIndex = 0;
			for (int i = 0; i < pattern.length(); i++) {
				char c = pattern.charAt(i);
				if (c == '{' && i + 1 < pattern.length() && pattern.charAt(i + 1) == '}') {
					sb.append(argIndex < params.length ? String.valueOf(params[argIndex++]) : "{}");
					i++;
				} else {
					sb.append(c);
				}
			}
			return sb.toString();
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	/** Reloads the resource bundle (e.g. after changing the default locale). */
	public static void reload() {
		RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
	}
}
