package cz.bliksoft.javautils.scheduler;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for tasks managed by {@link TaskScheduler}. Subclasses implement
 * {@link #perform()} with the actual work logic.
 */
public abstract class Task {

	protected Logger log = null;

	protected Task(String name) {
		this.name = name;
		log = Logger.getLogger(this.getClass().getSimpleName() + "[" + this.name + "]");
	}

	/**
	 * Executes the task body. Thrown exceptions are caught and forwarded to
	 * {@link #exception(Exception)}.
	 */
	public abstract void perform() throws Exception;

	/**
	 * Called after a successful {@link #perform()} invocation. Override to react to
	 * completion.
	 */
	public void success() {

	}

	/**
	 * Called when {@link #perform()} throws an exception. Default implementation
	 * logs at SEVERE level.
	 */
	public void exception(Exception e) {
		log.log(Level.SEVERE, "Task " + name + " threw an exception: ", e);
	}

	/**
	 * Interval after which a new task execution is scheduled.
	 *
	 * @return negative — rescheduled just before the next run starts; positive —
	 *         rescheduled after the previous run completes
	 */
	public long getRepeatInterval() {
		return repeatInterval;
	}

	/**
	 * Sets the repeat interval in milliseconds. Negative value reschedules before
	 * run, positive after.
	 */
	public void setRepeatInterval(long repeatInterval) {
		this.repeatInterval = repeatInterval;
	}

	/**
	 * Returns the initial delay in milliseconds before the first execution after
	 * scheduling.
	 */
	public long getInitialDelay() {
		return initialDelay;
	}

	/**
	 * Sets the initial delay in milliseconds before the first execution after
	 * scheduling.
	 */
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

	/**
	 * Returns the epoch-millisecond timestamp when this task last started
	 * execution.
	 */
	public long getLastRun() {
		return lastRun;
	}

	/** Returns the number of milliseconds the most recent execution took. */
	public long getLastTimeConsumed() {
		return lastTimeConsumed;
	}

	private long repeatInterval = 0;
	private long initialDelay = 0;

	// /**
	// * Calculates timestamp for next execution.
	// * @return timestamp when the event should be executed, or 0 if it should not
	// run
	// */
	// public long getNextRun() {
	// if (repeatInterval == 0)
	// return 0;
	// return (new Date()).getTime() + Math.abs(repeatInterval);
	// }

	protected String name = null;

	/**
	 * Returns the task name, falling back to {@link #toString()} if no name was
	 * set.
	 */
	public String getName() {
		if (name == null)
			return toString();
		return name;
	}

	/** Epoch-millisecond timestamp of the next scheduled run; 0 means disabled. */
	protected long nextRun = 0;
}
