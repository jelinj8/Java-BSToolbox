package cz.bliksoft.javautils.exceptions;

public class ProcessingException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 59530808317039051L;

	public ProcessingException(String message) {
		super(message);
	}

	public ProcessingException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public ProcessingException(Throwable throwable) {
		super(throwable);
	}
}
