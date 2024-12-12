package cz.bliksoft.javautils.threads;

public abstract class MessageWorkerWithResult<T> extends MessageInterceptWorker {

	protected T result = null;

	public T getResult() {
		return result;
	}

	public MessageWorkerWithResult() {
	}

}
