package cz.bliksoft.javautils.math.statistics;

public class ApproximatedRollingAverage implements IStatisticFilter {
	Double value = 0d;
	Long totalValCount = 0l;
	int valCount = 0;
	int windowSize = 10;

	public ApproximatedRollingAverage(int windowSize) {
		this.windowSize = windowSize;
	}

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

	public void addValue(Long value) {
		addValue(value.doubleValue());
	}

	public Long getLongValue() {
		return Math.round(value);
	}

	public Double getValue() {
		return value;
	}

	public Long getCount() {
		return totalValCount;
	}
}
