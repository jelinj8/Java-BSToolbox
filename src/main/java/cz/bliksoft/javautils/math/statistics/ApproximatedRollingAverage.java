package cz.bliksoft.javautils.math.statistics;

public class ApproximatedRollingAverage implements IStatisticFilter {
	Double value = 0d;
	Long totalValCount = 0l;
	int valCount = 0;
	int windowSize = 10;

	public ApproximatedRollingAverage(int windowSize) {
		this.windowSize = windowSize;
	}

	@Override
	public void addValue(Double value) {
		if (valCount == 0) {
			this.value = value;
			valCount++;
		} else {
			if (valCount < windowSize)
				valCount++;

			this.value = ((this.value * (valCount - 1)) + value) / valCount;
		}
	}

	@Override
	public void addValue(Long value) {
		addValue(value.doubleValue());
	}

	@Override
	public Long getLongValue() {
		return Math.round(value);
	}

	@Override
	public Double getValue() {
		return value;
	}

	@Override
	public Long getCount() {
		return (long) valCount;
	}

	@Override
	public Long getTotalCount() {
		return totalValCount;
	}
}
