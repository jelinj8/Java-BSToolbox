package cz.bliksoft.javautils.binding.interfaces;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import cz.bliksoft.javautils.binding.BeanUtils;

public interface IObservable {

	default PropertyChangeSupport getPropertyChangeSupport() {
		return BeanUtils.getPropertyChangeSupport(this);
	}

	default void addPropertyChangeListener(PropertyChangeListener listener) {
		getPropertyChangeSupport().addPropertyChangeListener(listener);
	}

	default void removePropertyChangeListener(PropertyChangeListener listener) {
		getPropertyChangeSupport().removePropertyChangeListener(listener);
	}

	default void addPropertyChangeListener(String property, PropertyChangeListener listener) {
		getPropertyChangeSupport().addPropertyChangeListener(property, listener);
	}

	default void removePropertyChangeListener(String property, PropertyChangeListener listener) {
		getPropertyChangeSupport().removePropertyChangeListener(property, listener);
	}
}
