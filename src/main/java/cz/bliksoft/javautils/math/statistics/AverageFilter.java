package cz.bliksoft.javautils.math.statistics;

/**
 * Cumulative arithmetic mean of all added values. Thread-safe.
 */
public class AverageFilter implements IStatisticFilter {

	Double sum = 0d;
	Long totalValCount = 0l;

	public AverageFilter() {
	}

	public synchronized void addValue(Double value) {
		this.sum += value;
		totalValCount++;
	}

	public void addValue(Long value) {
		addValue(value.doubleValue());
	}

	/**
	 * adds a pre-aggregated sum with its own count
	 *
	 * @param value sum of the aggregated values
	 * @param count number of values the sum was computed from
	 */
	public synchronized void addValue(Double value, Long count) {
		this.sum += value;
		totalValCount += count;
	}

	public void addValue(Long value, Long count) {
		addValue(value.doubleValue(), count);
	}

	public synchronized Long getLongValue() {
		return Math.round(sum / totalValCount);
	}

	public synchronized Double getValue() {
		return sum / totalValCount;
	}

	public synchronized Long getCount() {
		return totalValCount;
	}

	public synchronized Long getTotalCount() {
		return totalValCount;
	}

}
