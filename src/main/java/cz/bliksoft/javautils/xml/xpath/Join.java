package cz.bliksoft.javautils.xml.xpath;

import java.util.List;

import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.xml.XmlUtils;

/**
 * returns non-empty text values joined by separator (first arg)
 */
public class Join implements XPathFunction {

	@SuppressWarnings("rawtypes")
	@Override
	public Object evaluate(List args) throws XPathFunctionException {
		if (args.size() < 3)
			throw new XPathFunctionException("Minimal signature is join(separator, val1, val2[, val3...]");

		try {
			String separator = XmlUtils.getResultText(args.get(0));
			StringBuilder sb = new StringBuilder();

			boolean firstValue = true;
			int index = 1;
			while (true) {
				try {
					String val = XmlUtils.getResultText(args.get(index));
					if (StringUtils.hasLength(val)) {
						if (firstValue)
							firstValue = false;
						else
							sb.append(separator);
						
						sb.append(val);
					}
				} catch (XPathException e) {
					// empty node list = no value, skip
				}

				index++;
				if (index == args.size())
					break;
			}

			return sb.toString();
		} catch (XPathException e) {
			return args.get(1);
		}
	}
}
