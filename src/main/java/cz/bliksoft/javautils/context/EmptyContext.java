package cz.bliksoft.javautils.context;

import cz.bliksoft.javautils.StringUtils;

/**
 * implementace kontextu pro připojování vnořených kontextů
 * 
 */
public class EmptyContext extends Context {

	public EmptyContext(String comment) {
		super(comment);
	}

	@Override
	public String toString() {
		if (StringUtils.hasText(this.comment))
			return (isLevelContext ? "L" : "") + "EmptyCTX: " + this.comment;
		else
			return (isLevelContext ? "L" : "") + "EmptyCTX";
	}
}
