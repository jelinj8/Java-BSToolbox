package cz.bliksoft.javautils.collections;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.bliksoft.javautils.TimestampedObject;

public class TimestampedHashMap<K, V> implements Map<K, V> {

	private long validity = 0;

	private Map<K, TimestampedObject<V>> values = new HashMap<>();

	public TimestampedHashMap() {
	}

	/**
	 * validity in milliseconds. Negative means get-once and remove, zero is unlimited.
	 * @param validity
	 */
	public TimestampedHashMap(long validity) {
		this.validity = validity;
	}

	@Override
	public void clear() {
		synchronized (values) {
			values.clear();
		}
	}

	@Override
	public boolean containsKey(Object key) {
		synchronized (values) {
			return values.containsKey(key);
		}
	}

	@Override
	public boolean containsValue(Object value) {
		synchronized (values) {
			return values.containsValue(value);
		}
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		synchronized (values) {
			Set<java.util.Map.Entry<K, V>> entries = new HashSet<>();
			for (java.util.Map.Entry<K, TimestampedObject<V>> kvp : values.entrySet()) {
				final java.util.Map.Entry<K, TimestampedObject<V>> e = kvp;
				entries.add(new Entry<K, V>() {
					private java.util.Map.Entry<K, TimestampedObject<V>> kvp = e;

					@Override
					public K getKey() {
						return kvp.getKey();
					}

					@Override
					public V getValue() {
						if (kvp.getValue() == null)
							return null;
						return kvp.getValue().getValue();
					}

					@Override
					public V setValue(V value) {
						kvp.setValue(new TimestampedObject<V>(value));
						return value;
					}
				});
			}
			return entries;
		}
	}

	@Override
	public V get(Object key) {
		synchronized (values) {
			TimestampedObject<V> val = values.get(key);
			if (validity < 0)
				values.remove(key);

			if (val != null) {
				return val.getValue();
			}
			return null;
		}
	}

	/**
	 * like get, but updating object timestamp
	 * @param key
	 * @return
	 */
	public V touch(Object key) {
		synchronized (values) {
			TimestampedObject<V> val = values.get(key);
			if (validity < 0) {
				values.remove(key);
			}
			
			if (val != null) {
				val.touch();
				return val.getValue();
			}
			return null;
		}
	}

	@Override
	public boolean isEmpty() {
		synchronized (values) {
			return values.isEmpty();
		}
	}

	@Override
	public Set<K> keySet() {
		synchronized (values) {
			return values.keySet();
		}
	}

	@Override
	public V put(K key, V value) {
		synchronized (values) {

			TimestampedObject<V> oldVal = values.put(key, new TimestampedObject<V>(value));
			if (oldVal == null)
				return null;
			return oldVal.getValue();
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		synchronized (values) {
			for (java.util.Map.Entry<? extends K, ? extends V> kvp : m.entrySet()) {
				values.put(kvp.getKey(), new TimestampedObject<V>(kvp.getValue()));
			}
		}
	}

	@Override
	public V remove(Object key) {
		synchronized (values) {
			TimestampedObject<V> oldVal = values.remove(key);
			if (oldVal == null)
				return null;
			return oldVal.getValue();
		}
	}

	@Override
	public int size() {
		synchronized (values) {
			return values.size();
		}
	}

	@Override
	public Collection<V> values() {
		synchronized (values) {

			List<V> res = new LinkedList<>();
			for (java.util.Map.Entry<K, TimestampedObject<V>> kvp : values.entrySet()) {
				res.add(kvp.getValue().getValue());
			}
			return res;
		}
	}

	public void cleanup() {
		if (validity == 0)
			return;
		synchronized (values) {
			long treshold = (new Date()).getTime() - Math.abs(validity);
			Set<K> toRemove = new HashSet<>();
			for (java.util.Map.Entry<K, TimestampedObject<V>> kvp : values.entrySet()) {
				if (kvp.getValue().getTimestamp() < treshold)
					toRemove.add(kvp.getKey());
			}

			for (K keyToRemove : toRemove) {
				values.remove(keyToRemove);
			}
		}
	}
}
