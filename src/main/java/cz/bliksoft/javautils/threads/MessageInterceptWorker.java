package cz.bliksoft.javautils.threads;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Thread that intercepts a two-phase message exchange: a "prepare" phase where
 * the worker produces a record, and a "sign/modify" phase where the caller can
 * modify it before the worker proceeds. Synchronisation is handled via
 * semaphores.
 */
public abstract class MessageInterceptWorker extends Thread {

	public MessageInterceptWorker(String threadName) {
		setName(threadName);
	}

	public MessageInterceptWorker() {
		setName("MessageInterceptWorker");
	}

	protected Logger log = Logger.getLogger(this.getClass().toString());

	public final Semaphore prepareLock = new Semaphore(0, true);
	// public final Lock prepareLock = new ReentrantLock(true);
	private boolean prepared = false;

	public final Semaphore signedLock = new Semaphore(0, true);
	// public final Lock sentLock = new ReentrantLock(true);
	private boolean modified = false;

	private String preparedRecord;

	private String modifiedRecord;

	/**
	 * Exception thrown during processing, stored for later evaluation or logging.
	 */
	protected Exception callException = null;

	/**
	 * Returns the exception thrown during processing, or {@code null} if no
	 * exception occurred.
	 *
	 * @see #callException
	 */
	public Exception getException() {
		return callException;
	}

	/**
	 * Blocks until the worker signals the prepare phase is complete, with a
	 * 5-minute timeout.
	 */
	public boolean waitForPrepare() throws Exception {
		if (prepareLock.tryAcquire(5, TimeUnit.MINUTES)) {
			prepareLock.release();
			return true;
		} else {
			return false;
		}
	}

	public void setPreparedRecord(String record) {
		log.fine("setPreparedRecord");
		preparedRecord = record;
		prepared = true;
		prepareLock.release();
	}

	public String getPreparedRecord() {
		log.fine("getPreparedRecord");
		if (prepared)
			return preparedRecord;
		else
			return null;
	}

	public void setModifiedRecord(String record) {
		log.fine("setModifiedRecord");
		modifiedRecord = record;
		modified = true;
		signedLock.release();
	}

	public String getModifiedRecord() {
		log.fine("getModifiedRecord");
		if (modified) {
			return modifiedRecord;
		} else {
			return null;
		}
	}

}
