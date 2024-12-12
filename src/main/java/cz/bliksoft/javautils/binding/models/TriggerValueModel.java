package cz.bliksoft.javautils.binding.models;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.javautils.ObjectUtils;
import cz.bliksoft.javautils.logging.LogUtils;

/**
 * Value model that checks new value only against NULL (or preset neutral value).
 * 
 * To be used as event source for bound listeners.
 * @author jakub
 *
 * @param <T>
 */
public class TriggerValueModel<T> extends AbstractValueModel<T> {

	private static Logger log = Logger.getLogger(TriggerValueModel.class.getName());

	private String name = null;

	private StackTraceElement[] creationPoint;

	public TriggerValueModel() {
		if (log.isLoggable(Level.FINER)) {
			creationPoint = LogUtils.getStackTrace(25, 1);
		}
	}

	public TriggerValueModel(boolean checkIdentity) {
		this();
		this.checkIdentity = checkIdentity;
	}

	public TriggerValueModel(T neutralValue) {
		this();
		this.value = neutralValue;
	}

	public TriggerValueModel(String name, T neutralValue) {
		this(neutralValue);
		this.name = name;
	}

	public TriggerValueModel(T neutralValue, boolean checkIdentity) {
		this(neutralValue);
		this.checkIdentity = checkIdentity;
	}

	public TriggerValueModel(String name, T neutralValue, boolean checkIdentity) {
		this(neutralValue, checkIdentity);
		this.name = name;
	}

	private T value = null;

	@Override
	public final T getValue() {
		return value;
	}

	@Override
	public final void setValue(T newValue) {
		T oldValue = this.value;
		if (oldValue == null && newValue == null) {
			return;
		}

		if (name != null && !log.isLoggable(Level.FINER) && (!checkIdentity || oldValue != newValue)
				&& !ObjectUtils.equals(oldValue, newValue)) {
			log.info(MessageFormat.format("TriggerValue model ''{0}'' changed from [{1}] to [{2}]", name, oldValue, newValue));
		} else if ((!checkIdentity || oldValue != newValue) && log.isLoggable(Level.FINER)
				&& !ObjectUtils.equals(oldValue, newValue)) {
			if (name == null)
				log.finer(MessageFormat.format("TriggerValue model changed from {0} to {1}\n{2}", oldValue, newValue,
						LogUtils.traceToString(creationPoint, this.getClass().getName(), 25)));
			else
				log.finer(MessageFormat.format("Named triggerVvalue model ''{0}'' changed from {1} to {2}\n{3}", name, oldValue,
						newValue, LogUtils.traceToString(creationPoint, this.getClass().getName(), 25)));
		}
		firePropertyChange(PROPERTY_VALUE, oldValue, newValue, checkIdentity);
	}
}
