package cz.bliksoft.javautils.xml.xpath;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

import cz.bliksoft.javautils.xml.XmlUtils;

/**
 * {@code sprintf(format, args...)} - formats via
 * {@link String#format(String, Object...)}. All arguments are passed as
 * strings, so use {@code %s} conversions in the format.
 */
public class SprintfFunction implements XPathFunction {

	@Override
	public Object evaluate(@SuppressWarnings("rawtypes") List args) throws XPathFunctionException {
		if (args.isEmpty())
			throw new XPathFunctionException("sprintf requires at least 1 argument: format [, args...]");
		try {
			String format = XmlUtils.getResultText(args.get(0));
			List<Object> fArgs = new ArrayList<>();
			for (int i = 1; i < args.size(); i++)
				fArgs.add(XmlUtils.getResultText(args.get(i)));
			return String.format(format, fArgs.toArray());
		} catch (Exception e) {
			throw new XPathFunctionException("sprintf failed: " + e.getMessage());
		}
	}
}
