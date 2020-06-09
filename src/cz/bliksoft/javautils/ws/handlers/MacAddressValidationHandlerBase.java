package cz.bliksoft.javautils.ws.handlers;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.soap.Node;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;

public abstract class MacAddressValidationHandlerBase implements SOAPHandler<SOAPMessageContext> {

	@Override
	public boolean handleMessage(SOAPMessageContext context) {
		Boolean isRequest = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

		// for response message only, true for outbound messages, false for inbound
		if (!isRequest) {

			try {
				SOAPMessage soapMsg = context.getMessage();
				SOAPEnvelope soapEnv = soapMsg.getSOAPPart().getEnvelope();
				SOAPHeader soapHeader = soapEnv.getHeader();

				// if no header, add one
				if (soapHeader == null) {
					soapHeader = soapEnv.addHeader();
					// throw exception
					generateSOAPErrMessage(soapMsg, "No SOAP header.");
				}

				// Get client mac address from SOAP header
				Iterator it = soapHeader.extractHeaderElements(SOAPConstants.URI_SOAP_ACTOR_NEXT);

				// if no header block for next actor found? throw exception
				if (it == null || !it.hasNext()) {
					generateSOAPErrMessage(soapMsg, "No header block for next actor.");
				}

				// if no mac address found? throw exception
				Node macNode = (Node) it.next();
				String macValue = (macNode == null) ? null : macNode.getValue();

				if (macValue == null) {
					generateSOAPErrMessage(soapMsg, "No mac address in header block.");
				}

				// if mac address is not match, throw exception
				if (!canAccess(macValue)) {
					generateSOAPErrMessage(soapMsg, "Invalid mac address, access is denied.");
				}

			} catch (SOAPException e) {
				System.err.println(e);
			}

		}

		// continue other handler chain
		return true;
	}

	public abstract boolean canAccess(String mac);

	@Override
	public boolean handleFault(SOAPMessageContext context) {
		return true;
	}

	@Override
	public void close(MessageContext context) {
	}

	@Override
	public Set<QName> getHeaders() {
		return null;
	}

	private void generateSOAPErrMessage(SOAPMessage msg, String reason) {
		try {
			SOAPBody soapBody = msg.getSOAPPart().getEnvelope().getBody();
			SOAPFault soapFault = soapBody.addFault();
			soapFault.setFaultString(reason);
			throw new SOAPFaultException(soapFault);
		} catch (SOAPException e) {
		}
	}

}