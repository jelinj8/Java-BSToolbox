package cz.bliksoft.javautils.xml.xpath;

import java.util.List;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

/**
 * {@code now()} - the current time as epoch milliseconds ({@link Long}).
 *
 * <p>
 * Returns the raw {@code Long} so it can be consumed directly by
 * {@link FormatDateFunction} (which accepts a numeric epoch-millis value), e.g.
 * {@code formatDate(now(), 'yyyy-MM-dd HH:mm:ss')}.
 */
public class NowFunction implements XPathFunction {

	@SuppressWarnings("rawtypes")
	@Override
	public Object evaluate(List args) throws XPathFunctionException {
		return System.currentTimeMillis();
	}
}
