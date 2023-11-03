package cz.bliksoft.javautils.xml.xpath;

import java.util.List;

import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.xml.XmlUtils;

public class First implements XPathFunction {

	@SuppressWarnings("rawtypes")
	@Override
	public Object evaluate(List args) throws XPathFunctionException {
		try {
			for (Object o : args) {
				String s = XmlUtils.getResultText(o);
				if (StringUtils.hasLength(s))
					return s;
			}
			throw new XPathFunctionException("Failed to get first nonempty value (all empty)");
		} catch (XPathException e) {
			throw new XPathFunctionException(e);
		}
	}
}
