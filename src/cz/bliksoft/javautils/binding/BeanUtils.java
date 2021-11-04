package cz.bliksoft.javautils.binding;

import static cz.bliksoft.javautils.Preconditions.checkArgument;
import static cz.bliksoft.javautils.Preconditions.checkNotNull;

import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.VetoableChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import cz.bliksoft.javautils.binding.beans.ExtendedPropertyChangeSupport;
import cz.bliksoft.javautils.binding.beans.ExtendedVetoableChangeSupport;
import cz.bliksoft.javautils.binding.exceptions.PropertyNotBindableException;
import cz.bliksoft.javautils.binding.exceptions.PropertyUnboundException;
import cz.bliksoft.javautils.binding.interfaces.IObservable;
import cz.bliksoft.javautils.collections.WeakIdentityHashMap;

public class BeanUtils {

	private BeanUtils() {

	}

	private static final WeakIdentityHashMap<Object, PropertyChangeSupport> propertyChangeSupportInstances = new WeakIdentityHashMap<>();

	public static PropertyChangeSupport getPropertyChangeSupport(Object key) {
		return propertyChangeSupportInstances.computeIfAbsent(key, k -> new ExtendedPropertyChangeSupport(key));
	}

	private static final WeakIdentityHashMap<Object, VetoableChangeSupport> vetoableChangeSupportInstances = new WeakIdentityHashMap<>();

	public static VetoableChangeSupport getVetoableChangeSupport(Object key) {
		return vetoableChangeSupportInstances.computeIfAbsent(key, k -> new ExtendedVetoableChangeSupport(key));
	}

	public static boolean supportsBoundProperties(Class<?> cls) {
		return getPCLAdder(cls) != null && getPCLRemover(cls) != null;
	}

	private static final Class<?>[] PCL_PARAMS = new Class<?>[] { PropertyChangeListener.class };

	private static final Class<?>[] NAMED_PCL_PARAMS = new Class<?>[] { String.class, PropertyChangeListener.class };

