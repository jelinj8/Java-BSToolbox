package cz.bliksoft.javautils.net.http.proxy;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.function.Function;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import cz.bliksoft.javautils.net.http.BSHttpContext;
import cz.bliksoft.javautils.net.http.BasicHTTPHandler;
import cz.bliksoft.javautils.net.http.HTTPErrorCodes;

@SuppressWarnings("restriction")
public class ProxyHandler extends BasicHTTPHandler {
	private static final Logger log = Logger.getLogger(ProxyHandler.class.getPackage().getName());

	private long respondTimeout;
	private long receiveTimeout;

	public ProxyHandler(long respondTimeout, long receiveTimeout) {
		this.respondTimeout = respondTimeout;
		this.receiveTimeout = receiveTimeout;
		addSupportedMethods(HttpMethod.GET, HttpMethod.POST, HttpMethod.DELETE, HttpMethod.PUT);
	}

	private Function<URI, URI> rewriteUrl = null;

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

	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
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
				newUri = new URI(scheme != null ? scheme : (uri.getScheme() == null ? "http" : uri.getScheme()),
						(authority != null ? authority : uri.getAuthority()), (path != null ? path : uri.getPath()),
						(query != null ? query : uri.getQuery()), (fragment != null ? fragment : uri.getFragment()));
			}
			final String authority = newUri.getAuthority();

			log.info(MessageFormat.format("Proxy {0} to {1}", uri, newUri.toString()));

			req.setUri(newUri);
			final HttpUriRequestBase finalReq = req;

			Headers headers = httpExchange.getRequestHeaders();

			headers.forEach((h, i) -> {
				for (String value : i) {
					if ("Host".equals(h)) {
						finalReq.addHeader(h, authority);
					} else {
						finalReq.addHeader(h, value);
					}
				}
			});

			final HttpClientResponseHandlerImpl rh = new HttpClientResponseHandlerImpl(httpExchange);

			try (CloseableHttpClient client = HttpClients.createDefault()) {
				client.execute(req, rh);

				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						while (true) {
							if (rh.isReceiving())
								break;
							try {
								Thread.sleep(20);
							} catch (InterruptedException e) {
							}
						}
					}
				});
				
				t.setName("ProxyHTTPClientWaitThread");

				t.start();
				// wait for response from target
				t.join(respondTimeout);

				if (rh.isReceiving()) {
					// wait for data to copy
					Thread t2 = new Thread(new Runnable() {
						@Override
						public void run() {
							while (true) {
								while (true) {
									if (rh.isComplete())
										break;
									try {
										Thread.sleep(10);
									} catch (InterruptedException e) {
										log.severe("Failed to complete response in time!");
									}
								}
							}
						}
					});
					t2.setName("ProxyHTTPClientResponseForwardThread");
					t2.start();
					t2.join(receiveTimeout);
				} else {
					sendERR(httpExchange, "Proxy sending timeout", HTTPErrorCodes.SERVER_GATEWAY_TIMEOUT.getValue());
				}
			}
		} catch (Exception e) {
			sendERR(httpExchange, e.getMessage(), HTTPErrorCodes.SERVER_INTERNAL_SERVER_ERROR.getValue());
		}
	}

	@Override
	public void handle(BSHttpContext context) throws IOException {
		throw new RuntimeException("Unused method");
		// this method is not used in proxy handler
	}
}
