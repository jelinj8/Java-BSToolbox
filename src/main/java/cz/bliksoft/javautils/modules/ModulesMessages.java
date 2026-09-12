package cz.bliksoft.javautils.modules;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ModulesMessages {
	private static final String BUNDLE_NAME = "cz.bliksoft.javautils.modules.ModulesMessages"; //$NON-NLS-1$

	private static ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private ModulesMessages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	/**
	 * Substitutes {@code {}} placeholders (in encounter order, log4j
	 * {@code ParameterizedMessage} style - this bundle's entries are written for
	 * that, not for {@link java.text.MessageFormat}'s {@code {0}} style) with
	 * {@code params}, in the retrieved bundle string for {@code key}.
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

	public static void reload() {
		RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
	}
}
