package cz.bliksoft.javautils.binding.list.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import cz.bliksoft.javautils.binding.interfaces.IObservable;

public class ObservableHashSet<E> extends HashSet<E> implements IObservable {

	private static final long serialVersionUID = -2949992258573171695L;

	@Override
	public boolean add(E e) {
		if (super.add(e)) {
			fireDataEvent(new MapDataEvent<>(this, e, EventType.ADDED));
			return true;
		} else
			return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object o) {
		if (super.remove(o)) {
			fireDataEvent(new MapDataEvent<>(this, (E) o, EventType.REMOVED));
			return true;
		} else
			return false;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		if (super.addAll(c)) {
			fireDataEvent(new MapDataEvent<>(this, c, EventType.ADDED));
			return true;
		} else
			return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		Set<E> removed = new HashSet<>();
		removed.addAll(this);
		removed.removeAll(c);
		if (!removed.isEmpty()) {
			return removeAll(removed);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean removeAll(Collection<?> c) {
		Set<E> removed = new HashSet<>();
		for (Object i : c) {
			if (super.remove(i))
				removed.add((E) i);
		}
		if (!removed.isEmpty()) {
			fireDataEvent(new MapDataEvent<>(this, removed, EventType.REMOVED));
			return true;
		}
		return false;
	}

	@Override
	public void clear() {
		if (!isEmpty()) {
			MapDataEvent<E> e = new MapDataEvent<>(this, this, EventType.REMOVED);
			super.clear();
			fireDataEvent(e);
		}
	}

	private List<Consumer<MapDataEvent<E>>> setEventListeners = null;

	public List<Consumer<MapDataEvent<E>>> getSetEventListeners() {
		if (setEventListeners == null)
			setEventListeners = new ArrayList<>();
		return setEventListeners;
	}

	public void addSetDataListener(Consumer<MapDataEvent<E>> listener) {
		getSetEventListeners().add(listener);
	}

	public void removeSetDataListener(Consumer<MapDataEvent<E>> listener) {
		if (setEventListeners != null)
			setEventListeners.remove(listener);
	}

	private void fireDataEvent(MapDataEvent<E> event) {
		if (setEventListeners != null)
			for (Consumer<MapDataEvent<E>> listener : setEventListeners) {
				listener.accept(event);
			}
	}

	public enum EventType {
		ADDED, REMOVED
	}

	@SuppressWarnings("serial")
	public class MapDataEvent<V> extends EventObject {

		private final EventType type;
		private final Set<V> values;

		public MapDataEvent(Set<V> source, Collection<? extends V> values, EventType type) {
			super(source);
			this.type = type;
			this.values = new HashSet<>();
			this.values.addAll(values);
		}

		public MapDataEvent(Set<V> source, V value, EventType type) {
			super(source);
			this.type = type;
			this.values = new HashSet<>();
			this.values.add(value);
		}

		public EventType getType() {
			return type;
		}

		public Set<V> getValues() {
			return values;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o instanceof ObservableHashSet) {
			if (size() != ((ObservableHashSet<?>) o).size())
				return false;
			for (E i : this)
				if (((ObservableHashSet<?>) o).contains(i))
					return false;
			return true;
		} else
			return false;
	}

}
