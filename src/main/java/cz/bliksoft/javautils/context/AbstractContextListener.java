package cz.bliksoft.javautils.context;

import java.text.MessageFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.context.events.EventListener;

/** Base for listeners that observe a specific key in the context hierarchy. */
public abstract class AbstractContextListener<T> extends EventListener<ContextChangedEvent<T>> {

//	@SuppressWarnings("unchecked")
	protected AbstractContextListener(Object key, String comment) {
		super(ContextChangedEvent.class, comment);

//		super((Class<ContextChangedEvent<T>>) key /* .getClass() */, comment);
//		super((Class<ContextChangedEvent<T>>) ContextChangedEvent.class, comment);
//		super((Class<ContextChangedEvent<T>>)ContextChangedEvent.class, comment);
//		super(null, comment);
		this.comment = comment;
		this.key = key;
	}

//	@Override
//	public abstract void fired(ContextChangedEvent<T> event);

	private static final Logger log = LogManager.getLogger();

	/** The context key this listener watches. */
	Object key;

	/** Returns the key this listener is watching. */
	public Object getKey() {
		return this.key;
	}

	/**
	 * Previously observed value; used to detect changes and replay on reactivation.
	 */
	ContextSearchResult oldValue = new ContextSearchResult(null, null);
	ContextSearchResult currentValue = new ContextSearchResult(null, null);

	private boolean wasModified = false;

	/**
	 * Returns true if this listener should react to a change notification for the
	 * given key.
	 */
	public boolean isInterrested(Object resultKey, Integer levelsCrossed) {
		if ((maxLevelsCrossed > -1) && (levelsCrossed > maxLevelsCrossed)) {
			return false;
		} else if ((this.key instanceof Class) && (resultKey instanceof Class)) {
			if (((Class<?>) this.key).isAssignableFrom((Class<?>) resultKey))
				return true;
		} else if (this.key.equals(resultKey))
			return true;
		else if (log.isDebugEnabled())
			log.trace("{} is not interrested in value {}", this, resultKey);
		return false;
	}

	/** Triggers a re-evaluation of the listener's current value. */
	protected final void fireUpdate() {
		if (!this.active) {
			this.setActive(true);
			this.setActive(false);
		}
	}

	/**
	 * Notifies this listener that the watched value may have changed; fires it if
	 * active.
	 */
	protected final Boolean fireContextChanged(ContextSearchResult contextSearchResult) {
		if (this.active) {
			log.trace("Listener ''{}'' updated from ''{}'' to ''{}''", this, Context.getAbbrevDescription(oldValue),
					Context.getAbbrevDescription(contextSearchResult));

			// Boolean result = this.contextChanged(this.oldValue, contextSearchResult);
			ContextChangedEvent<T> event = new ContextChangedEvent<>(oldValue, contextSearchResult);
			this.fire(event);

			this.oldValue = this.currentValue;
			this.currentValue = contextSearchResult;
			this.wasModified = false;
			return event.isPropagationBlocked();
		} else {
			this.currentValue = contextSearchResult;
			this.wasModified = true;
			return true;
		}
	}

	// /**
	// * ošetření změny hodnoty kontextu pro daný klíč
	// *
	// * @param oldValue
	// * předchozí hodnota
	// * @param newValue
	// * aktuální hodnota
	// * @return false pro zastavení šíření na vyšší úrovně, jinak true
	// */
	// public abstract boolean contextChanged(ContextSearchResult oldValue,
	// ContextSearchResult newValue);

	@Override
	public String toString() {

		return "LISTENER: [" + this.key.toString() + "]" //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				+ (maxLevelsCrossed > -1 ? " LTL:" + maxLevelsCrossed : "") //$NON-NLS-1$
//				+ (currentValue.isValid() ? MessageFormat.format(" = {0}[{1}] = {2}", currentValue.getContext(), //$NON-NLS-1$
//						currentValue.getKey(), ObjectUtils.getAbbrevDescription(currentValue.getResult()))
//						: " (invalid)")
				+ (currentValue.isValid()
						? MessageFormat.format(" = {0}", Context.getAbbrevDescription(currentValue.getResult()))
						: " (invalid)")
				+ (this.active ? "" : " (deactivated)") //$NON-NLS-1$
				+ (StringUtils.hasText(this.comment) ? MessageFormat.format(" \"{0}\"", this.comment) : "");
	}

	public static final String PROP_ACTIVE = "active"; //$NON-NLS-1$
	private boolean active = true;

	/** Returns whether this listener is currently active. */
	public boolean getActive() {
		return this.active;
	}

	/**
	 * Activates or deactivates the listener; on reactivation, replays any missed
	 * changes.
	 */
	public void setActive(boolean newValue) {
		boolean oldActive = active;
		this.active = newValue;
		if (oldActive != newValue) {
			if (newValue) { // reaktivace s dohnáním změn
				if (this.wasModified) {
					this.fireContextChanged(this.currentValue);
				}
			} else { // uspání
				this.wasModified = false;
			}
		}
	}

	protected String comment = "";

	protected Integer maxLevelsCrossed = -1;

	/**
	 * Limits how many level boundaries a change may cross to still trigger this
	 * listener; -1 means unlimited.
	 */
	public void setMaxLevelsCrossed(Integer levelsCrossed) {
		maxLevelsCrossed = levelsCrossed;
	}

	/** Returns the current maximum-levels-crossed limit (-1 means unlimited). */
	public Integer getMaxLevelsCrossed() {
		return maxLevelsCrossed;
	}
}
