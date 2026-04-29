package cz.bliksoft.javautils.context;

import java.text.MessageFormat;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * třída pro uchovávání výsledku vyhledávání v kontextu
 *
 */
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

	/**
	 * vrátí výsledek vyhledávání
	 *
	 * @return
	 */
	public Object getResult() {
		if (!isValid()) {
			// throw new InvalidObjectException("Empty value!");
			log.log(Level.ERROR, "Reading invalid value!"); //$NON-NLS-1$
		}
		return result;
	}

	/**
	 * obsahuje stav platnosti výsledku
	 */
	Boolean valid;

	/**
	 * vrací klíč, pro který bylo vyhledávání provedeno
	 *
	 * @return
	 */
	public Object getKey() {
		return key;
	}

	/**
	 * vrací platnost výsledku
	 *
	 * @return
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * factory na neplatné výsledky (náhrada null)
	 *
	 * @param key
	 * @return
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

	/**
	 * sestaví nový výsledek pro předání
	 *
	 * @param key
	 * @param value
	 */
	public ContextSearchResult(Context _ctx, Object key, Object value) {
		this.valid = true;
		this.result = value;
		this.key = key;
		this.ctx = _ctx;
	}

	private Integer LTL = 0;

	public Integer getLevelsCrossed() {
		return LTL;
	}

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
