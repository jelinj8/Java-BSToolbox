package cz.bliksoft.javautils.context.holders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.context.Context;

public class SingleContextHolder extends ContextHolder {
	private static final Logger log = LogManager.getLogger();

	public SingleContextHolder(String comment) {
		super(comment);
	}

	@Override
	public void addContext(Context context) {
		if (childContexts.isEmpty())
			super.addContext(context);
		else
			log.error("SingleContextHolder can hold MAX 1 context!");
	}

	public void replaceContext(Context context) {
		if (!childContexts.isEmpty())
			removeAllContexts();
		super.addContext(context);
	}

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
