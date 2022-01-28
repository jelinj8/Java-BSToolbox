package cz.bliksoft.javautils.net.http;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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
	public static final String HEADER_CONTENT_DISPOSITION_FILENAME = "attachment; filename=";
	public static final String HEADER_SET_COOKIE = "Set-Cookie";
	public static final String HEADER_COOKIE = "Cookie";

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
		String method = httpExchange.getRequestMethod().toUpperCase();
		URI uri = httpExchange.getRequestURI();
		String path = uri.getPath();
		String query = uri.getQuery();

		handle(httpExchange, path, query, method);
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
			httpExchange.sendResponseHeaders(200, data.length);
			OutputStream os = httpExchange.getResponseBody();
			os.write(data);
			os.close();
		} else {
			httpExchange.sendResponseHeaders(200, 0);
		}
	}

	protected void sendData(HttpExchange httpExchange, byte[] data, String contentType, String fileName)
			throws IOException {
		addCommonHeaders(httpExchange, contentType);

		if (fileName != null) {
			addHeader(httpExchange, HEADER_CONTENT_DISPOSITION,
					HEADER_CONTENT_DISPOSITION_FILENAME + "\"" + fileName + "\"");
		}

		httpExchange.sendResponseHeaders(200, data.length);
		OutputStream os = httpExchange.getResponseBody();
		os.write(data);
		os.close();
	}

	protected void sendERR(HttpExchange httpExchange, String message, Integer code) throws IOException {
		sendERR(httpExchange, message, CONTENT_TYPE_TXT, code);
	}

	protected void sendERR(HttpExchange httpExchange, String message, String contentType, Integer code)
			throws IOException {
		addCommonHeaders(httpExchange, contentType);

		if (message != null) {
			byte[] data = message.getBytes(StandardCharsets.UTF_8);
			httpExchange.sendResponseHeaders((code == null ? 500 : code), data.length);
			OutputStream os = httpExchange.getResponseBody();
			os.write(data);
			os.close();
		} else {
			httpExchange.sendResponseHeaders(500, 0);
		}
	}

	protected void sendClasspathResource(HttpExchange httpExchange, Class<?> loader, String path) {
		try (InputStream is = loader.getResourceAsStream(path)) {
			if (is == null) {
				sendERR(httpExchange, "File not found.", 404);
				return;
			}
			String fileName = FilenameUtils.getName(path);
			sendStream(httpExchange, is, fileName);
		} catch (Exception e) {
			try {
				sendERR(httpExchange, e.getMessage(), 500);
			} catch (IOException e1) {
				log.severe("FAIL of FAIL");
			}
		}
	}

	protected void sendFile(HttpExchange httpExchange, File file) throws IOException {
		if (!file.isFile()) {
			sendERR(httpExchange, "File not found.", 404);
			return;
		}
		try (InputStream is = new FileInputStream(file)) {
			String fileName = FilenameUtils.getName(file.getName());
			sendStream(httpExchange, is, fileName);
		} catch (Exception e) {
			try {
				sendERR(httpExchange, e.getMessage(), 500);
			} catch (IOException e1) {
				log.severe("FAIL of FAIL");
			}
		}
	}

	protected void sendStream(HttpExchange httpExchange, InputStream is, String fileName) throws IOException {
		String extension = FilenameUtils.getExtension(fileName);
		addCommonHeaders(httpExchange, MimeTypes.getMimeType(extension));

		if (fileName != null) {
			addHeader(httpExchange, HEADER_CONTENT_DISPOSITION,
					HEADER_CONTENT_DISPOSITION_FILENAME + "\"" + fileName + "\"");
		}

		httpExchange.sendResponseHeaders(200, 0);
		OutputStream os = httpExchange.getResponseBody();
		IOUtils.copy(is, os);
		os.close();
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

	public void addCookie(HttpExchange httpExchange, String name, String value) {
		addHeader(httpExchange, HEADER_SET_COOKIE, name + "=" + value + "; SameSite=Lax");
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

//	public String testRequest(String path, Map<String, List<Optional<String>>> parameters) {
//		log.info("test");
//		StringBuilder testResult = new StringBuilder();
//		testResult.append("OK\n" + path + "\nParameters:\n-----\n");
//		for (Entry<String, List<Optional<String>>> p : parameters.entrySet()) {
//			String val = p.getKey() + "=";
//			for (Optional<String> v : p.getValue()) {
//				if (!v.isPresent())
//					val += "<EMPTY>,";
//				else
//					val += "'" + v.get() + "',";
//			}
//			testResult.append(val);
//			testResult.append("\n");
//		}
//		return testResult.toString();
//	}
}