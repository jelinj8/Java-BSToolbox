package cz.bliksoft.javautils.binding.properties;

/*
 * Copyright (c) 2002-2015 JGoodies Software GmbH. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of JGoodies Software GmbH nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import static cz.bliksoft.javautils.Preconditions.checkArgument;
import static cz.bliksoft.javautils.Preconditions.checkNotNull;

import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Function;

import cz.bliksoft.javautils.binding.exceptions.PropertyAccessException;
import cz.bliksoft.javautils.binding.exceptions.UnsupportedException;

/**
 * An unmodifiable Object that describes and provides access to a bean property.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.2 $
 *
 * @since 2.2
 */
public final class PropertyAccessor<B, V> {

	private final String propertyName;

	private final Method readMethod;
	private final Method writeMethod;

	private final BiConsumer<B, V> setterFunction;
	private final Function<B, V> getterFunction;
	private final Class<?> propertyClass;

	public enum AccessorType {
		FUNCTION, METHOD
	}

	private AccessorType accessorType;

	// Instance Creation ------------------------------------------------------

	/**
	 * Constructs a PropertyAcessor for the given property name, reader and writer.
	 *
	 * @param propertyName the name of the property, e.g. "name", "enabled"
	 * @param readMethod   the method that returns the property value, e.g.
	 *                     {@code getName()}, {@code isEnabled()}
	 * @param writeMethod  the method that sets the property value, e.g.
	 *                     {@code setName(String)}, {@code setEnabled(boolean)}
	 * @throws NullPointerException if {@code propertyName} is null
	 */
	public PropertyAccessor(String propertyName, Method readMethod, Method writeMethod) {
		checkNotNull(propertyName, "The property name must not be null.");
		checkArgument(readMethod != null || writeMethod != null, "Either the reader or writer must not be null.");
		this.propertyName = propertyName;
		this.readMethod = readMethod;
		this.writeMethod = writeMethod;
		accessorType = AccessorType.METHOD;
		this.setterFunction = null;
		this.getterFunction = null;
		this.propertyClass = null;
	}

	public PropertyAccessor(String propertyName, Class<?> propertyClass, Function<B, V> getterFunction,
			BiConsumer<B, V> setterFunction) {
		checkNotNull(propertyName, "The property name must not be null.");
		checkArgument(getterFunction != null || setterFunction != null,
				"Either the getter or setter must not be null.");
		this.propertyName = propertyName;
		this.getterFunction = getterFunction;
		this.setterFunction = setterFunction;
		this.propertyClass = propertyClass;
		accessorType = AccessorType.FUNCTION;
		this.writeMethod = null;
		this.readMethod = null;
	}

	// Accessors -------------------------------------------------------------

	/**
	 * @return the bean property name.
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * @return the type of the accessed bean property
	 */
	public Class<?> getPropertyType() {
		if (accessorType == AccessorType.FUNCTION)
			return propertyClass;
		else
			return readMethod != null ? readMethod.getReturnType() : writeMethod.getParameterTypes()[0];
	}

	/**
	 * @return the Method used to read the property value or {@code null}, if not
	 *         available
	 */
	public Method getReadMethod() {
		if (accessorType == AccessorType.FUNCTION)
			throw new UnsupportedException("PropertyAccessot " + this + " is Function based.");
		return readMethod;
	}

	public Function<B, V> getGetterFunction() {
		if (accessorType == AccessorType.METHOD)
			throw new UnsupportedException("PropertyAccessot " + this + " is Method based.");
		return getterFunction;
	}

	/**
	 * @return the Method used to write the property value or {@code null}, if not
	 *         available
	 */
	public Method getWriteMethod() {
		if (accessorType == AccessorType.FUNCTION)
			throw new UnsupportedException("PropertyAccessot " + this + " is Function based.");
		return writeMethod;
	}

	public BiConsumer<B, V> getSetterFunction() {
		if (accessorType == AccessorType.METHOD)
			throw new UnsupportedException("PropertyAccessot " + this + " is Method based.");
		return setterFunction;
	}

	/**
	 * @return {@code true} if the property cannot be written, {@code false} if it
	 *         can be written
	 */
	public boolean isReadOnly() {
		return (accessorType == AccessorType.METHOD ? writeMethod == null : setterFunction == null);
	}

	/**
	 * @return {@code true} if the property cannot be read, {@code false} if it can
	 *         be read
	 */
	public boolean isWriteOnly() {
		return (accessorType == AccessorType.METHOD ? readMethod == null : getterFunction == null);
	}

	// Operations -------------------------------------------------------------

