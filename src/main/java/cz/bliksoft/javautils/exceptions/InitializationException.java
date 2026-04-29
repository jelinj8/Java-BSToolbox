package cz.bliksoft.javautils.exceptions;

/** Unchecked exception thrown when a component fails to initialise. */
public class InitializationException extends RuntimeException {
	/**
	 *
	 */
	private static final long serialVersionUID = 5953081008317039051L;

	public InitializationException(String message) {
		super(message);
	}

	public InitializationException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public InitializationException(Throwable throwable) {
		super(throwable);
	}
}
