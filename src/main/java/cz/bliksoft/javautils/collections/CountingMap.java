package cz.bliksoft.javautils.collections;

import java.util.HashMap;
import java.util.Map;

/**
 * Map of incrementable values, thread safe (synchronized)
 */
public class CountingMap extends HashMap<String, Long> {
	/**
	 *
	 */
	private static final long serialVersionUID = -1238326355476731803L;

	private final Long startingValue;

	public CountingMap(long startValue) {
		this.startingValue = startValue;
	}

	public CountingMap() {
		this(0l);
	}

	/**
	 * get keyed value or specified default
	 *
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public Long getOrDefault(String key, Long defaultValue) {
		return super.getOrDefault(key, defaultValue);
	}

	/**
	 * increment keyed value, initial value as specified in constructor or 0
	 *
	 * @param key
	 * @return
	 */
	public long inc(String key) {
		Long v = super.getOrDefault(key, startingValue);
		put(key, ++v);
		return v;
	}

	/**
	 * increment a value by step
	 *
	 * @param key
	 * @param step
	 * @return
	 */
	public long inc(String key, long step) {
		Long v = super.getOrDefault(key, startingValue);
		v += step;
		put(key, v);
		return v;
	}

	/**
	 * decrement keyed value, initial value as specified in constructor or 0
	 *
	 * @param key
	 * @return
	 */
	public long dec(String key) {
		Long v = super.getOrDefault(key, startingValue);
		put(key, --v);
		return v;
	}

	/**
	 * decrement a value by step
	 *
	 * @param key
	 * @param step
	 * @return
	 */
	public long dec(String key, long step) {
		Long v = super.getOrDefault(key, startingValue);
		v -= step;
		put(key, v);
		return v;
	}

	/**
	 * get a copy of values map
	 *
	 * @return
	 */
	public Map<String, Long> getValues() {
		return new HashMap<>(this);
	}
}
