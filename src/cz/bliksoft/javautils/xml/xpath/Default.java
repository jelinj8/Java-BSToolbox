package cz.bliksoft.javautils.xml.xpath;

import java.util.List;

import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

import cz.bliksoft.javautils.xml.XmlUtils;

public class Default implements XPathFunction {

	@SuppressWarnings("rawtypes")
	@Override
	public Object evaluate(List args) throws XPathFunctionException {
		try {
			String s = XmlUtils.getResultText(args.get(0));
			return s; // args.get(0);
		} catch (XPathException e) {
			return args.get(1);
		}
	}
}
