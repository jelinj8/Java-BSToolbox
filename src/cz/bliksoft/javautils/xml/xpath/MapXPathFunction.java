package cz.bliksoft.javautils.xml.xpath;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

import cz.bliksoft.javautils.xml.XmlUtils;

public class MapXPathFunction implements XPathFunction {
	@SuppressWarnings("rawtypes")
	@Override
	public Object evaluate(List args) throws XPathFunctionException {
		if (args.size() < 2)
			throw new XPathFunctionException(
					"XPath map: minimal signature: map(inpud, default) or map(input, val1, res1, val2, res2...) or map(input, val1, res1, val2, res2..., default) ");

		String src = null;
		try {
			Iterator i = args.iterator();

			src = XmlUtils.getResultText(i.next());

			String val = null;
			String res = null;
			while (i.hasNext()) {
				val = XmlUtils.getResultText(i.next());
				if (!i.hasNext())
					return val;

				res = XmlUtils.getResultText(i.next());

				if (src.equals(val))
					return res;
			}

		} catch (Exception e) {
			throw new XPathFunctionException("Failed to evaluate mapping: " + e.getMessage());
		}

		throw new XPathFunctionException(MessageFormat.format(
				"XPath map: No value matched ''{0}'' and no default value (with even args count the last one is used as default)",
				src));
	}
}
