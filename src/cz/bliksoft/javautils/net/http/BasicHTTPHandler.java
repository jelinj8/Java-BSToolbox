package cz.bliksoft.javautils.net.http;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * HttpHandler to register with a path in BSHttpServer.
 *
 */
@SuppressWarnings("restriction")
public abstract class BasicHTTPHandler implements HttpHandler, Closeable {
	private Logger log = Logger.getLogger(BasicHTTPHandler.class.getName());

	public static final String CONTENT_TYPE_TXT = "text/plain; charset=utf-8";
	public static final String CONTENT_TYPE_HTML = "text/html; charset=utf-8";
	public static final String CONTENT_TYPE_XML = "text/xml; charset=utf-8";
	public static final String CONTENT_TYPE_DATA = "application/octet-stream";
	public static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	public static final String HEADER_ORIGIN = "Access-Control-Allow-Origin";
	public static final String HEADER_ORIGIN_ANY = "*";
	public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
	public static final String HEADER_CONTENT_DISPOSITION_ATTACHMENT = "attachment; filename=";
	public static final String HEADER_CONTENT_DISPOSITION_INLINE = "inline; filename=";
	public static final String HEADER_SET_COOKIE = "Set-Cookie";
	public static final String HEADER_COOKIE = "Cookie";

	/**
	 * (new Date()).toUTCString() http timestamp like "Wed, 21 Oct 2015 07:28:00
	 * GMT"
	 */
	public static final String ATTRIB_EXPIRES = "Expires=";

	/**
	 * validity in seconds
	 */
	public static final String ATTRIB_MAX_AGE = "Max-Age=";

	/**
	 * e.g. example.com
	 */
	public static final String ATTRIB_DOMAIN = "Domain=";

	/**
	 * e.g. /ui/
	 */
	public static final String ATTRIB_PATH = "Path=";
	public static final String ATTRIB_SECURE = "Secure";
	public static final String ATTRIB_HTTP_ONLY = "HttpOnly";
	public static final String ATTRIB_SAME_SITE_LAX = "SameSite=Lax";
	public static final String ATTRIB_SAME_SITE_STRICT = "SameSite=Strict";
	public static final String ATTRIB_SAME_SITE_NONE_SECURE = "SameSite=None; Secure";

