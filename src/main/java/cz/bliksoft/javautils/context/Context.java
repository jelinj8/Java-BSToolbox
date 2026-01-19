package cz.bliksoft.javautils.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.collections.WeakIdentityHashMap;
import cz.bliksoft.javautils.context.events.EventListener;
import cz.bliksoft.javautils.context.holders.SingleContextHolder;

/**
 * základní třída pro aplikační kontext
 * 
 */
public class Context {
	private static final Logger log = LogManager.getLogger();

//	public static class Constants {
//		private Constants() {
//		}
//
//		public static final String CONTEXT_IDENTITY_KEY = "ContextIdentityKey";
//		public static final String CONTEXT_APP_LOCK = "ContextAppLock";
//	}

	private static WeakIdentityHashMap<IContextProvider, Context> contextProviderContexts = new WeakIdentityHashMap<>();

	public static Context getContextProviderContext(IContextProvider key) {
		return contextProviderContexts.computeIfAbsent(key,
				k -> new EmptyContext("Default context for " + getAbbrevDescription(key)));
	}

	/**
	 * zařazené kontexty
	 */
	protected ArrayList<Context> childContexts = new ArrayList<>();
	/**
	 * rodičovské kontexty
	 */
	protected ArrayList<Context> parentContexts = new ArrayList<>();
	/**
	 * listenery tohoto kontextu
	 */
	protected ArrayList<AbstractContextListener<?>> contextListeners = new ArrayList<>();
	/**
	 * komentář ke kontextu - obvykle specifikace účelu
	 */
	protected String comment;

	/**
	 * konstruktor
	 * 
	 * @param comment komentář pro debugging
	 */
	public Context(String comment) {
		this.comment = comment;
	}

	@Override
	public String toString() {
		if (StringUtils.hasText(this.comment))
			return (isLevelContext ? "L" : "") + "CTX: " + this.comment;
		else
			return (isLevelContext ? "L" : "") + "CTX";
	}

	/**
	 * copy of child contexts list (changes won't be reflected)
	 * 
	 * @return
	 */
	public List<Context> getChildContexts() {
		return new ArrayList<>(childContexts);
	}

	/**
	 * zaregistruje jiný kontext jako svůj podřazený
	 * 
	 * @param context
	 */
	public void addContext(Context context) {
		if (context == null)
			return;
		if (this.childContexts.contains(context))
			return;
		this.childContexts.add(context);
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		log.trace("{}:{}: Added ''{}'' to ''{}''", stackTraceElements[2].getClassName(),
				stackTraceElements[2].getLineNumber(), context, this);
		context.parentContexts.add(this);
		context.notifyContextAllAdded(this);
	}

	/**
	 * odregistruje podřazený kontext
	 * 
	 * @param context
	 */
	public void removeContext(Context context) {
		if (context == null)
			return;
		if (this.childContexts.remove(context)) {
			StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
			log.trace("{}:{}: Removing ''{}'' from ''{}''", stackTraceElements[2].getClassName(),
					stackTraceElements[2].getLineNumber(), context, this);
			context.parentContexts.remove(this);
			context.notifyContextAllRemoved(this);
		} else {
			StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
			log.trace("{}:{}: Not removing ''{}'' from ''{}'' (not there)", stackTraceElements[2].getClassName(),
					stackTraceElements[2].getLineNumber(), context, this);
		}
	}

	/**
	 * odebere všechny vložené kontexty
	 */
	public void removeAllContexts() {
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		log.log(Level.TRACE, "{}:{}: Removing all contexts from ''{}''", stackTraceElements[2].getClassName(),
				stackTraceElements[2].getLineNumber(), this);
		ArrayList<Context> ctr = new ArrayList<>(this.childContexts);
		for (Context ctx : ctr) {
			this.removeContext(ctx);
		}
	}

	/**
	 * upozorní všechny kontexty v hierarchii nahoru na odebrání všech svých položek
	 * 
	 * Pozor na práci s kolekcemi - notifikace může způsobit změny
	 * 
	 * @param lowestLevel úroveň, od které má dojít k upozorňování
	 */
	protected void notifyContextAllRemoved(Context lowestLevel) {
		for (Context cont : this.childContexts) {
			cont.notifyContextAllRemoved(lowestLevel);
		}

		ArrayList<Object> keys = new ArrayList<>(this.mapValues.keySet());
		for (Object key : keys) {
			lowestLevel.notifyListeners(ContextSearchResult.getInvalid(this, key));
		}

		ArrayList<Object> vals = new ArrayList<>(this.listValues);
		for (Object value : vals) {
			lowestLevel.notifyListeners(ContextSearchResult.getInvalid(this, value.getClass()));
		}
	}

