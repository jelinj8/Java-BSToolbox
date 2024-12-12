package cz.bliksoft.javautils;

import java.util.Date;

/**
 * objekt, který má přiřazenou časovou značku
 * 
 * @author jjelinek
 *
 * @param <T>
 */
public class TimestampedObject<T> {

	T value;
	long timestamp;

	/**
	 * 
	 * @param value vlastní hodnota
	 */
	public TimestampedObject(T value) {
		this.value = value;
		touch();
	}

	/**
	 * nastavení časové značky na aktuální čas
	 */
	public void touch() {
		timestamp = (new Date()).getTime();
	}

	/**
	 * získání objektu
	 * 
	 * @return
	 */
	public T getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TimestampedObject<?>) {
			if (value == null)
				return (((TimestampedObject<?>) obj).getValue() == null);
			else
				return value.equals(((TimestampedObject<?>) obj).getValue());
		} else {
			if (value == null)
				return (obj == null);
			else
				return value.equals(obj);
		}
	}

	public static Object valueOf(TimestampedObject<?> obj) {
		if (obj == null)
			return null;
		return obj.getValue();
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
