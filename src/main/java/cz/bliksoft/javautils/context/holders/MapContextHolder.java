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

public class MapContextHolder<T, C extends Context> extends SingleContextHolder {
	private static final Logger log = LogManager.getLogger();

	private final Map<T, C> map = new LinkedHashMap<>();

	public MapContextHolder(String comment) {
		super(comment);
	}

	public void put(T key, C context) {
		map.put(key, context);
	}

	public C get(T key) {
		return map.get(key);
	}

	public C removeKey(T key) {
		C removed = map.remove(key);
		if (removed != null && removed == getContext())
			replaceContext(null);
		return removed;
	}

	public boolean containsKey(T key) {
		return map.containsKey(key);
	}

	public Set<T> keySet() {
		return Collections.unmodifiableSet(map.keySet());
	}

	public Collection<C> values() {
		return Collections.unmodifiableCollection(map.values());
	}

	public Map<T, C> asMap() {
		return Collections.unmodifiableMap(map);
	}

	public void select(T key) {
		C context = map.get(key);
		if (context == null) {
			log.warn("No context registered for key: {}", key);
			return;
		}
		replaceContext(context);
	}

	public void deselect() {
		replaceContext(null);
	}

	@SuppressWarnings("unchecked")
	public C getSelected() {
		return (C) getContext();
	}

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