	/**
	 * upozorní všechny kontexty v hierarchii nahoru na přidání všech svých položek
	 * 
	 * Pozor na práci s kolekcemi - notifikace může způsobit změny
	 * 
	 * @param lowestLevel úroveň, od které má dojít k upozorňování
	 */
	protected void notifyContextAllAdded(Context lowestLevel) {
		for (Context cont : this.childContexts) {
			cont.notifyContextAllAdded(lowestLevel);
		}

		ArrayList<Object> keys = new ArrayList<>(this.mapValues.keySet());

		for (Object key : keys) {
			lowestLevel.notifyListeners(new ContextSearchResult(this, key, this.mapValues.get(key)));
		}

		ArrayList<Object> vals = new ArrayList<>(this.listValues);
		for (Object value : vals) {
			lowestLevel.notifyListeners(new ContextSearchResult(this, value.getClass(), value));
		}
	}

	/**
	 * upozorní na změnu hodnoty obsažené v kontextu. Pokud listener vrátí false, je
	 * šíření upozornění do vyšších úrovní zastaveno
	 * 
	 * @param value
	 */
	public void notifyListeners(ContextSearchResult value) {
		boolean propagate = true;
		if (value.getContext() == this && log.isTraceEnabled())
			log.debug(StringUtils.format("ContextChangeFiring in ctx ''{0}'', {1}:{2}", value.getContext(),
					getAbbrevDescription(value.key), getAbbrevDescription(value.result)));

		ArrayList<AbstractContextListener<?>> lstnrs = new ArrayList<>(this.contextListeners);
		for (AbstractContextListener<?> listener : lstnrs) {
			if (listener.isInterrested(value.getKey(), value.getLevelsCrossed())) {
				// log.debug("| interrested lstnr '{}'", listener);
				ContextSearchResult newResult = this.getValue(listener.getKey());
				newResult.setLevelsCrossed(value.getLevelsCrossed());
				try {
					if (!listener.fireContextChanged(newResult)) {
						propagate = false;
					}
				} catch (Exception e) {
					log.error("ContextChanged ERR:", e);
				}
			}
		}
		if (propagate) {
			for (Context parent : this.parentContexts) {
				ContextSearchResult newResult = parent.getValue(value.getKey());
				newResult.setLevelsCrossed(value.getLevelsCrossed() + (isLevelContext ? 1 : 0));
				if (log.isTraceEnabled())
					log.trace("Context change propagated from [{}] to [{}] [{}]", this, parent, newResult);
				parent.notifyListeners(newResult);
			}
		}
	}

	/**
	 * vrací aktuální výsledek vyhledávání v kontextu a v podřízených kontextech
	 * podle zadaného klíče. Pokud je klíčem Class, jsou výsledkem i objekty,
	 * jejichž klíč je od daného typu odvozený, jinak podle .equals
	 * 
	 * @param key vyhledávací klíč
	 * @return
	 */
	public ContextSearchResult getValue(Object key) {
		if (key instanceof Class) {
			for (Entry<Object, Object> entry : this.mapValues.entrySet()) {
				if (entry.getKey() instanceof Class && ((Class<?>) key).isAssignableFrom((Class<?>) entry.getKey()))
					return new ContextSearchResult(this, key, entry.getValue());
				// return new ContextSearchResult(this, key, entry.getValue());
			}
		} else if (this.mapValues.containsKey(key)) {
			return new ContextSearchResult(this, key, this.mapValues.get(key));
		}

		for (Object value : this.listValues) {
			if (key instanceof Class && ((Class<?>) key).isAssignableFrom(value.getClass()))
				return new ContextSearchResult(this, key, value);
		}

		ContextSearchResult res = null;
		for (Context chContext : this.childContexts) {
			res = chContext.getValue(key);
			if ((res != null) && (res.isValid())) {

				// aktualizace kontextu, ze kterého hodnota pochází
				if (res.ctx == null)
					res.ctx = chContext;
				else
					res.level++;

				return res;
			}
		}
		return ContextSearchResult.getInvalid(this, key);
	}

	/**
	 * přidá hlídáček na kontext
	 * 
	 * @param listener
	 * @param initialize pro true spustí hlídáček s aktuální hodnotou
	 */
	public void addContextListener(AbstractContextListener<?> listener, boolean initialize) {
		if (!this.contextListeners.contains(listener)) {
			this.contextListeners.add(listener);
			if (initialize) {
				listener.fireContextChanged(this.getValue(listener.getKey()));
			}
		}
	}

	public List<AbstractContextListener<?>> getContextListeners() {
		return contextListeners;
	}

	/**
	 * přidá hlídáček na kontext
	 * 
	 * @param listener
	 */
	public void addContextListener(AbstractContextListener<?> listener) {
		addContextListener(listener, false);
	}

	/**
	 * odebere z kontextu hlídáček
	 * 
	 * @param listener
	 */
	public void removeContextListener(AbstractContextListener<?> listener) {
		this.contextListeners.remove(listener);
	}

	// </editor-fold>

	protected boolean isLevelContext = false;

	/**
	 * jedná se o základní kontext levelu?
	 * 
	 * @return
	 */
	public boolean isLevelContext() {
		return isLevelContext;
	}

