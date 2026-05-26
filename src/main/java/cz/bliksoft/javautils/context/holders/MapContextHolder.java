package cz.bliksoft.javautils.context.holders;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.context.Context;

/**
 * Context holder that stores named contexts in a map; {@link #select} makes one
 * the active child.
 */
public class MapContextHolder<T, C extends Context> extends SingleContextHolder {
	private static final Logger log = LogManager.getLogger();

	private final Map<T, C> map = new LinkedHashMap<>();

	/** Creates a holder with the given debug label. */
	public MapContextHolder(String comment) {
		super(comment);
	}

	/** Registers a context under the given key (does not auto-select it). */
	public void put(T key, C context) {
		map.put(key, context);
	}

	/** Returns the context registered under the given key, or null if absent. */
	public C get(T key) {
		return map.get(key);
	}

	/** Removes the context for the given key; deselects it if it was active. */
	public C removeKey(T key) {
		C removed = map.remove(key);
		if (removed != null && removed == getContext())
			replaceContext(null);
		return removed;
	}

	/** Returns true if a context is registered under the given key. */
	public boolean containsKey(T key) {
		return map.containsKey(key);
	}

	/** Returns an unmodifiable view of the registered keys. */
	public Set<T> keySet() {
		return Collections.unmodifiableSet(map.keySet());
	}

	/** Returns an unmodifiable view of the registered contexts. */
	public Collection<C> values() {
		return Collections.unmodifiableCollection(map.values());
	}

	/** Returns an unmodifiable view of the entire key-to-context map. */
	public Map<T, C> asMap() {
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Makes the context for the given key the active child; logs a warning and
	 * no-ops if the key is absent.
	 */
	public void select(T key) {
		C context = map.get(key);
		if (context == null) {
			log.warn("No context registered for key: {}", key);
			return;
		}
		replaceContext(context);
	}

	/** Clears the active child without removing it from the map. */
	public void deselect() {
		replaceContext(null);
	}

	/**
	 * Returns the currently active context cast to {@code C}, or null if none is
	 * selected.
	 */
	@SuppressWarnings("unchecked")
	public C getSelected() {
		return (C) getContext();
	}

	/**
	 * Returns the key of the currently active context, or null if none is selected.
	 */
	@SuppressWarnings("unchecked")
	public T getSelectedKey() {
		C current = (C) getContext();
		if (current == null)
			return null;
		for (Map.Entry<T, C> entry : map.entrySet()) {
			if (entry.getValue() == current)
				return entry.getKey();
		}
		return null;
	}

	@Override
	protected void dumpValues(StringBuilder sb, String prefix) {
		super.dumpValues(sb, prefix);
		Context selected = getContext();
		for (Map.Entry<T, C> entry : map.entrySet())
			if (entry.getValue() != selected)
				sb.append(prefix).append("[").append(entry.getKey()).append("] ").append(entry.getValue()).append("\n");
	}

	@Override
	public String toString() {
		if (StringUtils.hasText(this.comment))
			return (isLevelContext ? "L" : "") + "MapCTXHolder: " + this.comment;
		else
			return (isLevelContext ? "L" : "") + "MapCTXHolder";
	}
}
