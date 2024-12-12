package cz.bliksoft.javautils.net.http.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import cz.bliksoft.javautils.net.http.BSHttpContext;
import cz.bliksoft.javautils.net.http.BasicHTTPHandler;
import cz.bliksoft.javautils.net.http.HTTPErrorCodes;

public class ProxyHandler extends BasicHTTPHandler {
	private static final Logger log = Logger.getLogger(ProxyHandler.class.getPackage().getName());

	private long respondTimeout;
	private long receiveTimeout;

	/**
	 * create proxy with default timeouts (60s/10s respond/receive)
	 */
	public ProxyHandler() {
		this(60000, 10000);
	}

	/**
	 * Create proxy with defined max. response/receive times
	 * 
	 * @param respondTimeout
	 *                           ms until the target should start sending response
	 * @param receiveTimeout
	 *                           ms until the whole message is received
	 */
	public ProxyHandler(long respondTimeout, long receiveTimeout) {
		this.respondTimeout = respondTimeout;
		this.receiveTimeout = receiveTimeout;
		addSupportedMethods(HttpMethod.GET, HttpMethod.POST, HttpMethod.DELETE, HttpMethod.PUT);
	}

	private Function<URI, URI> rewriteUrl = null;

	/**
	 * function for URI transformation (overrides path prefix)
	 * 
	 * @param function
	 */
	public void setRewriteUri(Function<URI, URI> function) {
		rewriteUrl = function;
	}

	public String getScheme() {
		return scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public String getAuthority() {
		return authority;
	}

	public void setAuthority(String authority) {
		this.authority = authority;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getFragment() {
		return fragment;
	}

	public void setFragment(String fragment) {
		this.fragment = fragment;
	}

	private String scheme = null;
	private String authority = null;
	private String path = null;
	private String query = null;
	private String fragment = null;

	//	private static class ContentLengthHeaderRemover implements HttpRequestInterceptor {
	//		@Override
	//		public void process(HttpRequest request, EntityDetails entity, HttpContext context)
	//				throws HttpException, IOException {
	//			request.removeHeaders(BasicHTTPHandler.HEADER_CONTENT_LENGTH);
	//		}
	//	}

	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		HttpEntity entity = null;
		try {
			HttpMethod method = HttpMethod.fromString(httpExchange.getRequestMethod());
			URI uri = httpExchange.getRequestURI();

			HttpUriRequestBase req = null;
			switch (method) {
			case GET:
				req = new HttpGet(uri);
				break;
			case POST:
				req = new HttpPost(uri);
				break;
			case DELETE:
				req = new HttpDelete(uri);
				break;
			case PUT:
				req = new HttpPut(uri);
				break;
			default:
				sendERR(httpExchange, "Unsupported method: " + method,
						HTTPErrorCodes.CLIENT_UNSUPPORTED_MEDIA_TYPE.getValue());
				return;
			}

			URI newUri = null;

			if (rewriteUrl != null)
				newUri = rewriteUrl.apply(uri);
			else {
				String newPath = uri.getPath();

				if (getPathPrefix() != null)
					newPath = newPath.replaceFirst(getPathPrefix(), "");

				newUri = new URI(scheme != null ? scheme : (uri.getScheme() == null ? "http" : uri.getScheme()),
						(authority != null ? authority : uri.getAuthority()), (path != null ? path : newPath),
						(query != null ? query : uri.getQuery()), (fragment != null ? fragment : uri.getFragment()));
			}
			final String authority = newUri.getAuthority();

			log.info(MessageFormat.format("Proxy {0} to {1}", uri, newUri.toString()));

			req.setUri(newUri);
			final HttpUriRequestBase finalReq = req;

			Headers headers = httpExchange.getRequestHeaders();

			String contentType = null;

			for (Entry<String, List<String>> h : headers.entrySet()) {

				// remove header
				if (HEADER_CONTENT_LENGTH.toLowerCase().equals(h.getKey().toLowerCase())) {
					continue;
				}

				// save mime for later usage
				if (HEADER_CONTENT_TYPE.toLowerCase().equals(h.getKey().toLowerCase())) {
					contentType = h.getValue().get(0);
				}
				for (String value : h.getValue()) {
					if ("Host".equals(h.getKey())) {
						finalReq.addHeader(h.getKey(), authority);
					} else {
						finalReq.addHeader(h.getKey(), value);
					}
				}
			}

			InputStream is = httpExchange.getRequestBody();
			if (is != null) {
				ContentType ct = (contentType != null ? ContentType.create(contentType) : null);
				entity = new InputStreamEntity(is, ct);
				req.setEntity(entity);
			}

			final HttpClientResponseHandlerImpl rh = new HttpClientResponseHandlerImpl(httpExchange);

			try (CloseableHttpClient client = HttpClients.createDefault()
			//					HttpClients.custom().addRequestInterceptorFirst(new ContentLengthHeaderRemover()).build() // to fix 500: Content-Length already sent
			) {
				client.execute(req, rh);
				synchronized (rh) {
					wait(respondTimeout);
				}

				if (rh.isReceiving()) {
					synchronized (rh) {
						wait(receiveTimeout);
					}
				} else {
					sendERR(httpExchange, "Proxy sending timeout", HTTPErrorCodes.SERVER_GATEWAY_TIMEOUT.getValue());
				}
			}
		} catch (Exception e) {
			sendERR(httpExchange, e.getMessage(), HTTPErrorCodes.SERVER_INTERNAL_SERVER_ERROR.getValue());
		} finally {
			if (entity != null)
				entity.close();
		}
	}

	@Override
	public boolean handle(BSHttpContext context) throws IOException {
		//		handle(context.httpExchange);
		//		return true;
		throw new RuntimeException("Unused method - should not be called!");
		// this method is not used in proxy handler
	}
}
