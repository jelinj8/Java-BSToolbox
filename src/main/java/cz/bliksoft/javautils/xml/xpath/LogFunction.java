package cz.bliksoft.javautils.xml.xpath;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.xml.XmlUtils;

/**
 * log(message+return_value) log(message, return_value) log(logger,
 * message|null, return_value)
 */
public class LogFunction implements XPathFunction {

	Logger log = Logger.getLogger(LogFunction.class.getName());

	@SuppressWarnings("rawtypes")
	@Override
	public Object evaluate(List args) throws XPathFunctionException {
		try {
			String arg0 = XmlUtils.getResultText(args.get(0));
			String msg = null;
			if (args.size() > 1) {
				try {
					msg = XmlUtils.getResultText(args.get(1));
				} catch (XPathException e) {
				}

				if (!StringUtils.hasLength(msg) && args.size() > 2) {
					msg = XmlUtils.getResultText(args.get(2));
				}
			}

			if (args.size() < 3)
				log.info(arg0);
			else
				Logger.getLogger(arg0).info(msg);

			if (args.size() == 1)
				return args.get(0);
			else if (args.size() == 2)
				return args.get(1);
			else if (args.size() == 3)
				return args.get(2);
		} catch (XPathException e) {
		}
		throw new XPathFunctionException("Failed to \"log\" from XPath with " + args.size()
				+ " args. Supported call variants: log(message+return_value), log(message, return_value), log(logger, message|null, return_value)");
	}
}
