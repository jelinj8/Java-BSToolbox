package cz.bliksoft.javautils.binding.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.javautils.binding.interfaces.IBeanStateProvider;
import cz.bliksoft.javautils.binding.interfaces.IDefaultObservable;
import cz.bliksoft.javautils.binding.interfaces.IVetoObservable;
import cz.bliksoft.javautils.logging.LogUtils;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class BasicBean implements IBeanStateProvider, IDefaultObservable, IVetoObservable {

	private static Logger log = Logger.getLogger(BasicBean.class.getName());

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
		if (oldState != newState && log.isLoggable(Level.FINER))
			log.finer(MessageFormat.format("Bean '{'{4}'}' \"{0}\" state changed from [{1}] to [{2}]\n{3}", this,
					oldState, newState, LogUtils.traceToString(LogUtils.getStackTrace(0, 1)),
					this.getClass().getSimpleName()));
		boolean oldModified = beanState.isModified();
		beanState = newState;
		getPropertyChangeSupport().firePropertyChange(PROP_BEAN_STATE, oldState, newState);
		if (oldModified != beanState.isModified())
			getPropertyChangeSupport().firePropertyChange(PROP_BEAN_MODIFIED, oldModified, beanState.isModified());
	}

	public void monitorBeanState() {
		if (beanStateMonitoringHandler == null) {
			if (beanStateIgnoredProperties == null) {
				beanStateIgnoredProperties = new HashSet<>(defaultIgnoredProperties);
			}
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
		beanStateIgnoredProperties.add(propertyName);
	}

	public void ignorePropertyBeanState(String... propertyNames) {
		monitorBeanState();
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
			if (!beanStateIgnoredProperties.contains(evt.getPropertyName())
					&& (beanStateMonitoredProperties == null
							|| beanStateMonitoredProperties.contains(evt.getPropertyName()))
					&& modifyBean() && log.isLoggable(Level.FINE)) {
				log.fine(MessageFormat.format("Bean '{'{0}'}' \"{2}\" modified by change of property ''{1}''",
						BasicBean.this.getClass().getSimpleName(), evt.getPropertyName(), BasicBean.this));
			}
		}
	}
}
