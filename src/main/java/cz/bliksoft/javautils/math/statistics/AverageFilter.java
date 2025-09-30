package cz.bliksoft.javautils.math.statistics;

public class AverageFilter implements IStatisticFilter {

	Double sum = 0d;
	Long totalValCount = 0l;

	public AverageFilter() {
	}

	public void addValue(Double value) {
		this.sum += value;
		totalValCount++;
	}

	public void addValue(Long value) {
		addValue(value.doubleValue());
	}

	public void addValue(Double value, Long count) {
		this.sum += value;
		totalValCount += count;
	}

	public void addValue(Long value, Long count) {
		addValue(value.doubleValue(), count);
	}

	public Long getLongValue() {
		return Math.round(sum / totalValCount);
	}

	public Double getValue() {
		return sum / totalValCount;
	}

	public Long getCount() {
		return totalValCount;
	}

	public Long getTotalCount() {
		return totalValCount;
	}

}
