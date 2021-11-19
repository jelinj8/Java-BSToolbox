package cz.bliksoft.javautils.binding.interfaces;

import java.beans.PropertyChangeListener;

import cz.bliksoft.javautils.binding.exceptions.UnsupportedException;

public interface IValueModel<T> {
	String PROPERTY_VALUE = "value";

	T getValue();

	void setValue(T newValue);

	void addValueChangeListener(PropertyChangeListener l);

	void removeValueChangeListener(PropertyChangeListener l);

	default boolean isIdentityCheckEnabled() {
		return false;
	}

	default void setIdentityCheckEnabled(boolean checkIdentity) {
		throw new UnsupportedException("This ValueModel doesn't support identity checking.");
	}
	
	@SuppressWarnings("unchecked")
	default PropertyChangeListener getPropertyListener() {
		return evt -> setValue((T) evt.getNewValue());
	}
	
	/**
	 * force notify as if value changed
	 */
	void forceFireValueChanged();
}
