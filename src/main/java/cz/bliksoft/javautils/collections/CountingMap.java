package cz.bliksoft.javautils.collections;

import java.util.HashMap;
import java.util.Map;

/**
 * Map of incrementable values, thread safe (synchronized)
 */
public class CountingMap {
	Map<String, Long> values = new HashMap<>();
	private Object lock = new Object();

	private final Long startingValue;

	public CountingMap(long startValue) {
		this.startingValue = startValue;
	}

	public CountingMap() {
		this(0l);
	}

	/**
	 * get keyed value, starting value if not present
	 * 
	 * @param key
	 * @return
	 */
	public Long getValue(String key) {
		synchronized (lock) {
			return values.get(key);
		}
	}

	/**
	 * get keyed value or specified default
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public Long getOrDefault(String key, Long defaultValue) {
		synchronized (lock) {
			return values.getOrDefault(key, defaultValue);
		}
	}

	/**
	 * increment keyed value, initial value as specified in constructor or 0
	 * 
	 * @param key
	 * @return
	 */
	public long inc(String key) {
		synchronized (lock) {
			Long v = values.getOrDefault(key, startingValue);
			values.put(key, ++v);
			return v;
		}
	}

	/**
	 * decrement keyed value, initial value as specified in constructor or 0
	 * 
	 * @param key
	 * @return
	 */
	public long dec(String key) {
		synchronized (lock) {
			Long v = values.getOrDefault(key, startingValue);
			values.put(key, --v);
			return v;
		}
	}

	/**
	 * remove all values
	 */
	public void clear() {
		synchronized (lock) {
			values.clear();
		}
	}

	/**
	 * count used keys
	 * 
	 * @return
	 */
	public long count() {
		synchronized (lock) {
			return values.size();
		}
	}

	/**
	 * get a copy of values map
	 * 
	 * @return
	 */
	public Map<String, Long> getValues() {
		synchronized (lock) {
			return new HashMap<>(values);
		}
	}
}
