package cz.bliksoft.javautils;

import java.util.HashMap;
import java.util.Map;

/**
 * helper for recording process averages
 */
public class AveragingUtils {

	/**
	 * contains mapped values with <sum, count>
	 */
	private static Map<String, DoubleObject<Double, Long>> averages = new HashMap<>();

	/**
	 * add new record to keyed sum
	 * 
	 * @param key
	 * @param value
	 */
	public static void addToAverage(String key, Double value) {
		synchronized (averages) {
			DoubleObject<Double, Long> val = averages.get(key);
			if (val == null)
				averages.put(key, new DoubleObject<Double, Long>(value, 1l));
			else {
				Double sum = val.getO1();
				sum += value;
				Long count = val.getO2();
				count++;
				averages.put(key, new DoubleObject<Double, Long>(sum, count));
			}
		}
	}

	/**
	 * get current keyed average
	 * 
	 * @param key
	 * @return
	 */
	public static Double getAverage(String key) {
		synchronized (averages) {
			DoubleObject<Double, Long> val = averages.get(key);
			if (val == null)
				return 0d;
			else
				return val.getO1() / val.getO2();
		}
	}

	/**
	 * get recorded value count for a key
	 * 
	 * @param key
	 * @return
	 */
	public static Long getCount(String key) {
		synchronized (averages) {
			DoubleObject<Double, Long> val = averages.get(key);
			if (val == null)
				return 0l;
			else
				return val.getO2();
		}
	}
}
