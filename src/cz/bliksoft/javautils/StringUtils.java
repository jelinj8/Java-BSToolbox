package cz.bliksoft.javautils;

import java.math.BigInteger;
import java.security.SecureRandom;

public class StringUtils {
	private static SecureRandom random = null;

	/**
	 * náhodná změť znaků
	 * 
	 * @return
	 */
	public static String randomString() {
		if (random == null)
			random = new SecureRandom();
		return new BigInteger(130, random).toString(32);
	}

	/**
	 * je řetězec prázdný?
	 * 
	 * @param value
	 * @return
	 */
	public static boolean isEmpty(String value) {
		if (value == null)
			return true;
		if (value.length() == 0)
			return true;
		return false;
	}

	/**
	 * je řetězec neprázdný?
	 * 
	 * @param value
	 * @return
	 */
	public static boolean hasValue(String value) {
		return !isEmpty(value);
	}

	/**
	 * trim ošetřený na NULL vstup (vrátí zase null)
	 * 
	 * @param val
	 * @return
	 */
	public static String trim(String val) {
		if (val == null)
			return null;
		return val.trim();
	}

	public static boolean hasLength(String value) {
		if (isEmpty(value))
			return false;
		return value.length() > 0;
	}
}
