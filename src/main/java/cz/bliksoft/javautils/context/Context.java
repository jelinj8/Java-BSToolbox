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

/** Base class for the application context tree. */
public class Context {
	private static final Logger log = LogManager.getLogger();

	private static WeakIdentityHashMap<IContextProvider, Context> contextProviderContexts = new WeakIdentityHashMap<>();

	/** Returns (or auto-creates) the context associated with the given provider. */
	public static Context getContextProviderContext(IContextProvider key) {
		return contextProviderContexts.computeIfAbsent(key,
				k -> new Context("Default context for " + getAbbrevDescription(key)));
	}

	/**
	 * Wrap a provided context in a level context with comment
	 *
	 * @param content
	 * @param comment
	 * @return
	 */
	public static Context wrapAsLevelContext(Context content, String comment) {
		Context ctx = new Context(comment);
		ctx.setLevelContext();
		if (content != null)
			ctx.addContext(content);
		return ctx;
	}

	/** Child contexts registered under this node. */
	protected ArrayList<Context> childContexts = new ArrayList<>();
	/** Parent contexts this node is registered under. */
	protected ArrayList<Context> parentContexts = new ArrayList<>();
	/** Listeners observing value changes in this context. */
	protected ArrayList<AbstractContextListener<?>> contextListeners = new ArrayList<>();
	/** Human-readable label used for debugging. */
	protected String comment;

	/** Creates a context node with the given debug label. */
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
	 * Registers a context as a child of this one; no-op if already registered or
	 * null.
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

	/** Removes a previously registered child context. */
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

	/** Removes all registered child contexts. */
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
	 * Notifies listeners up the hierarchy that all values in this subtree have been
	 * removed.
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
	 * Notifies listeners up the hierarchy that all values in this subtree have been
	 * added.
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
	 * Fires a change notification for the given result, propagating up the parent
	 * chain unless blocked.
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
	 * Searches this node and its children for the given key; Class keys match by
	 * assignability.
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
	 * Registers a context listener; if {@code initialize} is true, fires it
	 * immediately with the current value.
	 */
	public void addContextListener(AbstractContextListener<?> listener, boolean initialize) {
		if (!this.contextListeners.contains(listener)) {
			this.contextListeners.add(listener);
			if (initialize) {
				listener.fireContextChanged(this.getValue(listener.getKey()));
			}
		}
	}

	/** Returns the context listeners attached to this node. */
	public List<AbstractContextListener<?>> getContextListeners() {
		return contextListeners;
	}

	/** Registers a context listener without an initial notification. */
	public void addContextListener(AbstractContextListener<?> listener) {
		addContextListener(listener, false);
	}

	/** Removes a previously registered context listener. */
	public void removeContextListener(AbstractContextListener<?> listener) {
		this.contextListeners.remove(listener);
	}

	// </editor-fold>

	protected boolean isLevelContext = false;

	/**
	 * Returns whether this context acts as a level boundary for event propagation.
	 */
	public boolean isLevelContext() {
		return isLevelContext;
	}

	/**
	 * Marks this context as a level boundary, stopping {@link ILevelEvent}
	 * propagation.
	 */
	public void setLevelContext() {
		isLevelContext = true;
	}

	private static Context rootContext = null;

	/** Returns the global root context, creating it on first access. */
	public static Context getRoot() {
		if (rootContext == null)
			rootContext = new Context("Global context root");
		return rootContext;
	}

	/** Returns true if the global root context has been created. */
	public static boolean isContextInitialized() {
		return rootContext != null;
	}

	private static SingleContextHolder currentContext = null;

	/**
	 * Returns the global switchable current-context holder, creating it on first
	 * access.
	 */
	public static SingleContextHolder getCurrentContext() {
		if (currentContext == null) {
			currentContext = new SingleContextHolder("Switchable (current) context");
			getRoot().addContext(currentContext);
		}
		return currentContext;
	}

	/** Replaces the active context in the global current-context holder. */
	public static void setCurrentContext(Context ctx) {
		getCurrentContext().replaceContext(ctx);
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

	/** Adds a value to the type-indexed list; notifies listeners. */
	public void addValue(Object value) {
		if (!this.listValues.contains(value)) {
			this.listValues.add(value);
			this.notifyListeners(new ContextSearchResult(this, value.getClass(), value));
		}
	}

	/** Removes a value from the type-indexed list; notifies listeners. */
	public void removeValue(Object value) {
		if (this.listValues.contains(value)) {
			this.listValues.remove(value);
			this.notifyListeners(ContextSearchResult.getInvalid(this, value.getClass()));
		}
	}

	/**
	 * Stores a key-value pair and notifies listeners; passing {@code null} as value
	 * removes the key.
	 */
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

	/** Removes the value for the given key and notifies listeners. */
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

	/** Returns the event listeners on this node. */
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

	/**
	 * Fires an event, optionally enforcing EDT; propagates up unless consumed or
	 * blocked at a level boundary.
	 */
	public void fireEvent(Object event, boolean enforceEDT) {
		if (enforceEDT && !EventListener.isEdt()) {
			if (event != null)
				throw new RuntimeException("Events should be fired on EDT! " + event.getClass().getName());
			else
				throw new RuntimeException("Events should be fired on EDT!");
		}

		List<EventListener<?>> currentListeners = new ArrayList<>(eventListeners);
		for (EventListener<?> lstnr : currentListeners) {
			if (lstnr.fire(event))
				return; // event consumed
		}

		// level boundary
		if (isLevelContext && event instanceof ILevelEvent)
			return;

		for (Context parent : this.parentContexts) {
			parent.fireEvent(event, enforceEDT);
		}
	}

	/** Prepends an event listener so it runs before previously added listeners. */
	public void addEventListener(EventListener<?> listener) {
		eventListeners.addFirst(listener);
	}

	/** Removes an event listener; returns true if it was present. */
	public boolean removeEventListener(EventListener<?> listener) {
		return eventListeners.remove(listener);
	}

	/**
	 * Returns a short human-readable description of an object, suitable for log
	 * messages.
	 */
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

	/** Returns a full recursive text dump of this context subtree for debugging. */
	public String dump() {
		StringBuilder sb = new StringBuilder();
		dump(sb, "");
		return sb.toString();
	}

	private void dump(StringBuilder sb, String prefix) {
		String currentPrefix = prefix;
		sb.append(currentPrefix);
		sb.append(toString());
		if (this == Context.getCurrentContext())
			sb.append(" [CURRENT]");
		sb.append("\n");
		currentPrefix += "\t";
		for (EventListener<?> l : eventListeners) {
			sb.append(currentPrefix);
			sb.append(l.toString());
			sb.append("\n");
		}
		for (AbstractContextListener<?> l : contextListeners) {
			sb.append(currentPrefix);
			sb.append(l.toString());
			sb.append("\n");
		}
		dumpValues(sb, currentPrefix);
		for (Context c : childContexts) {
			c.dump(sb, currentPrefix);
		}
	}

	/**
	 * Appends this node's own values to the dump output; override to add custom
	 * entries.
	 */
	protected void dumpValues(StringBuilder sb, String prefix) {
		if (listValues != null) {
			listValues.forEach(o -> {
				sb.append(String.format("%s+V: %s\n", prefix, o));
			});
		}
		if (mapValues != null) {
			mapValues.forEach((k, v) -> {
				sb.append(String.format("%s+V: [%s]:%s\n", prefix, k, v));
			});
		}
	}
}
