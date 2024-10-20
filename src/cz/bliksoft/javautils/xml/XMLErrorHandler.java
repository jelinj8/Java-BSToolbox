package cz.bliksoft.javautils.xml;

import java.util.logging.Logger;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLErrorHandler implements ErrorHandler {
	private static Logger log = Logger.getLogger(XMLErrorHandler.class.getName());

	@Override
	public void warning(SAXParseException exception) throws SAXException {
		log.warning("SAXException (warning): " + exception.getMessage());
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		log.severe("SAXException (fatalError): " + exception.getMessage());
	}

	@Override
	public void error(SAXParseException exception) throws SAXException {
		log.severe("SAXException (error): " + exception.getMessage());
	}

}
