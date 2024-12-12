package cz.bliksoft.javautils.net.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler chainer (e.g. to preprocess context/session variables for another
 * handler). Calls handlers in defined order until one of them returns "true" as
 * processing result (meaning that it returned response to client).
 */
public class HTTPHandlerChain extends BasicHTTPHandler {

	List<BasicHTTPHandler> handlers;

	public HTTPHandlerChain(BasicHTTPHandler... handlers) {
		this.handlers = new ArrayList<>(handlers.length);
		for (int i = 0; i < handlers.length; i++) {
			BasicHTTPHandler h = handlers[i];
			if (h != null) {
				this.handlers.add(h);
				if (h.isSessionAware() && !isSessionAware())
					makeSessionAware();
				
				for (HttpMethod m : h.getSupportedMethods()) {
					addSupportedMethods(m);
				}
			}
		}
	}

	@Override
	public boolean handle(BSHttpContext context) throws IOException {
		for (BasicHTTPHandler h : handlers) {
			if (h.getSupportedMethods().contains(context.method) && h.handle(context))
				return true;
		}
		return false;
	}

}
