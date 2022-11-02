package cz.bliksoft.javautils.xml.xpath;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

public class IfElseIf implements XPathFunction {

	@Override
	public Object evaluate(List args) throws XPathFunctionException {
		if (args.size() < 3)
			throw new XPathFunctionException(
					"XPath ifElseIf: minimal signature: ifElseIf(cond1, result1, elsecond2, result2, ... [, elseresultX])  ");

		String src = null;
		try {
			Iterator i = args.iterator();

			Object val = null;
			Object res = null;
			while (i.hasNext()) {
				val = i.next();
				if (!i.hasNext())
					return val;

				res = i.next();

				if ((boolean) val)
					return res;
			}

		} catch (Exception e) {
			throw new XPathFunctionException("Failed to evaluate conditioning: " + e.getMessage());
		}

		throw new XPathFunctionException(MessageFormat.format(
				"XPath ifElseIf: No condition true and no else value (with odd args count the last one is used as default)",
				src));
	}

}