	public static Method getPCLAdder(Class<?> clazz) {
		try {
			return clazz.getMethod("addPropertyChangeListener", PCL_PARAMS);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	public static Method getPCLRemover(Class<?> clazz) {
		try {
			return clazz.getMethod("removePropertyChangeListener", PCL_PARAMS);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	public static Method getNamedPCLAdder(Class<?> clazz) {
		try {
			return clazz.getMethod("addPropertyChangeListener", NAMED_PCL_PARAMS);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	public static Method getNamedPCLRemover(Class<?> clazz) {
		try {
			return clazz.getMethod("removePropertyChangeListener", NAMED_PCL_PARAMS);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	public static void addPropertyChangeListener(Object bean, Class<?> beanClass, PropertyChangeListener listener) {
		checkNotNull(listener, "The listener must not be null.");
		if (beanClass == null) {
			beanClass = bean.getClass();
		} else {
			checkArgument(beanClass.isInstance(bean), "The bean {0} must be an instance of {1}", bean, beanClass);
		}

		if (bean instanceof IObservable) {
			((IObservable) bean).addPropertyChangeListener(listener);
			return;
		}
		if (bean instanceof Component) {
			((Component) bean).addPropertyChangeListener(listener);
			return;
		}

		if (!BeanUtils.supportsBoundProperties(beanClass)) {
			throw new PropertyUnboundException("Bound properties unsupported by bean class=" + beanClass
					+ "\nThe Bean class must provide a pair of methods:"
					+ "\npublic void addPropertyChangeListener(PropertyChangeListener x);"
					+ "\npublic void removePropertyChangeListener(PropertyChangeListener x);");
		}

		Method multicastPCLAdder = getPCLAdder(beanClass);
		try {
			multicastPCLAdder.invoke(bean, listener);
		} catch (InvocationTargetException e) {
			throw new PropertyNotBindableException("Due to an InvocationTargetException we failed to add "
					+ "a multicast PropertyChangeListener to bean: " + bean, e.getCause());
		} catch (IllegalAccessException e) {
			throw new PropertyNotBindableException("Due to an IllegalAccessException we failed to add "
					+ "a multicast PropertyChangeListener to bean: " + bean, e);
		}
	}

	public static void addPropertyChangeListener(Object bean, Class<?> beanClass, String propertyName,
			PropertyChangeListener listener) {
		checkNotNull(propertyName, "The property name must not be null.");
		checkNotNull(listener, "The listener must not be null.");
		if (beanClass == null) {
			beanClass = bean.getClass();
		} else {
			checkArgument(beanClass.isInstance(bean), "The bean {0} must be an instance of {1}", bean, beanClass);
		}
		if (bean instanceof IObservable) {
			((IObservable) bean).addPropertyChangeListener(propertyName, listener);
			return;
		}
		if (bean instanceof Component) {
			((Component) bean).addPropertyChangeListener(propertyName, listener);
			return;
		}
		Method namedPCLAdder = getNamedPCLAdder(beanClass);
		if (namedPCLAdder == null) {
			throw new PropertyNotBindableException("Could not find the bean method"
					+ "\npublic void addPropertyChangeListener(String, PropertyChangeListener);" + "\nin bean: "
					+ bean);
		}
		try {
			namedPCLAdder.invoke(bean, propertyName, listener);
		} catch (InvocationTargetException e) {
			throw new PropertyNotBindableException("Due to an InvocationTargetException we failed to add "
					+ "a named PropertyChangeListener to bean: " + bean, e.getCause());
		} catch (IllegalAccessException e) {
			throw new PropertyNotBindableException("Due to an IllegalAccessException we failed to add "
					+ "a named PropertyChangeListener to bean: " + bean, e);
		}
	}

	public static void addPropertyChangeListener(Object bean, PropertyChangeListener listener) {
		addPropertyChangeListener(bean, (Class<?>) null, listener);
	}

	public static void addPropertyChangeListener(Object bean, String propertyName, PropertyChangeListener listener) {
		addPropertyChangeListener(bean, null, propertyName, listener);
	}

	public static void removePropertyChangeListener(Object bean, Class<?> beanClass, PropertyChangeListener listener) {
		checkNotNull(listener, "The listener must not be null.");
		if (beanClass == null) {
			beanClass = bean.getClass();
		} else {
			checkArgument(beanClass.isInstance(bean), "The bean {0} must be an instance of {1}", bean, beanClass);
		}
		if (bean instanceof IObservable) {
			((IObservable) bean).removePropertyChangeListener(listener);
			return;
		}
		if (bean instanceof Component) {
			((Component) bean).removePropertyChangeListener(listener);
			return;
		}
		Method multicastPCLRemover = getPCLRemover(beanClass);
		if (multicastPCLRemover == null) {
			throw new PropertyUnboundException("Could not find the method:"
					+ "\npublic void removePropertyChangeListener(String, PropertyChangeListener x);" + "\nfor bean:"
					+ bean);
		}
		try {
			multicastPCLRemover.invoke(bean, listener);
		} catch (InvocationTargetException e) {
			throw new PropertyNotBindableException("Due to an InvocationTargetException we failed to remove "
					+ "a multicast PropertyChangeListener from bean: " + bean, e.getCause());
		} catch (IllegalAccessException e) {
			throw new PropertyNotBindableException("Due to an IllegalAccessException we failed to remove "
					+ "a multicast PropertyChangeListener from bean: " + bean, e);
		}
	}

	public static void removePropertyChangeListener(Object bean, Class<?> beanClass, String propertyName,
			PropertyChangeListener listener) {
		checkNotNull(propertyName, "The property name must not be null.");
		checkNotNull(listener, "The listener must not be null.");
		if (beanClass == null) {
			beanClass = bean.getClass();
		} else {
			checkArgument(beanClass.isInstance(bean), "The bean {0} must be an instance of {1}", bean, beanClass);
		}
		if (bean instanceof IObservable) {
			((IObservable) bean).removePropertyChangeListener(propertyName, listener);
			return;
		}
		if (bean instanceof Component) {
			((Component) bean).removePropertyChangeListener(propertyName, listener);
			return;
		}
		Method namedPCLRemover = getNamedPCLRemover(beanClass);
		if (namedPCLRemover == null) {
			throw new PropertyNotBindableException("Could not find the bean method"
					+ "\npublic void removePropertyChangeListener(String, PropertyChangeListener);" + "\nin bean: "
					+ bean);
		}
		try {
			namedPCLRemover.invoke(bean, propertyName, listener);
		} catch (InvocationTargetException e) {
			throw new PropertyNotBindableException("Due to an InvocationTargetException we failed to remove "
					+ "a named PropertyChangeListener from bean: " + bean, e.getCause());
		} catch (IllegalAccessException e) {
			throw new PropertyNotBindableException("Due to an IllegalAccessException we failed to remove "
					+ "a named PropertyChangeListener from bean: " + bean, e);
		}
	}

	public static void removePropertyChangeListener(Object bean, PropertyChangeListener listener) {
		removePropertyChangeListener(bean, (Class<?>) null, listener);
	}

	public static void removePropertyChangeListener(Object bean, String propertyName, PropertyChangeListener listener) {
		removePropertyChangeListener(bean, null, propertyName, listener);
	}

}