	/**
	 * Invokes this accessor's reader on the given bean.
	 *
	 * @return the value of the property that is described by this accessor when
	 *         read from the given bean
	 *
	 * @param bean the target bean where the reader is to be invoked
	 *
	 * @throws NullPointerException          if {@code bean} is {@code null}
	 * @throws UnsupportedOperationException if the property is write-only
	 */
	@SuppressWarnings("unchecked")
	public V getValue(B bean) {
		checkNotNull(bean, "The bean must not be null.");
		if (isWriteOnly()) {
			throw PropertyAccessException.createReadAccessException(bean, this, createMissingGetterCause(bean));
		}
		if (accessorType == AccessorType.FUNCTION)
			return getterFunction.apply(bean);
		else {
			try {
				return (V) readMethod.invoke(bean, (Object[]) null);
			} catch (InvocationTargetException e) {
				throw PropertyAccessException.createReadAccessException(bean, this, e.getCause());
			} catch (IllegalAccessException e) {
				throw PropertyAccessException.createReadAccessException(bean, this, e);
			}
		}
	}

	/**
	 * Invokes this accessor's writer on the given bean with the given value.
	 *
	 * @param bean     the target bean where the property value shall be set
	 * @param newValue the value to set
	 *
	 * @throws NullPointerException          if {@code bean} is {@code null}
	 * @throws UnsupportedOperationException if the property is write-only
	 * @throws PropertyVetoException         if the invoked writer throws such an
	 *                                       exception
	 */
	public void setValue(B bean, V newValue) throws PropertyVetoException {
		checkNotNull(bean, "The bean must not be null.");
		if (isReadOnly()) {
			throw PropertyAccessException.createWriteAccessException(bean, newValue, this,
					createMissingSetterCause(bean));
		}
		if (accessorType == AccessorType.FUNCTION)
			setterFunction.accept(bean, newValue);
		else {
			try {
				writeMethod.invoke(bean, newValue);
			} catch (InvocationTargetException e) {
				Throwable cause = e.getCause();
				if (cause instanceof PropertyVetoException) {
					throw (PropertyVetoException) cause;
				}
				throw PropertyAccessException.createWriteAccessException(bean, newValue, this, cause);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw PropertyAccessException.createWriteAccessException(bean, newValue, this, e);
			}

		}
	}

	private String createMissingGetterCause(B bean) {
		boolean isBooleanProperty = getPropertyType() == Boolean.class || getPropertyType() == Boolean.TYPE;
		String methodName1 = formatAccessorMethodSignature(bean, "is", null);
		String methodName2 = formatAccessorMethodSignature(bean, "get", null);
		String typicalMethodOrMethods = isBooleanProperty ? methodName1 + " or " + methodName2 : methodName2;
		return String.format(
				"The bean property is write-only; " + "the bean class has no public getter method, typically " + "%s.",
				typicalMethodOrMethods);
	}

	private String createMissingSetterCause(B bean) {
		String methodName = formatAccessorMethodSignature(bean, "set", getPropertyType());
		return String.format(
				"The bean property is read-only; " + "the bean class has no public setter method, typically " + "%s.",
				methodName);
	}

	private String formatAccessorMethodSignature(Object bean, String methodPrefix, Class<?> methodParamType) {
		return String.format("%1$s#%2$s%3$s(%4$s)", bean.getClass().getSimpleName(), methodPrefix,
				capitalizedPropertyName(), methodParamType == null ? "" : methodParamType.getSimpleName());
	}

	private String capitalizedPropertyName() {
		return getPropertyName().substring(0, 1).toUpperCase() + getPropertyName().substring(1);
	}

	// Overriding Superclass Behavior -----------------------------------------

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		PropertyAccessor<B, V> other = null;
		try {
			other = (PropertyAccessor<B, V>) obj;
		} catch (ClassCastException e) {
			return false;
		}

		if (propertyName == null) {
			if (other.propertyName != null) {
				return false;
			}
		} else if (!propertyName.equals(other.propertyName)) {
			return false;
		}

		if (other.accessorType != accessorType)
			return false;
		if (accessorType == AccessorType.METHOD) {
			if (readMethod == null) {
				if (other.readMethod != null) {
					return false;
				}
			} else if (!readMethod.equals(other.readMethod)) {
				return false;
			}
			if (writeMethod == null) {
				if (other.writeMethod != null) {
					return false;
				}
			} else if (!writeMethod.equals(other.writeMethod)) {
				return false;
			}
		} else {
			if (getterFunction == null) {
				if (other.getterFunction != null) {
					return false;
				}
			} else if (!getterFunction.equals(other.getterFunction)) {
				return false;
			}
			if (setterFunction == null) {
				if (other.setterFunction != null) {
					return false;
				}
			} else if (!setterFunction.equals(other.setterFunction)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (propertyName == null ? 0 : propertyName.hashCode());
		result = prime * result + (readMethod == null ? 0 : readMethod.hashCode());
		result = prime * result + (writeMethod == null ? 0 : writeMethod.hashCode());
		result = prime * result + (getterFunction == null ? 0 : getterFunction.hashCode());
		result = prime * result + (setterFunction == null ? 0 : setterFunction.hashCode());
		result = prime * result + (propertyClass == null ? 0 : propertyClass.hashCode());
		return result;
	}

}
