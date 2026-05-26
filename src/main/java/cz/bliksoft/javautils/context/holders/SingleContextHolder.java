package cz.bliksoft.javautils.context.holders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.context.Context;

/**
 * Context holder that enforces a single active child; the child can be replaced
 * atomically.
 */
public class SingleContextHolder extends ContextHolder {
	private static final Logger log = LogManager.getLogger();

	/** Creates a holder with the given debug label. */
	public SingleContextHolder(String comment) {
		super(comment);
	}

	/**
	 * Enforces the single-child constraint; logs an error and no-ops if already
	 * occupied.
	 */
	@Override
	public void addContext(Context context) {
		if (childContexts.isEmpty())
			super.addContext(context);
		else
			log.error("SingleContextHolder can hold MAX 1 context!");
	}

	/**
	 * Removes any existing child and installs the given context as the new active
	 * child.
	 */
	public void replaceContext(Context context) {
		if (!childContexts.isEmpty())
			removeAllContexts();
		super.addContext(context);
	}

	/** Returns the current child context, or null if none is set. */
	public Context getContext() {
		if (childContexts.isEmpty())
			return null;
		return childContexts.get(0);
	}

	@Override
	public String toString() {
		if (StringUtils.hasText(this.comment))
			return (isLevelContext ? "L" : "") + "SingleCTXHolder: " + this.comment;
		else
			return (isLevelContext ? "L" : "") + "SingleCTXHolder";
	}
}
