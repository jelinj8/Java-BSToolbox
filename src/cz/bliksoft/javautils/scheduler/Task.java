package cz.bliksoft.javautils.scheduler;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Task {

	protected Logger log = null;

	public Task(String name) {
		this.name = name;
		log = Logger.getLogger(this.getClass().getSimpleName() + "[" + this.name + "]");
	}

	public abstract void perform() throws Exception;

	public void success() {

	}

	public void exception(Exception e) {
		log.log(Level.SEVERE, "Task " + name + " threw an exception: ", e);
	}

	/**
	 * interval, za jak dlouho bude naplánováno nové spuštění úlohy
	 * 
	 * @return záporný - naplánováno těsně před spuštěním, kladný - naplánováno až
	 *         po doběhnutí předchozího spuštění
	 */
	public long getRepeatInterval() {
		return repeatInterval;
	}

	public void setRepeatInterval(long repeatInterval) {
		this.repeatInterval = repeatInterval;
	}

	public long getInitialDelay() {
		return initialDelay;
	}

	public void setInitialDelay(long initialDelay) {
		this.initialDelay = initialDelay;
	}

	private long lastRun = 0;
	private long lastTimeConsumed = 0;
	Exception lastException = null;

	void doRun() {
		lastRun = (new Date()).getTime();
		try {
			perform();
			lastException = null;
			success();
		} catch (Exception e) {
			lastException = e;
			exception(e);
		} finally {
			lastTimeConsumed = (new Date()).getTime() - lastRun;
		}
	}

	public long getLastRun() {
		return lastRun;
	}

	public long getLastTimeConsumed() {
		return lastTimeConsumed;
	}

	private long repeatInterval = 0;
	private long initialDelay = 0;

	// /**
	// * spočítá timestamp pro příští spuštění
	// *
	// * @return timestamp, kdy má být událost spuštěna nebo 0, pokud být spuštěna
	// * nemá
	// */
	// public long getNextRun() {
	// if (repeatInterval == 0)
	// return 0;
	// return (new Date()).getTime() + Math.abs(repeatInterval);
	// }

	protected String name = null;

	public String getName() {
		if (name == null)
			return toString();
		return name;
	}

	/**
	 * timestamp, kdy by mělo dojít k příštímu spuštění, 0 pro disable
	 */
	protected long nextRun = 0;
}
