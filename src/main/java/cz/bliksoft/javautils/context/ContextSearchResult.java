package cz.bliksoft.javautils.context;

import java.text.MessageFormat;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Wraps the result of a key lookup in the context tree. */
public class ContextSearchResult {
	private static final Logger log = LogManager.getLogger();

	Object result;
	Object key;
	Context ctx;
	int level = 0;

	/**
	 * kontext, ze kterého pochází výsledek
	 *
	 * @return
	 */
	public Context getContext() {
		return ctx;
	}

	/** Returns the found value; logs an error if the result is invalid. */
	public Object getResult() {
		if (!isValid()) {
			// throw new InvalidObjectException("Empty value!");
			log.log(Level.ERROR, "Reading invalid value!"); //$NON-NLS-1$
		}
		return result;
	}

	/** Whether the lookup found a matching value. */
	Boolean valid;

	/** Returns the key used for the lookup. */
	public Object getKey() {
		return key;
	}

	/** Returns true if a value was found for the key. */
	public boolean isValid() {
		return valid;
	}

	/**
	 * Creates an invalid (not-found) result as a null-safe substitute for a missing
	 * value.
	 */
	public static ContextSearchResult getInvalid(Context ctx, Object key) {
		ContextSearchResult res = new ContextSearchResult(null, key, null);
		res.valid = false;
		return res;
	}

	public ContextSearchResult(Context ctx, Object key) {
		valid = false;
		this.ctx = ctx;
		this.key = key;
	}

	/** Creates a valid result containing the found value. */
	public ContextSearchResult(Context _ctx, Object key, Object value) {
		this.valid = true;
		this.result = value;
		this.key = key;
		this.ctx = _ctx;
	}

	private Integer LTL = 0;

	/** Returns the number of level boundaries crossed to reach this result. */
	public Integer getLevelsCrossed() {
		return LTL;
	}

	/** Sets the number of level boundaries crossed. */
	public void setLevelsCrossed(Integer levelsCrossed) {
		LTL = levelsCrossed;
	}

	@Override
	public String toString() {
		if (valid) {
			return MessageFormat.format("src:[{0}] key:[{1}] value:[{2}]", ctx, key, result);
		} else {
			return MessageFormat.format("src:[{0}] key:[{1}] INVALID", ctx, key);
		}
	}
}
