package cz.bliksoft.javautils.xml.xpath;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

import cz.bliksoft.javautils.xml.XmlUtils;

public class FormatXPathFunction implements XPathFunction {
	@Override
	public Object evaluate(@SuppressWarnings("rawtypes") List args) throws XPathFunctionException {
		String format;
		try {
			format = XmlUtils.getResultText(args.get(0));
		} catch (XPathException e) {
			throw new XPathFunctionException("Failed to get format string: " + e.getMessage());
		}

		List<Object> fArgs = new ArrayList<>();
		for (int i = 1; i < args.size(); i++) {
			try {
				fArgs.add(XmlUtils.getResultText(args.get(i)));
			} catch (Exception e) {
				throw new XPathFunctionException(
						MessageFormat.format("Failed to get format arg {0}:{1}", i, e.getMessage()));
			}
		}

		return MessageFormat.format(format, fArgs.toArray());
	}
}
