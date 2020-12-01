package cz.bliksoft.javautils.ws.handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import cz.bliksoft.javautils.logging.LogUtils;
import cz.bliksoft.javautils.ws.Binder;

public class SOAPLogHandler implements SOAPHandler<SOAPMessageContext> {
	private static Logger log = Logger.getLogger(SOAPLogHandler.class.getName());

	private String _name;

	public SOAPLogHandler(String name) {
		this._name = name;
	}
	
	public static void addLogHandler(Object service) {
		Binder.addHandler((BindingProvider) service, new SOAPLogHandler(service.getClass().getSimpleName()));
	}

	public static void addLogHandler(Object service, String serviceName) {
		if (serviceName == null)
			addLogHandler(service);
		else
			Binder.addHandler((BindingProvider) service, new SOAPLogHandler(serviceName));
	}
	
	public static void addLogHandler(Binding binding) {
		Binder.addHandler(binding, new SOAPLogHandler(binding.getClass().getSimpleName()));
	}

	public static void addLogHandler(Binding binding, String serviceName) {
		if (serviceName == null)
			addLogHandler(binding);
		else
			Binder.addHandler(binding, new SOAPLogHandler(serviceName));
	}
	
	@Override
	public boolean handleMessage(final SOAPMessageContext context) {

		// Indicator telling us which direction this message is going in
		final Boolean outInd = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

		File logFile = LogUtils.getFile("SOAP{" + this._name + "}_" + (outInd ? "out" : "in"), "xml");

		if (logFile == null)
			return true;

		try (FileOutputStream fos = new FileOutputStream(logFile)) {
			SOAPMessage message = context.getMessage();

			message.writeTo(fos);
		} catch (Exception e) {
			log.severe("Error logging SOAP{" + this._name + "}_" + (outInd ? "out" : "in") + " message: "
					+ e.getMessage());
		}

		return true;
	}

	// Other required methods on interface need no guts

	@Override
	public boolean handleFault(SOAPMessageContext context) {
		final Boolean outInd = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

		File logFile = LogUtils.getFile("SOAP{" + this._name + "}_" + (outInd ? "out" : "in") + "_FAULT", "xml");

		if (logFile == null)
			return true;

		try (FileOutputStream fos = new FileOutputStream(logFile)) {
			SOAPMessage message = context.getMessage();

			message.writeTo(fos);
		} catch (Exception e) {
			log.severe("Error logging SOAP{" + this._name + "}_" + (outInd ? "out" : "in") + " FAULT message: "
					+ e.getMessage());
		}

		return true;
	}

	@Override
	public void close(MessageContext context) {
	}

	@Override
	public Set<QName> getHeaders() {
		return null;
	}

}
