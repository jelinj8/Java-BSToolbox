package cz.bliksoft.javautils.context;

/**
 * Event passed to an {@link AbstractContextListener} when the watched context
 * value changes.
 */
public class ContextChangedEvent<T> {
	ContextSearchResult oldValue = new ContextSearchResult(null, null);
	ContextSearchResult newValue = new ContextSearchResult(null, null);
	boolean blockPropagation = false;

	public ContextChangedEvent(ContextSearchResult oldValue, ContextSearchResult newValue) {
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	/**
	 * Prevents the change notification from propagating further up the context
	 * hierarchy.
	 */
	public void blockEventPropagation() {
		blockPropagation = true;
	}

	/** Returns true if propagation has been blocked by this event. */
	public boolean isPropagationBlocked() {
		return blockPropagation;
	}

//	public ContextSearchResult getOldResult() {
//		return oldValue;
//	}
//
//	public ContextSearchResult getNewResult() {
//		return oldValue;
//	}

	/** Returns true if there was a previous valid value. */
	public boolean isOldValid() {
		return oldValue.isValid();
	}

	/** Returns true if the new value is valid (key was found in the context). */
	public boolean isNewValid() {
		return newValue.isValid();
	}

	/** Returns true if the new value is valid and non-null. */
	public boolean isNewNotNull() {
		return newValue.isValid() && (newValue.getResult() != null);
	}

	/** Returns true if the old value was valid and non-null. */
	public boolean isOldNotNull() {
		return oldValue.isValid() && (oldValue.getResult() != null);
	}

	/** Returns the previous value cast to {@code T}, or null if it was invalid. */
	@SuppressWarnings("unchecked")
	public T getOldValue() {
		if (oldValue.isValid())
			return (T) oldValue.getResult();
		return null;
	}

	/** Returns the new value cast to {@code T}, or null if it is invalid. */
	@SuppressWarnings("unchecked")
	public T getNewValue() {
		if (newValue.isValid())
			return (T) newValue.getResult();
		return null;
	}
}
