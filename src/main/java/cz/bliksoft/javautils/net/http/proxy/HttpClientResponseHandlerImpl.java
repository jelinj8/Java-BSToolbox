package cz.bliksoft.javautils.net.http.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;

import com.sun.net.httpserver.HttpExchange;

import cz.bliksoft.javautils.net.http.BasicHTTPHandler;
import cz.bliksoft.javautils.net.http.HTTPErrorCodes;

import org.apache.hc.core5.http.io.HttpClientResponseHandler;

public class HttpClientResponseHandlerImpl implements HttpClientResponseHandler<HttpExchange> {
	HttpExchange httpExchange;

	public HttpClientResponseHandlerImpl(HttpExchange httpExchange) {
		this.httpExchange = httpExchange;
	}

	Boolean receiving = false;
	Boolean completed = false;

	@Override
	public HttpExchange handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
		synchronized (this) {
			receiving = true;
			notifyAll();
		}

		Iterator<Header> it = response.headerIterator();

		Long dataLength = 0l;
		// copy response headers, except length, to be set by response handler
		while (it.hasNext()) {
			Header h = it.next();

			if (BasicHTTPHandler.HEADER_CONTENT_LENGTH.toLowerCase().equals(h.getName().toLowerCase())) {
				dataLength = Long.parseLong(h.getValue());
			} else {
				List<String> hVal = httpExchange.getResponseHeaders().get(h.getName());
				if (hVal == null) {
					hVal = new ArrayList<>(1);
					hVal.add(h.getValue());
					httpExchange.getResponseHeaders().put(h.getName(), hVal);
				} else {
					hVal.add(h.getValue());
				}
			}
		}

		// copy body
		try (OutputStream os = httpExchange.getResponseBody()) {
			try (InputStream is = response.getEntity().getContent()) {
				httpExchange.sendResponseHeaders(HTTPErrorCodes.OK.getValue(), dataLength);
				IOUtils.copy(is, os);
			}
		}

		synchronized (this) {
			completed = true;
			notifyAll();
		}

		return httpExchange;
	}

	public boolean isReceiving() {
		synchronized (this) {
			return receiving;
		}
	}

	public boolean isComplete() {
		synchronized (this) {
			return completed;
		}
	}
}
