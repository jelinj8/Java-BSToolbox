package cz.bliksoft.javautils.exceptions;

public class InitializationException extends RuntimeException {
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
