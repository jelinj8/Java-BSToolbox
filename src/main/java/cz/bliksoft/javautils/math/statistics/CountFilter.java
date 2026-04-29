package cz.bliksoft.javautils.math.statistics;

public class CountFilter implements IStatisticFilter {

	private long count;

	@Override
	public void addValue(Double value) {
		count++;
	}

	@Override
	public void addValue(Long value) {
		count++;
	}

	@Override
	public Long getLongValue() {
		return count;
	}

	@Override
	public Double getValue() {
		return Double.valueOf(count);
	}

	@Override
	public Long getCount() {
		return count;
	}

	@Override
	public Long getTotalCount() {
		return count;
	}

}
