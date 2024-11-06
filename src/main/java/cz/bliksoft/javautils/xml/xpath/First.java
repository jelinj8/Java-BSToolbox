package cz.bliksoft.javautils.xml.xpath;

import java.util.List;

import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

import org.w3c.dom.NodeList;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.xml.XmlUtils;

/**
 * returns text representation of first available input (first found node or text in first non-empty argument)
 */
public class First implements XPathFunction {

	@SuppressWarnings("rawtypes")
	@Override
	public Object evaluate(List args) throws XPathFunctionException {
		try {
			for (Object o : args) {
				if (o instanceof NodeList) {
					NodeList nl = (NodeList) o;
					if (nl.getLength() == 0)
						continue;
				}

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
