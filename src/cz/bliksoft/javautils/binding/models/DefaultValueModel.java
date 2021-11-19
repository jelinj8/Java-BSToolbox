package cz.bliksoft.javautils.binding.models;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.javautils.ObjectUtils;
import cz.bliksoft.javautils.logging.LogUtils;

public class DefaultValueModel<T> extends AbstractValueModel<T> {

	private static Logger log = Logger.getLogger(DefaultValueModel.class.getName());

	private String name = null;

	private StackTraceElement[] creationPoint;

	public DefaultValueModel() {
		if (log.isLoggable(Level.FINER)) {
			creationPoint = LogUtils.getStackTrace(25, 1);
		}
	}

	public DefaultValueModel(boolean checkIdentity) {
		this();
		this.checkIdentity = checkIdentity;
	}

	public DefaultValueModel(T initialValue) {
		this();
		this.value = initialValue;
	}

	public DefaultValueModel(String name, T initialValue) {
		this(initialValue);
		this.name = name;
	}

	public DefaultValueModel(T initialValue, boolean checkIdentity) {
		this(initialValue);
		this.checkIdentity = checkIdentity;
	}

	public DefaultValueModel(String name, T initialValue, boolean checkIdentity) {
		this(initialValue, checkIdentity);
		this.name = name;
	}

	private T value;

	@Override
	public final T getValue() {
		return value;
	}

	@Override
	public final void setValue(T newValue) {
		T oldValue = this.value;
		this.value = newValue;
		if (oldValue == null && newValue == null) {
			return;
		}

		if (name != null && !log.isLoggable(Level.FINER) && (!checkIdentity || oldValue != newValue)
				&& !ObjectUtils.equals(oldValue, newValue)) {
			log.info(MessageFormat.format("Value model ''{0}'' changed from [{1}] to [{2}]", name, oldValue, newValue));
		} else if ((!checkIdentity || oldValue != newValue) && log.isLoggable(Level.FINER)
				&& !ObjectUtils.equals(oldValue, newValue)) {
			if (name == null)
				log.finer(MessageFormat.format("Value model changed from {0} to {1}\n{2}", oldValue, newValue,
						LogUtils.traceToString(creationPoint, this.getClass().getName(), 25)));
			else
				log.finer(MessageFormat.format("Named value model ''{0}'' changed from {1} to {2}\n{3}", name, oldValue,
						newValue, LogUtils.traceToString(creationPoint, this.getClass().getName(), 25)));
		}
		firePropertyChange(PROPERTY_VALUE, oldValue, newValue, checkIdentity);
	}
}
