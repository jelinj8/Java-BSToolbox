package cz.bliksoft.javautils.binding.exceptions;

import cz.bliksoft.javautils.binding.properties.PropertyAccessor;

/**
 * A runtime exception that describes read and write access problems when
 * getting/setting a Java Bean property.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.9 $
 * @see com.jgoodies.binding.beans.PropertyAdapter
 */
public final class PropertyAccessException extends PropertyException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3114614676607000862L;

	/**
	 * Constructs a new exception instance with the specified detail message and
	 * cause.
	 *
	 * @param message the detail message which is saved for later retrieval by the
	 *                {@link #getMessage()} method.
	 * @param cause   the cause which is saved for later retrieval by the
	 *                {@link #getCause()} method. A {@code null} value is permitted,
	 *                and indicates that the cause is nonexistent or unknown.
	 */
	public PropertyAccessException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates and returns a new PropertyAccessException instance for a failed read
	 * access for the specified bean, property accessor and cause.
	 *
	 * @param bean             the target bean
	 * @param propertyAccessor describes the bean's property
	 * @param cause            the cause which is saved for later retrieval by the
	 *                         {@link #getCause()} method. A {@code null} value is
	 *                         permitted, and indicates that the cause is
	 *                         nonexistent or unknown.
	 * @return an exception that describes a read access problem
	 */
	@SuppressWarnings("rawtypes")
	public static PropertyAccessException createReadAccessException(Object bean, PropertyAccessor propertyAccessor,
			Throwable cause) {
		return new PropertyAccessException(createReadAccessMessage(bean, propertyAccessor, cause), cause);
	}

	/**
	 * Creates and returns a new PropertyAccessException instance for a failed read
	 * access for the specified bean, property accessor and cause.
	 *
	 * @param bean             the target bean
	 * @param propertyAccessor describes the bean's property
	 * @param causeMessage     describes the cause
	 * @return an exception that describes a read access problem
	 */
	@SuppressWarnings("rawtypes")
	public static PropertyAccessException createReadAccessException(Object bean, PropertyAccessor propertyAccessor,
			String causeMessage) {
		return new PropertyAccessException(createReadAccessMessage(bean, propertyAccessor, causeMessage), null);
	}

	/**
	 * Creates and returns a new PropertyAccessException instance for a failed write
	 * access for the specified bean, value, property accessor and cause.
	 *
	 * @param bean             the target bean
	 * @param value            the value that could not be set
	 * @param propertyAccessor describes the bean's property
	 * @param cause            the cause which is saved for later retrieval by the
	 *                         {@link #getCause()} method. A {@code null} value is
	 *                         permitted, and indicates that the cause is
	 *                         nonexistent or unknown.
	 * @return an exception that describes a write access problem
	 */
	@SuppressWarnings("rawtypes")
	public static PropertyAccessException createWriteAccessException(Object bean, Object value,
			PropertyAccessor propertyAccessor, Throwable cause) {
		return new PropertyAccessException(createWriteAccessMessage(bean, value, propertyAccessor, cause), cause);
	}

	/**
	 * Creates and returns a new PropertyAccessException instance for a failed write
	 * access for the specified bean, value, property accessor and cause.
	 *
	 * @param bean             the target bean
	 * @param value            the value that could not be set
	 * @param propertyAccessor describes the bean's property
	 * @param causeMessage     describes the cause
	 * @return an exception that describes a write access problem
	 */
	@SuppressWarnings("rawtypes")
	public static PropertyAccessException createWriteAccessException(Object bean, Object value,
			PropertyAccessor propertyAccessor, String causeMessage) {
		return new PropertyAccessException(createWriteAccessMessage(bean, value, propertyAccessor, causeMessage), null);
	}

	@SuppressWarnings("rawtypes")
	private static String createReadAccessMessage(Object bean, PropertyAccessor propertyAccessor, Object cause) {
		String beanType = bean == null ? null : bean.getClass().getName();
		return "Failed to read an adapted Java Bean property." + "\ncause=" + cause + "\nbean=" + bean + "\nbean type="
				+ beanType + "\nproperty name=" + propertyAccessor.getPropertyName() + "\nproperty type="
				+ propertyAccessor.getPropertyType().getName() + "\nproperty reader="
				+ propertyAccessor.getReadMethod();
	}

	@SuppressWarnings("rawtypes")
	private static String createWriteAccessMessage(Object bean, Object value, PropertyAccessor propertyAccessor,
			Object cause) {

		String beanType = bean == null ? null : bean.getClass().getName();
		String valueType = value == null ? null : value.getClass().getName();
		return "Failed to set an adapted Java Bean property." + "\ncause=" + cause + "\nbean=" + bean + "\nbean type="
				+ beanType + "\nvalue=" + value + "\nvalue type=" + valueType + "\nproperty name="
				+ propertyAccessor.getPropertyName() + "\nproperty type=" + propertyAccessor.getPropertyType().getName()
				+ "\nproperty setter=" + propertyAccessor.getWriteMethod();
	}

}
