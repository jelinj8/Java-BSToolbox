package cz.bliksoft.javautils.math.statistics;

/**
 * Approximate rolling mean with O(1) memory — no window storage; each new value
 * is blended in with weight {@code 1/windowSize} once warmed up. Thread-safe.
 */
public class ApproximatedRollingAverage implements IStatisticFilter {
	Double value = 0d;
	Long totalValCount = 0l;
	int valCount = 0;
	int windowSize = 10;

	public ApproximatedRollingAverage(int windowSize) {
		this.windowSize = windowSize;
	}

	@Override
	public synchronized void addValue(Double value) {
		if (valCount == 0) {
			this.value = value;
			valCount++;
		} else {
			if (valCount < windowSize)
				valCount++;

			this.value = ((this.value * (valCount - 1)) + value) / valCount;
		}
		totalValCount++;
	}

	@Override
	public void addValue(Long value) {
		addValue(value.doubleValue());
	}

	@Override
	public synchronized Long getLongValue() {
		return Math.round(value);
	}

	@Override
	public synchronized Double getValue() {
		return value;
	}

	@Override
	public synchronized Long getCount() {
		return (long) valCount;
	}

	@Override
	public synchronized Long getTotalCount() {
		return totalValCount;
	}
}
