package cz.bliksoft.javautils.context;

import java.text.MessageFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.context.events.EventListener;

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

	/**
	 * klíč, který je v kontextu sledován
	 */
	Object key;

	/**
	 * @return vyhledávací klíč
	 */
	public Object getKey() {
		return this.key;
	}

	/**
	 * uložený předchozí výsledek
	 */
	ContextSearchResult oldValue = new ContextSearchResult(null, null);
	ContextSearchResult currentValue = new ContextSearchResult(null, null);

	private boolean wasModified = false;

	/**
	 * určuje, zda má být hlídáček spuštěn pro hodnotu pod předaným klíčem -> zda
	 * vlastnímu vyhledávacímu klíči vyhovuje předaná hodnota
	 * 
	 * @param resultKey hodnota klíče, která má být ověřena proti klíči listeneru
	 * @return
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

	/**
	 * spustí kontrolu hlídáčků
	 */
	protected final void fireUpdate() {
		if (!this.active) {
			this.setActive(true);
			this.setActive(false);
		}
	}

	/**
	 * upozorní na změnu ve sledované hodnotě
	 * 
	 * @param contextSearchResult
	 * @return
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

	/**
	 * @return zjistí stav zapnuto/vypnuto
	 */
	public boolean getActive() {
		return this.active;
	}

	/**
	 * aktivuje a deaktivuje hlídáček
	 * 
	 * @param newValue
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

	public void setMaxLevelsCrossed(Integer levelsCrossed) {
		maxLevelsCrossed = levelsCrossed;
	}

	public Integer getMaxLevelsCrossed() {
		return maxLevelsCrossed;
	}
}
