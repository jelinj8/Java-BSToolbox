package cz.bliksoft.javautils.xml.xpath;

import java.util.List;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

public class ChooseXPathFunction implements XPathFunction {
	@SuppressWarnings("rawtypes")
	@Override
	public Object evaluate(List args) throws XPathFunctionException {
		boolean val;
		val = (boolean) args.get(0);
		if (val)
			return args.get(1);
		else
			return args.get(2);
	}
}
