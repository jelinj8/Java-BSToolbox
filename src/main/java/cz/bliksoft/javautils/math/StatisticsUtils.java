package cz.bliksoft.javautils.math;

import java.util.HashMap;
import java.util.Map;

import cz.bliksoft.javautils.math.statistics.ApproximatedRollingAverage;
import cz.bliksoft.javautils.math.statistics.AverageFilter;
import cz.bliksoft.javautils.math.statistics.IStatisticFilter;
import cz.bliksoft.javautils.math.statistics.RollingAverageFilter;

/**
 * helper for processing values, by default sum/count (real average),
 * supports registering other implementations/filters
 */
public class StatisticsUtils {

	/**
	 * contains mapped filters
	 */
	private static Map<Object, IStatisticFilter> filters = new HashMap<>();

	/**
	 * register {@link IStatisticFilter} (instance of specific implementation)
	 * 
	 * @param key
	 * @param averager
	 *            instance of specific implementation ({@link RollingAverageFilter},
	 *            {@link ApproximatedRollingAverage}, {@link AverageFilter}...)
	 */
	public static void addFilter(Object key, IStatisticFilter averager) {
		filters.put(key, averager);
	}

	/**
	 * add new record to keyed filter
	 * 
	 * @param key
	 * @param value
	 */
	public static void addValue(Object key, Double value) {
		synchronized (filters) {
			IStatisticFilter val = filters.get(key);
			if (val == null) {
				AverageFilter a = new AverageFilter();
				a.addValue(value);
				filters.put(key, a);
			} else {
				val.addValue(value);
			}
		}
	}

	/**
	 * get current keyed filter value, swallows potential
	 * {@link ArithmeticException} (e.g. division by zero)
	 * 
	 * @param key
	 * @return
	 */
	public static Double getValue(Object key) {
		synchronized (filters) {
			IStatisticFilter val = filters.get(key);
			if (val == null)
				return 0d;
			else {
				try {
					return val.getValue();
				} catch (ArithmeticException e) {
					return 0d;
				}
			}
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
