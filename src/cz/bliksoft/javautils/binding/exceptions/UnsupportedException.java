package cz.bliksoft.javautils.binding.exceptions;

public class UnsupportedException extends PropertyException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7025524363240325483L;

	public UnsupportedException(String message) {
		this(message, null);
	}

	public UnsupportedException(String message, Throwable cause) {
		super(message, cause);
	}
}
