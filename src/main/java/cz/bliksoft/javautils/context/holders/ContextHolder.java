package cz.bliksoft.javautils.context.holders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.context.Context;
import cz.bliksoft.javautils.context.ContextSearchResult;

/**
 * Abstract context that delegates value reads and writes to its first child
 * context.
 */
public class ContextHolder extends Context {
	private static final Logger log = LogManager.getLogger();

	/** Creates a holder with the given debug label. */
	public ContextHolder(String comment) {
		super(comment);
	}

	/**
	 * Delegates to the first child; returns an invalid result if no child is set.
	 */
	@Override
	public ContextSearchResult getValue(Object key) {
		if (childContexts.isEmpty())
			return new ContextSearchResult(this, key, null);
		Context ctx = childContexts.get(0);
		return ctx.getValue(key);
	}

	/** Delegates to the first child via {@code put(null, value)}. */
	@Override
	public void addValue(Object value) {
		put(null, value);
	}

	/**
	 * Delegates to the first child; logs an error and no-ops if no child is set.
	 */
	@Override
	public void put(Object key, Object value) {
		if (childContexts.isEmpty()) {
			log.error("Can't put value into empty SingleContextContainer ({})", comment);
			return;
		}

		Context ctx = childContexts.get(0);

		if (key != null)
			((Context) ctx).put(key, value);
		else
			((Context) ctx).addValue(value);
	}

	/** Returns true if a child context is currently attached. */
	public boolean isSet() {
		return !childContexts.isEmpty();
	}

	@Override
	public String toString() {
		if (StringUtils.hasText(this.comment))
			return (isLevelContext ? "L" : "") + "CTXHolder: " + this.comment;
		else
			return (isLevelContext ? "L" : "") + "CTXHolder";
	}
}
