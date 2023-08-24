package cz.bliksoft.javautils.ws;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.ws.Binding;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

public class Binder {
	public static void bindService(BindingProvider bindingProvider, String endpointAddress) {
		bindService(bindingProvider, null, endpointAddress);
	}

	public static void bindService(BindingProvider bindingProvider, SOAPHandler<SOAPMessageContext> sh,
			String endpointAddress) {
		bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
		if (sh != null) {
			addHandler(bindingProvider, sh);
		}
	}

	@SuppressWarnings("rawtypes")
	public static void addHandler(BindingProvider bindingProvider, SOAPHandler<SOAPMessageContext> sh) {
		Binding binding = bindingProvider.getBinding();
		if (sh != null) {
			List<Handler> handlerList = binding.getHandlerChain();
			if (handlerList == null)
				handlerList = new ArrayList<Handler>();

			handlerList.add(sh);
			binding.setHandlerChain(handlerList);
		}
	}

	@SuppressWarnings("rawtypes")
	public static void addHandler(Binding binding, SOAPHandler<SOAPMessageContext> sh) {
		if (sh != null) {
			List<Handler> handlerList = binding.getHandlerChain();
			if (handlerList == null)
				handlerList = new ArrayList<Handler>();

			handlerList.add(sh);
			binding.setHandlerChain(handlerList);
		}
	}
}
