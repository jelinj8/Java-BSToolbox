package cz.bliksoft.javautils.threads;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public abstract class MessageInterceptWorker extends Thread {
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
	 * výjimka, ke které dojde během zpracovávání - pro pozdější vyhodnocení/logování 
	 */
	protected Exception callException = null;
	
	/**
	 * getter na výjimku zpracování
	 * @see #callException 
	 * @return
	 */
	public Exception getException(){
		return callException;
	}
	
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
