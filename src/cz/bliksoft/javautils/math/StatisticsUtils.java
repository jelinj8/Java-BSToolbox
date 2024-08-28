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
	private static Map<Object, IStatisticFilter> filters = new HashMap<>();

	/**
	 * register averager (instance of specific implementation)
	 * 
	 * @param key
	 * @param averager instance of specific implementation ({@link RollingAverage},
	 *                 {@link ApproximatedRollingAverage}, {@link Average}...)
	 */
	public static void addFilter(Object key, IStatisticFilter averager) {
		filters.put(key, averager);
	}

	/**
	 * add new record to keyed sum
	 * 
	 * @param key
	 * @param value
	 */
	public static void addToFilter(Object key, Double value) {
		synchronized (filters) {
			IStatisticFilter val = filters.get(key);
			if (val == null) {
				Average a = new Average();
				a.addValue(value);
				filters.put(key, a);
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
	public static Double getValue(Object key) {
		synchronized (filters) {
			IStatisticFilter val = filters.get(key);
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
		synchronized (filters) {
			IStatisticFilter val = filters.get(key);
			if (val == null)
				return 0l;
			else
				return val.getCount();
		}
	}
}
