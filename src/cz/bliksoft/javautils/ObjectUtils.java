package cz.bliksoft.javautils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ObjectUtils {

	ObjectUtils() {

	}

	/**
	 * Provides a means to copy objects that do not implement Cloneable. Performs a
	 * deep copy where the copied object has no references to the original object
	 * for any object that implements Serializable. If the original is {@code null},
	 * this method just returns {@code null}.
	 *
	 * @param <T>      the type of the object to be cloned
	 * @param original the object to copied, may be {@code null}
	 * @return the copied object
	 *
	 * @since 1.1.1
	 */
	public static <T extends Serializable> T deepCopy(T original) {
		if (original == null) {
			return null;
		}
		try {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
			final ObjectOutputStream oas = new ObjectOutputStream(baos);
			oas.writeObject(original);
			oas.flush();
			// close is unnecessary
			final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			final ObjectInputStream ois = new ObjectInputStream(bais);
			return (T) ois.readObject();
		} catch (Throwable e) {
			throw new RuntimeException("Deep copy failed", e);
		}
	}

	/**
	 * Checks and answers if the two objects are both {@code null} or equal.
	 *
	 * <pre>
	 * Objects.equals(null, null) == true
	 * Objects.equals("Hi", "Hi") == true
	 * Objects.equals("Hi", null) == false
	 * Objects.equals(null, "Hi") == false
	 * Objects.equals("Hi", "Ho") == false
	 * </pre>
	 *
	 * @param o1 the first object to compare
	 * @param o2 the second object to compare
	 * @return boolean {@code true} if and only if both objects are {@code null} or
	 *         equal according to {@link Object#equals(Object) equals} invoked on
	 *         the first object
	 */
	public static boolean equals(Object o1, Object o2) {
		return o1 == o2 || o1 != null && o1.equals(o2);
	}
}
