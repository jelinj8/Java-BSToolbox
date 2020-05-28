package cz.bliksoft.javautils.ws;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class DelayHandler implements SOAPHandler<SOAPMessageContext> {

	public DelayHandler(long delay) {
		_delay = 20000l;
	}

	private long _delay;

	@Override
	public boolean handleMessage(final SOAPMessageContext msgCtx) {
		// Indicator telling us which direction this message is going in
		final Boolean outInd = (Boolean) msgCtx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

		// Handler must only add security headers to outbound messages
		// if (outInd.booleanValue()) {
		try {
			Thread.sleep(_delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// }
		return true;
	}

	// Other required methods on interface need no guts

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
