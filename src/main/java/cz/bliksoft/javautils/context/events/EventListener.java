package cz.bliksoft.javautils.context.events;

import cz.bliksoft.javautils.exceptions.InitializationException;

public abstract class EventListener<T> {

	protected Class<?> eventClass;
	private String comment;

	protected EventListener(Class<?> cls) {
		eventClass = cls;
	}

	protected EventListener(Class<?> eventClass, String comment) {
		this.comment = comment;
		this.eventClass = eventClass;
	}

	public boolean accepts(Class<?> cls) {
		return eventClass.isAssignableFrom(cls);
	}

	public String getComment() {
		return this.comment;
	}

	@SuppressWarnings("unchecked")
	/**
	 * notify listener if applicable
	 *
	 * @param event
	 * @return event consumed (stop further notifications)
	 */
	public boolean fire(Object event) {
		if (accepts(event.getClass())) {
			boolean process = false;
			Boolean processed = null;
			try {
				process = beforeEvent((T) event);
				if (process) {
					fired((T) event);
				}
				processed = process;
			} finally {
				afterEvent(processed, (T) event);
			}

			if (event instanceof IConsumableEvent) {
				return ((IConsumableEvent) event).isConsumed();
			}
		}
		return false;
	}

	/**
	 * process event listener
	 *
	 * @param event
	 */
	public abstract void fired(T event);

	/**
	 * called before main processing part.
	 *
	 * @param event
	 * @return return false to skip rest of processing
	 */
	public boolean beforeEvent(T event) {
		return true;
	}

	/**
	 * called after main event processing
	 *
	 * @param processed set to false if processing was skipped, null if it was
	 *                  interrupted by an exception, true if processed.
	 * @param event
	 */
	public void afterEvent(Boolean processed, T event) {
	}

	@Override
	public String toString() {
		return "EVT LISTENER: [" + eventClass.getSimpleName() + "]" + (comment == null ? "" : " " + comment);
	}

	private static Thread edt = null;

	/**
	 * Call once from target thread to set EDT to checked if events are firing on
	 * this thread (Swing EDT, JavaFX Event thread any custom app thread). If not
	 * set, check won't be enforced.
	 */
	public static void linkEdt() {
		if (edt != null)
			throw new InitializationException("EDT already bound!");
		edt = Thread.currentThread();
	}

	/**
	 * soft check if called from optionally configured EventDispatching thread.
	 *
	 * @return
	 */
	public static boolean isEdt() {
		if (edt == null)
			return true;
		return edt == Thread.currentThread();
	}

	/**
	 * hard check for call from EDT. Throws runtime exceptions if called from other
	 * thread or the EDT is not configured.
	 */
	public static void eforceEdt() {
		if (edt == null)
			throw new InitializationException("EDT not bound!");
		if (edt == Thread.currentThread())
			throw new RuntimeException("Called from non-EDT thread!");
	}
}
