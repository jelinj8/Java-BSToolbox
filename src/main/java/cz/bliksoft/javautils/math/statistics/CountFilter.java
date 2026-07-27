package cz.bliksoft.javautils.math.statistics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Counts added values; the values themselves are ignored. Thread-safe.
 */
public class CountFilter implements IStatisticFilter {

	private final AtomicLong count = new AtomicLong();

	@Override
	public void addValue(Double value) {
		count.incrementAndGet();
	}

	@Override
	public void addValue(Long value) {
		count.incrementAndGet();
	}

	@Override
	public Long getLongValue() {
		return count.get();
	}

	@Override
	public Double getValue() {
		return Double.valueOf(count.get());
	}

	@Override
	public Long getCount() {
		return count.get();
	}

	@Override
	public Long getTotalCount() {
		return count.get();
	}

}
