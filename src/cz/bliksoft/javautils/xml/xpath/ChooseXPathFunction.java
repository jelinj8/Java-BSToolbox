package cz.bliksoft.javautils.xml.xpath;

import java.util.List;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

public class ChooseXPathFunction implements XPathFunction {
	@SuppressWarnings("rawtypes")
	@Override
	public Object evaluate(List args) throws XPathFunctionException {
		if (args.size() != 3)
			throw new XPathFunctionException(
					"Expecting 3 arguments - boolean, result when tTRUE and result when FALSE. Received " + args.size()
							+ " arguments.");
		boolean val;
		try {
			val = (boolean) args.get(0);
		} catch (Exception e) {
			throw new XPathFunctionException("Failed to cast first argument to boolean.");
		}
		if (val)
			return args.get(1);
		else
			return args.get(2);
	}
}
