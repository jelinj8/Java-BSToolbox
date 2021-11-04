package cz.bliksoft.javautils.binding.interfaces;

import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;

import cz.bliksoft.javautils.binding.BeanUtils;

public interface IVetoObservable {

	default VetoableChangeSupport getVetoableChangeSupport() {
		return BeanUtils.getVetoableChangeSupport(this);
	}

	default void addPropertyChangeListener(VetoableChangeListener listener) {
		getVetoableChangeSupport().addVetoableChangeListener(listener);
	}

	default void removePropertyChangeListener(VetoableChangeListener listener) {
		getVetoableChangeSupport().addVetoableChangeListener(listener);
	}

	default void addPropertyChangeListener(String property, VetoableChangeListener listener) {
		getVetoableChangeSupport().addVetoableChangeListener(property, listener);
	}

	default void removePropertyChangeListener(String property, VetoableChangeListener listener) {
		getVetoableChangeSupport().addVetoableChangeListener(property, listener);
	}
}
