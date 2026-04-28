package cz.bliksoft.javautils;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** Accessor for the library's internal message resource bundle. */
public class Messages {
	private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".Messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private Messages() {
	}

	/**
	 * Returns the message for {@code key}, or {@code !key!} if the key is missing
	 * from the bundle.
	 *
	 * @param key resource bundle key
	 * @return localised message string
	 */
	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
