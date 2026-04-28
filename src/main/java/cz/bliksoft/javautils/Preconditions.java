package cz.bliksoft.javautils;

import java.text.MessageFormat;

/**
 * Static guard methods that throw standard Java exceptions on constraint
 * violations. Serves as a lightweight alternative to Guava's
 * {@code Preconditions}.
 */
public final class Preconditions {

	private Preconditions() {
	}

	public static final String MUST_NOT_BE_NULL = "The {0} must not be null."; //$NON-NLS-1$
	public static final String MUST_NOT_BE_BLANK = "The {0} must not be null, empty, or whitespace."; //$NON-NLS-1$

	/**
	 * Throws {@link IllegalArgumentException} with {@code message} if
	 * {@code expression} is false.
	 *
	 * @param expression condition that must be true
	 * @param message    exception message
	 */
	public static void checkArgument(boolean expression, String message) {
		if (!expression) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Throws {@link IllegalArgumentException} with a formatted message if
	 * {@code expression} is false.
	 *
	 * @param expression    condition that must be true
	 * @param messageFormat {@link java.text.MessageFormat} pattern
	 * @param messageArgs   arguments for the pattern
	 */
	public static void checkArgument(boolean expression, String messageFormat, Object... messageArgs) {
		if (!expression) {
			throw new IllegalArgumentException(MessageFormat.format(messageFormat, messageArgs));
		}
	}

	/**
	 * Throws {@link NullPointerException} with {@code message} if {@code reference}
	 * is null.
	 *
	 * @param reference value to check
	 * @param message   exception message
	 * @return {@code reference} for chaining
	 */
	public static <T> T checkNotNull(T reference, String message) {
		if (reference == null) {
			throw new NullPointerException(message);
		}
		return reference;
	}

	/**
	 * Throws {@link NullPointerException} with a formatted message if
	 * {@code reference} is null.
	 *
	 * @param reference     value to check
	 * @param messageFormat {@link java.text.MessageFormat} pattern
	 * @param messageArgs   arguments for the pattern
	 * @return {@code reference} for chaining
	 */
	public static <T> T checkNotNull(T reference, String messageFormat, Object... messageArgs) {
		if (reference == null) {
			throw new NullPointerException(MessageFormat.format(messageFormat, messageArgs));
		}
		return reference;
	}

	/**
	 * Throws if {@code str} is null, empty, or whitespace-only.
	 *
	 * @param str     string to check
	 * @param message exception message
	 * @return {@code str} for chaining
	 */
	public static String checkNotBlank(String str, String message) {
		checkNotNull(str, message);
		checkArgument(StringUtils.hasLength(str), message);
		return str;
	}

	/**
	 * Throws if {@code str} is null, empty, or whitespace-only, using a formatted
	 * message.
	 *
	 * @param str           string to check
	 * @param messageFormat {@link java.text.MessageFormat} pattern
	 * @param messageArgs   arguments for the pattern
	 * @return {@code str} for chaining
	 */
	public static String checkNotBlank(String str, String messageFormat, Object... messageArgs) {
		checkNotNull(str, messageFormat, messageArgs);
		checkArgument(StringUtils.hasLength(str), messageFormat, messageArgs);
		return str;
	}
}
