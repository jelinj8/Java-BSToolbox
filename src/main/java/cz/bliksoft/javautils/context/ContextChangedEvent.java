package cz.bliksoft.javautils.context;

public class ContextChangedEvent<T> {
	ContextSearchResult oldValue = new ContextSearchResult(null, null);
	ContextSearchResult newValue = new ContextSearchResult(null, null);
	boolean blockPropagation = false;

	public ContextChangedEvent(ContextSearchResult oldValue, ContextSearchResult newValue) {
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	public void blockEventPropagation() {
		blockPropagation = true;
	}

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

	public boolean isOldValid() {
		return oldValue.isValid();
	}

	public boolean isNewValid() {
		return newValue.isValid();
	}

	public boolean isNewNotNull() {
		return newValue.isValid() && (newValue.getResult() != null);
	}

	public boolean isOldNotNull() {
		return oldValue.isValid() && (oldValue.getResult() != null);
	}

	@SuppressWarnings("unchecked")
	public T getOldValue() {
		if (oldValue.isValid())
			return (T) oldValue.getResult();
		return null;
	}

	@SuppressWarnings("unchecked")
	public T getNewValue() {
		if (newValue.isValid())
			return (T) newValue.getResult();
		return null;
	}
}
