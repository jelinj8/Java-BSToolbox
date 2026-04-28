package cz.bliksoft.javautils.ws.handlers;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

/**
 * delay processing of SOAP message in one/both directions
 * 
 * @author jelinj8
 *
 */
public class DelayHandler implements SOAPHandler<SOAPMessageContext> {

	public enum MessageDirection {
		IN, OUT, BOTH;
	};

	private MessageDirection direction;
	private long _delay;

	public DelayHandler(long delay, MessageDirection direction) {
		this._delay = delay;
		this.direction = direction;
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public boolean handleMessage(final SOAPMessageContext msgCtx) {
		if (direction != MessageDirection.BOTH) {
			final Boolean outInd = (Boolean) msgCtx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

			switch (direction) {
			case IN:
				if (outInd)
					return true;
			case OUT:
				if (!outInd)
					return true;
			}
		}

		try {
			Thread.sleep(_delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
