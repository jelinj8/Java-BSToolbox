package cz.bliksoft.javautils.math.statistics;

public interface IStatisticFilter {

	/**
	 * add new value
	 *
	 * @param value
	 */
	public void addValue(Double value);

	/**
	 * add new value
	 *
	 * @param value
	 */
	public void addValue(Long value);

	/**
	 * current {@link Long} value
	 *
	 * @return
	 */
	public Long getLongValue();

	/**
	 * current {@link Double} value
	 *
	 * @return
	 */
	public Double getValue();

	/**
	 * count of values having effect on result
	 *
	 * @return
	 */
	public Long getCount();

	/**
	 * total count of processed values
	 *
	 * @return
	 */
	public Long getTotalCount();
}
