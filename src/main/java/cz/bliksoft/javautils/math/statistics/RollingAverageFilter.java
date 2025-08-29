package cz.bliksoft.javautils.math.statistics;

import java.util.ArrayDeque;
import java.util.Deque;

public class RollingAverageFilter implements IStatisticFilter {

	Deque<Double> values;
	Double sum = 0d;

	Long totalValCount = 0l;

	int windowSize = 10;

	public RollingAverageFilter(int windowSize) {
		this.windowSize = windowSize;
		values = new ArrayDeque<Double>(windowSize + 1);
	}

	public void addValue(Double value) {
		sum += value;
		values.add(value);
		if (values.size() > windowSize)
			sum -= values.removeFirst();
		totalValCount++;
	}

	public void addValue(Long value) {
		addValue(value.doubleValue());
	}

	public Long getLongValue() {
		return Math.round(sum / values.size());
	}

	public Double getValue() {
		return sum / values.size();
	}

	public Long getCount() {
		return (long) values.size();
	}

	@Override
	public Long getTotalCount() {
		return totalValCount;
	}

}
