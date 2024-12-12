package cz.bliksoft.javautils.xml.xpath;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

import cz.bliksoft.javautils.xml.XmlUtils;

public class XPathVarCache implements XPathFunction {

	private static Map<String, Map<String, String>> vals = new HashMap<>();

	private static Object lockObject = new Object();

	public static void createNS(String nsName) {
		synchronized (lockObject) {
			vals.put(nsName, new HashMap<>());
		}
	}

	public static void removeNS(String nsName) {
		synchronized (lockObject) {
			vals.remove(nsName);
		}
	}

	public static void clearNS(String nsName) {
		synchronized (lockObject) {
			vals.get(nsName).clear();
		}
	}

	public static void setVar(String nsName, String name, String value) {
		synchronized (lockObject) {
			vals.get(nsName).put(name, value);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object evaluate(List args) throws XPathFunctionException {
		try {
			String nsKey = XmlUtils.getResultText(args.get(0));
			String varKey = XmlUtils.getResultText(args.get(1));
			synchronized (lockObject) {
				Map<String, String> m = vals.get(nsKey);
				if (m == null)
					throw new XPathFunctionException(
							MessageFormat.format("Undefined variable namespace {0}.{1}", nsKey, varKey));
				return m.get(varKey);
			}
		} catch (XPathException e) {
			throw new XPathFunctionException("Failed to get cached value: " + e.getMessage());
		}
	}
}
