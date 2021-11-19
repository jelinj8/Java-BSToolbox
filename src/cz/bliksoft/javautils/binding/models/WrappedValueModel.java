package cz.bliksoft.javautils.binding.models;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.javautils.ObjectUtils;
import cz.bliksoft.javautils.binding.interfaces.IValueModel;
import cz.bliksoft.javautils.logging.LogUtils;

/**
 * Wraps a <code>IValueModel</code>. Enables easy replacement of the wrapped
 * model without need to re-register change listeners.
 * 
 * @author jakub
 *
 * @param <T>
 */
public class WrappedValueModel<T> extends AbstractValueModel<T> {

	private static Logger log = Logger.getLogger(WrappedValueModel.class.getName());

	private String name = null;

	private StackTraceElement[] creationPoint;

	private IValueModel<T> wrappedModel = null;

	public WrappedValueModel() {
		if (log.isLoggable(Level.FINER)) {
			creationPoint = LogUtils.getStackTrace(25, 1);
		}
	}

	public WrappedValueModel(boolean checkIdentity) {
		this();
		this.checkIdentity = checkIdentity;
	}

	public WrappedValueModel(IValueModel<T> wrappedModel) {
		this();
		setValueModel(wrappedModel);
	}

	public WrappedValueModel(String name, IValueModel<T> wrappedModel) {
		this(wrappedModel);
		this.name = name;
	}

	public WrappedValueModel(IValueModel<T> wrappedModel, boolean checkIdentity) {
		this(wrappedModel);
		this.checkIdentity = checkIdentity;
	}

	public WrappedValueModel(String name, IValueModel<T> wrappedModel, boolean checkIdentity) {
		this(name, wrappedModel);
		this.checkIdentity = checkIdentity;
	}

	public IValueModel<T> getValueModel() {
		return wrappedModel;
	}

	public void setValueModel(IValueModel<T> wrappedModel) {
		if (this.wrappedModel != null)
			this.wrappedModel.removeValueChangeListener(valueListener);
		T oldValue = getValue();
		this.wrappedModel = wrappedModel;
		T newValue = getValue();
		if (this.wrappedModel != null)
			this.wrappedModel.addValueChangeListener(valueListener);

		if (oldValue == null && newValue == null) {
			return;
		}

		if ((!checkIdentity || oldValue != newValue) && log.isLoggable(Level.FINER)
				&& ObjectUtils.equals(oldValue, newValue)) {
			if (name == null)
				log.finer(MessageFormat.format("WrappedValue model replaced from {0} to {1}\n{2}", oldValue, newValue,
						LogUtils.traceToString(creationPoint, this.getClass().getName(), 25)));
			else
				log.finer(MessageFormat.format("Named WrappedValue model ''{0}'' replaced from {1} to {2}\n{3}", name,
						oldValue, newValue, LogUtils.traceToString(creationPoint, this.getClass().getName(), 25)));
		}
		firePropertyChange(PROPERTY_VALUE, oldValue, newValue, checkIdentity);
	}

	@Override
	public final T getValue() {
		return wrappedModel != null ? wrappedModel.getValue() : null;
	}

	@Override
	public final void setValue(T newValue) {
		if (wrappedModel != null)
			wrappedModel.setValue(newValue);
	}

	private PropertyChangeListener valueListener = new PropertyChangeListener() {

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getOldValue() == null && evt.getNewValue() == null) {
				return;
			}

			if ((!checkIdentity || evt.getOldValue() != evt.getNewValue()) && log.isLoggable(Level.FINER)
					&& ObjectUtils.equals(evt.getOldValue(), evt.getNewValue())) {
				if (name == null)
					log.finer(MessageFormat.format("WrappedValue model changed from {0} to {1}\n{2}", evt.getOldValue(),
							evt.getNewValue(), LogUtils.traceToString(creationPoint, this.getClass().getName(), 25)));
				else
					log.finer(MessageFormat.format("Named WrappedValue model ''{0}'' changed from {1} to {2}\n{3}",
							name, evt.getOldValue(), evt.getNewValue(),
							LogUtils.traceToString(creationPoint, this.getClass().getName(), 25)));
			}
			firePropertyChange(PROPERTY_VALUE, evt.getOldValue(), evt.getNewValue(), checkIdentity);
		}
	};

	public void release() {
		wrappedModel = null;
	}
}
