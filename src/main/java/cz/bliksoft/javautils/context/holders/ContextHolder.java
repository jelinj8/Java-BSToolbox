package cz.bliksoft.javautils.context.holders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.context.Context;
import cz.bliksoft.javautils.context.ContextSearchResult;

public class ContextHolder extends Context {
	private static final Logger log = LogManager.getLogger();

	public ContextHolder(String comment) {
		super(comment);
	}

	@Override
	public ContextSearchResult getValue(Object key) {
		if (childContexts.isEmpty())
			return new ContextSearchResult(this, key, null);
		Context ctx = childContexts.get(0);
		//
		////		if (ctx instanceof KeyedContext) {
		////			return ((KeyedContext) ctx).getValue(key);
		////		} else if (ctx instanceof ListContext) {
		////			return ((ListContext) ctx).getValue(key);
		////		} else 
		//		if (ctx instanceof ContextWithValues) {
		//			return ((ContextWithValues) ctx).getValue(key);
		//		} else if (ctx instanceof SingleContextHolder) {
		//			return ((SingleContextHolder) ctx).getValue(key);
		//		} else {			
		//			log.error("ContextHolder can't search values in " + ctx.getClass().getName());
		//			return null;
		//		}
		return ctx.getValue(key);
	}

	/*
	 * add value into child context (key is optional for lists)
	 */
	public void putValue(Object key, Object value) {
		if (childContexts.isEmpty()) {
			log.error("Can't put value into empty SingleContextContainer ({})", comment);
			return;
		}

		Context ctx = childContexts.get(0);

		//		if (ctx instanceof KeyedContext) {
		//			((KeyedContext) ctx).put(key, value);
		//		} else if (ctx instanceof ListContext) {
		//			((ListContext) ctx).addValue(value);
		if (key != null)
			((Context) ctx).put(key, value);
		else
			((Context) ctx).addValue(value);
	}

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
