package cz.bliksoft.javautils.binding.interfaces;

import java.beans.PropertyChangeListener;

public interface IObservable {

	void addPropertyChangeListener(PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);

	void addPropertyChangeListener(String property, PropertyChangeListener listener);

	void removePropertyChangeListener(String property, PropertyChangeListener listener);
}
