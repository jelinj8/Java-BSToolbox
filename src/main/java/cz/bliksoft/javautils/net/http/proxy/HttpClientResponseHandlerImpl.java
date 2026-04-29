package cz.bliksoft.javautils.net.http.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import com.sun.net.httpserver.HttpExchange;

import cz.bliksoft.javautils.net.http.BasicHTTPHandler;

@SuppressWarnings("restriction")
public class HttpClientResponseHandlerImpl implements HttpClientResponseHandler<HttpExchange> {
	HttpExchange httpExchange;

	// hop-by-hop headers must not be forwarded to the client
	private static final Set<String> HOP_BY_HOP_HEADERS = Collections
			.unmodifiableSet(new HashSet<>(Arrays.asList("connection", "keep-alive", "transfer-encoding", "te",
					"trailer", "upgrade", "proxy-authorization", "proxy-authenticate")));

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
		// copy response headers, except hop-by-hop and Content-Length (set by
		// sendResponseHeaders)
		while (it.hasNext()) {
			Header h = it.next();
			String nameLower = h.getName().toLowerCase();

			if (BasicHTTPHandler.HEADER_CONTENT_LENGTH.toLowerCase().equals(nameLower)) {
				dataLength = Long.parseLong(h.getValue());
			} else if (!HOP_BY_HOP_HEADERS.contains(nameLower)) {
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
				httpExchange.sendResponseHeaders(response.getCode(), dataLength);
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
