package cz.bliksoft.javautils.math.statistics;

import java.util.ArrayDeque;
import java.util.Deque;

public class ThroughputFilter implements IStatisticFilter {

	private final long windowMs;
	private final long startTime;

	private final Deque<Long> timestamps = new ArrayDeque<>();
	private long count = 0;
	private long totalCount = 0;

	/** @param windowSeconds length of the sliding time window in seconds */
	public ThroughputFilter(int windowSeconds) {
		this.windowMs  = windowSeconds * 1000L;
		this.startTime = System.currentTimeMillis();
	}

	private void evict(long cutoff) {
		while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
			timestamps.removeFirst();
			count--;
		}
	}

	@Override
	public void addValue(Double value) {
		long now = System.currentTimeMillis();
		evict(now - windowMs);
		timestamps.addLast(now);
		count++;
		totalCount++;
	}

	@Override
	public void addValue(Long value) {
		addValue(value.doubleValue());
	}

	/**
	 * Per-second throughput over the configured window.
	 * When the filter is younger than its window, the result is extrapolated
	 * from elapsed time so the rate stabilises progressively.
	 */
	@Override
	public Double getValue() {
		long now = System.currentTimeMillis();
		evict(now - windowMs);
		long effectiveWindowMs = Math.min(now - startTime, windowMs);
		if (effectiveWindowMs == 0) return 0.0;
		return count * 1000.0 / effectiveWindowMs;
	}

	@Override
	public Long getLongValue() {
		return Math.round(getValue());
	}

	/** Raw count of events within the current window. */
	@Override
	public Long getCount() {
		return count;
	}

	@Override
	public Long getTotalCount() {
		return totalCount;
	}

}
