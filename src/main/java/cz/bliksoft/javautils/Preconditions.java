package cz.bliksoft.javautils;

import java.text.MessageFormat;

public final class Preconditions {

	private Preconditions() {
	}

	public static final String MUST_NOT_BE_NULL = "The {0} must not be null."; //$NON-NLS-1$
	public static final String MUST_NOT_BE_BLANK = "The {0} must not be null, empty, or whitespace."; //$NON-NLS-1$

	public static void checkArgument(boolean expression, String message) {
		if (!expression) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void checkArgument(boolean expression, String messageFormat, Object... messageArgs) {
		if (!expression) {
			throw new IllegalArgumentException(MessageFormat.format(messageFormat, messageArgs));
		}
	}

	public static <T> T checkNotNull(T reference, String message) {
		if (reference == null) {
			throw new NullPointerException(message);
		}
		return reference;
	}

	public static <T> T checkNotNull(T reference, String messageFormat, Object... messageArgs) {
		if (reference == null) {
			throw new NullPointerException(MessageFormat.format(messageFormat, messageArgs));
		}
		return reference;
	}

	public static String checkNotBlank(String str, String message) {
		checkNotNull(str, message);
		checkArgument(StringUtils.hasLength(str), message);
		return str;
	}

	public static String checkNotBlank(String str, String messageFormat, Object... messageArgs) {
		checkNotNull(str, messageFormat, messageArgs);
		checkArgument(StringUtils.hasLength(str), messageFormat, messageArgs);
		return str;
	}
}
