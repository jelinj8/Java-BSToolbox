package cz.bliksoft.javautils.threads;

/**
 * Extension of {@link MessageInterceptWorker} that captures a typed result
 * produced by the worker thread.
 *
 * @param <T> result type
 */
public abstract class MessageWorkerWithResult<T> extends MessageInterceptWorker {

	protected T result = null;

	/**
	 * Returns the result produced by the worker, or {@code null} if not yet
	 * available.
	 */
	public T getResult() {
		return result;
	}

	public MessageWorkerWithResult() {
	}

}