	/**
	 * <pre>
	 * switch (method) {
	 * case "GET":
	 * case "POST":
	 * 	break;
	 * default:
	 * 	throw new IOException("Unsupported method: " + method);
	 * }
	 *
	 * switch (path) {
	 * case "/test":
	 * 	sendOK(t, testRequest(path, params));
	 * 	break;
	 * default:
	 * 	sendERR(t, "Unknown path: " + path);
	 * }
	 * </pre>
	 * 
	 * @param httpExchange
	 * @param path         URI, starts with '/'
	 * @param params
	 * @throws IOException
	 */
	public abstract void handle(HttpExchange httpExchange, String path, String query, String method) throws IOException;

	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		try {
			String method = httpExchange.getRequestMethod().toUpperCase();
			URI uri = httpExchange.getRequestURI();
			String path = uri.getPath();
			String query = uri.getQuery();
			handle(httpExchange, path, query, method);
		} catch (Exception e) {
			sendERR(httpExchange, e.getMessage(), HTTPErrorCodes.SERVER_INTERNAL_SERVER_ERROR.getValue());
		}
	}

	protected String getRequestBody(HttpExchange httpExchange) throws IOException {
		return IOUtils.toString(httpExchange.getRequestBody());
	}

	protected Map<String, List<Optional<String>>> getPostParams(HttpExchange httpExchange) throws IOException {
		return URIParameterDecode.splitQuery(getRequestBody(httpExchange));
	}

	protected Map<String, List<Optional<String>>> getGetParams(String query) throws IOException {
		return URIParameterDecode.splitQuery(query);
	}

	protected void sendOK(HttpExchange httpExchange) throws IOException {
		sendOK(httpExchange, "OK");
	}

	protected void sendOK(HttpExchange httpExchange, String message) throws IOException {
		sendOK(httpExchange, message, CONTENT_TYPE_TXT);
	}

	protected void sendOK(HttpExchange httpExchange, String message, String contentType) throws IOException {
		addCommonHeaders(httpExchange, contentType);

		if (message != null) {
			byte[] data = message.getBytes(StandardCharsets.UTF_8);
			httpExchange.sendResponseHeaders(HTTPErrorCodes.OK.getValue(), data.length);
			OutputStream os = httpExchange.getResponseBody();
			os.write(data);
			os.close();
		} else {
			httpExchange.sendResponseHeaders(HTTPErrorCodes.OK.getValue(), 0);
		}
	}

	protected void sendOKDocument(HttpExchange httpExchange, File document) throws IOException {
		addCommonHeaders(httpExchange, MimeTypes.getMimeType(FilenameUtils.getExtension(document.getName())));
		addHeader(httpExchange, HEADER_CONTENT_DISPOSITION,
				HEADER_CONTENT_DISPOSITION_INLINE + "\"" + document.getName() + "\"");
		httpExchange.sendResponseHeaders(HTTPErrorCodes.OK.getValue(), document.length());
		try (FileInputStream is = new FileInputStream(document)) {
			OutputStream os = httpExchange.getResponseBody();
			IOUtils.copy(is, os);
			os.close();
		}
	}

	protected void sendOKResource(HttpExchange httpExchange, Class<?> loader, String path) throws IOException {
		try (InputStream is = loader.getResourceAsStream(path)) {
			if (is == null) {
				sendERR(httpExchange, "File not found.", HTTPErrorCodes.CLIENT_NOT_FOUND.getValue());
				return;
			}
			String fileName = FilenameUtils.getName(path);
			addHeader(httpExchange, HEADER_CONTENT_DISPOSITION,
					HEADER_CONTENT_DISPOSITION_INLINE + "\"" + fileName + "\"");
			addCommonHeaders(httpExchange, MimeTypes.getMimeType(FilenameUtils.getExtension(fileName)));
			httpExchange.sendResponseHeaders(HTTPErrorCodes.OK.getValue(), 0);

			OutputStream os = httpExchange.getResponseBody();
			IOUtils.copy(is, os);
			os.close();
		}
	}

	protected void sendERR(HttpExchange httpExchange, String message, Integer code) throws IOException {
		sendERR(httpExchange, message, CONTENT_TYPE_TXT, code);
	}

	protected void sendERR(HttpExchange httpExchange, String message, String contentType, Integer code)
			throws IOException {
		addCommonHeaders(httpExchange, contentType);

		if (message != null) {
			byte[] data = message.getBytes(StandardCharsets.UTF_8);
			httpExchange.sendResponseHeaders(
					(code == null ? HTTPErrorCodes.SERVER_INTERNAL_SERVER_ERROR.getValue() : code), data.length);
			OutputStream os = httpExchange.getResponseBody();
			os.write(data);
			os.close();
		} else {
			httpExchange.sendResponseHeaders(HTTPErrorCodes.SERVER_INTERNAL_SERVER_ERROR.getValue(), 0);
		}
	}

	protected void sendData(HttpExchange httpExchange, byte[] data, String contentType, String fileName)
			throws IOException {
		addCommonHeaders(httpExchange, contentType);

		if (fileName != null) {
			addHeader(httpExchange, HEADER_CONTENT_DISPOSITION,
					HEADER_CONTENT_DISPOSITION_ATTACHMENT + "\"" + fileName + "\"");
		}

		httpExchange.sendResponseHeaders(HTTPErrorCodes.OK.getValue(), data.length);
		OutputStream os = httpExchange.getResponseBody();
		os.write(data);
		os.close();
	}

	protected void sendClasspathResource(HttpExchange httpExchange, Class<?> loader, String path) throws IOException {
		try (InputStream is = loader.getResourceAsStream(path)) {
			if (is == null) {
				sendERR(httpExchange, "File not found.", HTTPErrorCodes.CLIENT_NOT_FOUND.getValue());
				return;
			}
			String fileName = FilenameUtils.getName(path);
			addCommonHeaders(httpExchange, MimeTypes.getMimeType(FilenameUtils.getExtension(fileName)));
			addHeader(httpExchange, HEADER_CONTENT_DISPOSITION,
					HEADER_CONTENT_DISPOSITION_ATTACHMENT + "\"" + fileName + "\"");
			httpExchange.sendResponseHeaders(HTTPErrorCodes.OK.getValue(), 0);

			OutputStream os = httpExchange.getResponseBody();
			IOUtils.copy(is, os);
			os.close();
		}
	}

	protected void sendFile(HttpExchange httpExchange, File file) throws IOException {
		if (!file.isFile()) {
			sendERR(httpExchange, "File not found.", HTTPErrorCodes.CLIENT_NOT_FOUND.getValue());
			return;
		}
		addCommonHeaders(httpExchange, MimeTypes.getMimeType(FilenameUtils.getExtension(file.getName())));
		addHeader(httpExchange, HEADER_CONTENT_DISPOSITION,
				HEADER_CONTENT_DISPOSITION_ATTACHMENT + "\"" + file.getName() + "\"");
		httpExchange.sendResponseHeaders(HTTPErrorCodes.OK.getValue(), file.length());

		try (InputStream is = new FileInputStream(file)) {
			OutputStream os = httpExchange.getResponseBody();
			IOUtils.copy(is, os);
			os.close();
		}
	}

	private void addCommonHeaders(HttpExchange httpExchange, String contentType) {
		addHeader(httpExchange, HEADER_CONTENT_TYPE, contentType);
		addHeader(httpExchange, HEADER_ORIGIN, HEADER_ORIGIN_ANY);
	}

	private void addHeader(HttpExchange httpExchange, String header, String value) {
		List<String> hVal = httpExchange.getResponseHeaders().get(header);
		if (hVal == null)
			hVal = new ArrayList<>(1);
		hVal.add(value);
		httpExchange.getResponseHeaders().put(header, hVal);
	}

	public void addCookie(HttpExchange httpExchange, String name, String value, int validity) {
		addHeader(httpExchange, HEADER_SET_COOKIE,
				MessageFormat.format("{0}={1}; SameSite=Lax; Max-Age={2,number,#}", name, value, validity));
	}

	public void addCookie(HttpExchange httpExchange, String name, String value) {
		addHeader(httpExchange, HEADER_SET_COOKIE, MessageFormat.format("{0}={1}; SameSite=Lax", name, value));
	}

	public void addCookie(HttpExchange httpExchange, String name, String value, String attribs) {
		addHeader(httpExchange, HEADER_SET_COOKIE, MessageFormat.format("{0}={1}; {2}", name, value, attribs));
	}

	public void addCookie(HttpExchange httpExchange, Cookie cookie) {
		addHeader(httpExchange, HEADER_SET_COOKIE, cookie.toString());
	}

	public Map<String, String> getCookies(HttpExchange httpExchange) {
		Map<String, String> cookies = new HashMap<>();

		String cookieString = httpExchange.getRequestHeaders().getFirst(HEADER_COOKIE);
		if (cookieString != null) {
			String[] cookiePairs = cookieString.split("; ");
			for (int i = 0; i < cookiePairs.length; i++) {
				String[] cookieValue = cookiePairs[i].split("=");
				cookies.put(cookieValue[0], cookieValue[1]);
			}
		}

		return cookies;
	}

	public void start() {
	}

	@Override
	public void close() {
	}

	public boolean canClose() {
		return true;
	}

	protected boolean checkMandatoryPresent(Map<String, List<Optional<String>>> parameters, String paramName) {
		return getParameterFirstValue(parameters, paramName) == null;
	}

	protected String getParameterFirstValue(Map<String, List<Optional<String>>> parameters, String paramName) {
		List<Optional<String>> vals = parameters.get(paramName);
		if (vals == null)
			return null;
		if (vals.get(0).isPresent())
			return vals.get(0).get();
		else
			return "";
	}

	protected List<Optional<String>> getParameterValues(Map<String, List<Optional<String>>> parameters,
			String paramName) {
		List<Optional<String>> vals = parameters.get(paramName);
		return vals;
	}
}