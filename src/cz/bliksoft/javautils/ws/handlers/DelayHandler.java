package cz.bliksoft.javautils.ws.handlers;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

public class DelayHandler implements SOAPHandler<SOAPMessageContext> {

	public DelayHandler(long delay) {
		_delay = 20000l;
	}

	private long _delay;

	@Override
	public boolean handleMessage(final SOAPMessageContext msgCtx) {
		// Indicator telling us which direction this message is going in
//		final Boolean outInd = (Boolean) msgCtx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

		// if (outInd.booleanValue()) {
		try {
			Thread.sleep(_delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// }
		return true;
	}

	@Override
	public boolean handleFault(SOAPMessageContext context) {
		return true;
	}

	@Override
	public void close(MessageContext context) {
	}

	@Override
	public Set<QName> getHeaders() {
		HashSet<QName> headers = new HashSet<QName>();
		return headers;
	}

}
