package cz.bliksoft.javautils.xml.xpath;

import java.text.DecimalFormat;
import java.util.List;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

import cz.bliksoft.javautils.xml.XmlUtils;

/**
 * {@code formatNumber(value, pattern)} - formats a numeric value with a
 * {@link DecimalFormat} pattern. Returns an empty string for a null or empty
 * value.
 */
public class FormatNumberFunction implements XPathFunction {

	@Override
	public Object evaluate(@SuppressWarnings("rawtypes") List args) throws XPathFunctionException {
		if (args.size() < 2)
			throw new XPathFunctionException("formatNumber requires 2 arguments: value, pattern");
		try {
			String value = XmlUtils.getResultText(args.get(0));
			String pattern = XmlUtils.getResultText(args.get(1));
			if (value == null || value.isEmpty())
				return "";
			DecimalFormat fmt = new DecimalFormat(pattern);
			return fmt.format(Double.parseDouble(value));
		} catch (Exception e) {
			throw new XPathFunctionException("formatNumber failed: " + e.getMessage());
		}
	}
}
