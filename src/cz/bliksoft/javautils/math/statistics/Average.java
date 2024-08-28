package cz.bliksoft.javautils.math.statistics;

public class Average implements IStatisticFilter {

	Double sum;
	Long totalValCount = 0l;

	public Average() {
	}

	public void addValue(Double value) {
		this.sum += value;
		totalValCount++;
	}

	public void addValue(Long value) {
		addValue(value.doubleValue());
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
}
