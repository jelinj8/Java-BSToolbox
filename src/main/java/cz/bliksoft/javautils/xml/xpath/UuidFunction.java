package cz.bliksoft.javautils.xml.xpath;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

import cz.bliksoft.javautils.uuid.RandomUUIDCreator;

/**
 * uuid() - generate random uuid
 */
public class UuidFunction implements XPathFunction {

	Logger log = Logger.getLogger(UuidFunction.class.getName());

	@SuppressWarnings("rawtypes")
	@Override
	public Object evaluate(List args) throws XPathFunctionException {
		return RandomUUIDCreator.getRandomUuid().toString();
	}
}
