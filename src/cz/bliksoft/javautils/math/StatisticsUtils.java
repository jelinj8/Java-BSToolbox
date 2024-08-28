package cz.bliksoft.javautils.math;

import java.util.HashMap;
import java.util.Map;

import cz.bliksoft.javautils.math.statistics.ApproximatedRollingAverage;
import cz.bliksoft.javautils.math.statistics.Average;
import cz.bliksoft.javautils.math.statistics.IStatisticFilter;
import cz.bliksoft.javautils.math.statistics.RollingAverage;

/**
 * helper for recording process averages, by default sum/count (real average),
 * supports registering other implementations/filters
 */
public class StatisticsUtils {

	/**
	 * contains mapped values with <sum, count>
	 */
	private static Map<Object, IStatisticFilter> averages = new HashMap<>();

	/**
	 * register averager (instance of specific implementation)
	 * 
	 * @param key
	 * @param averager instance of specific implementation ({@link RollingAverage},
	 *                 {@link ApproximatedRollingAverage}, {@link Average}...)
	 */
	public static void addAverage(Object key, IStatisticFilter averager) {
		averages.put(key, averager);
	}

	/**
	 * add new record to keyed sum
	 * 
	 * @param key
	 * @param value
	 */
	public static void addToAverage(Object key, Double value) {
		synchronized (averages) {
			IStatisticFilter val = averages.get(key);
			if (val == null) {
				Average a = new Average();
				a.addValue(value);
				averages.put(key, a);
			} else {
				val.addValue(value);
			}
		}
	}

	/**
	 * get current keyed average
	 * 
	 * @param key
	 * @return
	 */
	public static Double getAverage(Object key) {
		synchronized (averages) {
			IStatisticFilter val = averages.get(key);
			if (val == null)
				return 0d;
			else
				return val.getValue();
		}
	}

	/**
	 * get recorded value count for a key
	 * 
	 * @param key
	 * @return
	 */
	public static Long getCount(Object key) {
		synchronized (averages) {
			IStatisticFilter val = averages.get(key);
			if (val == null)
				return 0l;
			else
				return val.getCount();
		}
	}
}
