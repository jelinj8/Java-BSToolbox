package cz.bliksoft.javautils.context.events;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Event listener processed again only after previous run finishes
 * 
 * @author jelinj8
 *
 * @param <T>
 */
public abstract class ConcurrentEventListener<T> extends EventListener<T> {

	protected ConcurrentEventListener(Class<T> cls) {
		super(cls);
	}

	protected ConcurrentEventListener(Class<T> cls, String comment) {
		super(cls, comment);
	}

	AtomicBoolean currentlyRunning = new AtomicBoolean(false);

	/**
	 * skip another run while current is running
	 */
	@Override
	public boolean beforeEvent(T event) {
		if (currentlyRunning.get())
			return false;
		else
			currentlyRunning.set(true);
		return true;
	}

	/**
	 * unlocks event to be re-run
	 */
	@Override
	public void afterEvent(Boolean processed, T event) {
		if (processed)
			currentlyRunning.set(false);
	}

	/**
	 * unlocks re-running of event listener process
	 */
	public void resetRunning() {
		currentlyRunning.set(false);
	}
}
