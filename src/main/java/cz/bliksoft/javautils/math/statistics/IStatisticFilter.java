package cz.bliksoft.javautils.math.statistics;

public interface IStatisticFilter {
	public void addValue(Double value);

	public void addValue(Long value);

	public Long getLongValue();

	public Double getValue();

	public Long getCount();
}
