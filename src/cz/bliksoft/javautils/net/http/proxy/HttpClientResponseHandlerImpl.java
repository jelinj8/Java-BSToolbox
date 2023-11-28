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

import cz.bliksoft.javautils.net.http.HTTPErrorCodes;

import org.apache.hc.core5.http.io.HttpClientResponseHandler;

@SuppressWarnings("restriction")
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

		// copy response headers
		while (it.hasNext()) {
			Header h = it.next();

			List<String> hVal = httpExchange.getResponseHeaders().get(h.getName());
			if (hVal == null) {
				hVal = new ArrayList<>(1);
				hVal.add(h.getValue());
				httpExchange.getResponseHeaders().put(h.getName(), hVal);
			} else {
				hVal.add(h.getValue());
			}
		}
		

		// copy body
		try (OutputStream os = httpExchange.getResponseBody()) {
			try (InputStream is = response.getEntity().getContent()) {
				httpExchange.sendResponseHeaders(HTTPErrorCodes.OK.getValue(), 0);
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