	/**
	 * označí kontext jako základní kontext levelu
	 */
	public void setLevelContext() {
		isLevelContext = true;
	}

	private static Context globalContext = null;

	public static Context getGlobal() {
		if (globalContext == null)
			globalContext = new Context("Global context root");
		return globalContext;
	}

	public static boolean isContextInitialized() {
		return globalContext != null;
	}

	private static SingleContextHolder switchableContext = null;

	public static SingleContextHolder getSwitchedContext() {
		if (switchableContext == null)
			switchableContext = new SingleContextHolder("Switchable (current) context");
		return switchableContext;
	}

	public static void setCurrentContext(Context ctx) {
		getSwitchedContext().replaceContext(ctx);
	}

	ArrayList<Object> listValues = new ArrayList<>();

	/**
	 * copy of listValues (modifications won't be reflected in context)
	 * 
	 * @return
	 */
	public List<Object> getListValues() {
		return new ArrayList<>(listValues);
	}

	private HashMap<Object, Object> mapValues = new HashMap<>();

	/**
	 * copy of mapValues (modifications won't be reflected in context)
	 * 
	 * @return
	 */
	public Map<Object, Object> getMapValues() {
		return new HashMap<>(mapValues);
	}

	public void addValue(Object value) {
		if (!this.listValues.contains(value)) {
			this.listValues.add(value);
			this.notifyListeners(new ContextSearchResult(this, value.getClass(), value));
		}
	}

	public void removeValue(Object value) {
		if (this.listValues.contains(value)) {
			this.listValues.remove(value);
			this.notifyListeners(ContextSearchResult.getInvalid(this, value.getClass()));
		}
	}

	public void put(Object key, Object value) {
		if (value == null) {
			this.remove(key);
			return;
		}
		if (key instanceof Class) {
			if (((Class<?>) key).isAssignableFrom(value.getClass())) {
				this.mapValues.put(key, value);
				this.notifyListeners(new ContextSearchResult(this, key, value));
			} else {
				log.error("Unassignable Class key! Key value nulled."); //$NON-NLS-1$
				this.mapValues.remove(key);
				this.notifyListeners(ContextSearchResult.getInvalid(this, key));
			}
		} else {
			this.mapValues.put(key, value);
			this.notifyListeners(new ContextSearchResult(this, key, value));
		}
	}

	public void remove(Object key) {
		if (key instanceof Class) {
			Object keyToRemove = null;
			for (Object k : this.mapValues.keySet()) {
				if (k instanceof Class && ((Class<?>) key).isAssignableFrom((Class<?>) k)) {
					keyToRemove = k;
					break;
				}
			}
			if (keyToRemove != null) {
				this.mapValues.remove(keyToRemove);
			}
		} else if (this.mapValues.containsKey(key)) {
			this.mapValues.remove(key);
		}

		this.notifyListeners(ContextSearchResult.getInvalid(this, key));
	}

	private LinkedList<EventListener<?>> eventListeners = new LinkedList<>();

	public List<EventListener<?>> getEventListeners() {
		return eventListeners;
	}

	/**
	 * fire event that can be processed on current thread
	 * 
	 * @param event
	 */
	public void fireEvent(Object event) {
		fireEvent(event, false);
	}

	/**
	 * fire event that should be processed on EDT, runtime exception if EDT is bound
	 * and called from other thread
	 * 
	 * @param event
	 */
	public void fireGUIEvent(Object event) {
		fireEvent(event, true);
	}

	public void fireEvent(Object event, boolean enforceEDT) {
		if (enforceEDT && !EventListener.isEdt()) {
			if (event != null)
				throw new RuntimeException("Events should be fired on EDT! " + event.getClass().getName());
			else
				throw new RuntimeException("Events should be fired on EDT!");
		}

		boolean consumed = false;
		List<EventListener<?>> currentListeners = new ArrayList<>(eventListeners);
		for (EventListener<?> lstnr : currentListeners) {
			consumed |= lstnr.fire(event);
		}

		if (consumed || event instanceof ILevelEvent)
			return;

		for (Context parent : this.parentContexts) {
			parent.fireEvent(event, enforceEDT);
		}
	}

	public void addEventListener(EventListener<?> listener) {
		eventListeners.addFirst(listener);
	}

	public boolean removeEventListener(EventListener<?> listener) {
		return eventListeners.remove(listener);
	}

	public static String getAbbrevDescription(Object o) {
		if (o == null) {
			return "<NULL>";
		} else if (o instanceof ContextSearchResult) {
			if (((ContextSearchResult) o).isValid()) {
				return getAbbrevDescription(((ContextSearchResult) o).getResult());
			} else {
				return "<INVALID>";
			}
		} else if (o instanceof Class) {
			return StringUtils.format("<{0}>", o.getClass().getName());
		} else if (o instanceof String) {
			return "'" + o + "'";
		} else {
			String res = (StringUtils.hasText(o.toString()) ? o.toString() : o.getClass().toString());
			return StringUtils.ellipsis(res, 100);
		}
	}
}
