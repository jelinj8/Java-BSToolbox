package cz.bliksoft.javautils.binding.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import cz.bliksoft.javautils.binding.interfaces.IBeanStateProvider;
import cz.bliksoft.javautils.binding.interfaces.IObservable;
import cz.bliksoft.javautils.binding.interfaces.IVetoObservable;

public abstract class BasicBean implements IBeanStateProvider, IObservable, IVetoObservable {

	public static final String PROP_BEAN_STATE = IBeanStateProvider.PROP_BEAN_STATE;
	public static final String PROP_BEAN_MODIFIED = "beanModified";

	protected BeanState beanState = BeanState.INITIAL;

	private static final Set<String> defaultIgnoredProperties = new HashSet<>(
			Arrays.asList(PROP_BEAN_STATE, PROP_BEAN_MODIFIED));

	private Set<String> beanStateMonitoredProperties = null;
	private Set<String> beanStateIgnoredProperties = null;
	private StateChangeHandler beanStateMonitoringHandler = null;

	@Override
	public BeanState getBeanState() {
		return beanState;
	}

	@Override
	public void setBeanState(BeanState newState) {
		BeanState oldState = beanState;
		boolean oldModified = beanState.isModified();
		this.beanState = newState;
		getPropertyChangeSupport().firePropertyChange(PROP_BEAN_STATE, oldState, newState);
		if (oldModified != beanState.isModified())
			getPropertyChangeSupport().firePropertyChange(PROP_BEAN_MODIFIED, oldModified, beanState.isModified());
	}

	public void monitorBeanState() {
		if (beanStateMonitoringHandler == null) {
			beanStateMonitoringHandler = new StateChangeHandler();
			addPropertyChangeListener(beanStateMonitoringHandler);
		}
	}

	public void monitorPropertyBeanState(String propertyName) {
		monitorBeanState();
		if (beanStateMonitoredProperties == null) {
			beanStateMonitoredProperties = new HashSet<>();
		}
		beanStateMonitoredProperties.add(propertyName);
	}

	public void monitorPropertyBeanState(String... propertyNames) {
		monitorBeanState();
		if (beanStateMonitoredProperties == null) {
			beanStateMonitoredProperties = new HashSet<>();
		}
		beanStateMonitoredProperties.addAll(Arrays.asList(propertyNames));
	}

	public void ignorePropertyBeanState(String propertyName) {
		monitorBeanState();
		if (beanStateIgnoredProperties == null) {
			beanStateIgnoredProperties = new HashSet<>(defaultIgnoredProperties);
		}
		beanStateIgnoredProperties.add(propertyName);
	}

	public void ignorePropertyBeanState(String... propertyNames) {
		monitorBeanState();
		if (beanStateIgnoredProperties == null) {
			beanStateIgnoredProperties = new HashSet<>(defaultIgnoredProperties);
		}
		beanStateIgnoredProperties.addAll(Arrays.asList(propertyNames));
	}

	public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		getPropertyChangeSupport().firePropertyChange(propertyName, oldValue, newValue);
	}

	public void firePropertyChange(String propertyName, Object oldValue, Object newValue, boolean checkIdentity) {
		if (checkIdentity && oldValue == newValue)
			return;
		getPropertyChangeSupport().firePropertyChange(propertyName, oldValue, newValue);
	}

	public void firePropertyChange(String propertyName, int oldValue, int newValue) {
		getPropertyChangeSupport().firePropertyChange(propertyName, oldValue, newValue);
	}

	public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
		getPropertyChangeSupport().firePropertyChange(propertyName, oldValue, newValue);
	}

	private class StateChangeHandler implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (
			!beanStateIgnoredProperties.contains(evt.getPropertyName())
					&&(beanStateMonitoredProperties == null || beanStateMonitoredProperties.contains(evt.getPropertyName())))
				modifyBean();
		}
	}
}
