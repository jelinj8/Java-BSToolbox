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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * HttpHandler to register with a path in BSHttpServer. multipart POST from
 * https://gist.github.com/JensWalter/0f19780d131d903879a2
 */
@SuppressWarnings("restriction")
public abstract class BasicHTTPHandler implements HttpHandler, Closeable {

	private static Logger log = Logger.getLogger(BasicHTTPHandler.class.getName());

	public static final String CONTENT_TYPE_TXT = "text/plain; charset=utf-8";
	public static final String CONTENT_TYPE_HTML = "text/html; charset=utf-8";
	public static final String CONTENT_TYPE_XML = "text/xml; charset=utf-8";
	public static final String CONTENT_TYPE_DATA = "application/octet-stream";
	public static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	public static final String HEADER_CONTENT_TYPE_URLENCODED = "application/x-www-form-urlencoded";
	public static final String HEADER_CONTENT_TYPE_MULTIPART = "multipart/form-data";
	public static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
	public static final String HEADER_ORIGIN = "Access-Control-Allow-Origin";
	public static final String HEADER_ORIGIN_ANY = "*";
	public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
	public static final String HEADER_CONTENT_DISPOSITION_ATTACHMENT = "attachment; filename=";
	public static final String HEADER_CONTENT_DISPOSITION_INLINE = "inline; filename=";
	public static final String HEADER_SET_COOKIE = "Set-Cookie";
	public static final String HEADER_COOKIE = "Cookie";

	public static final String COOKIE_SESSION_ID = "ExchangeId";
	public static final String COOKIE_PREVIOUS_REQ = "PreviousReq";

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
	 * addSupportedMethods(HttpMethod ... method) or e.g. addSupported*()
	 *
	 * setPathPrefix(String) to be removed from required string
	 * 
	 * setDefaultRequired(String) to replace null or '/' required string
	 * 
	 * makeSessionAware() to load session in context
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
	public abstract void handle(BSHttpContext context) throws IOException;

	public enum HttpMethod {
		GET, POST, PUT, PATCH, DELETE, CREATE, UPDATE, OTHER;

		public static HttpMethod fromString(String methodName) {
			if (methodName == null)
				return OTHER;
			switch (methodName.toUpperCase()) {
			case "GET":
				return GET;
			case "POST":
				return POST;
			case "PUT":
				return PUT;
			case "PATCH":
				return PATCH;
			case "DELETE":
				return DELETE;
			case "CREATE":
				return CREATE;
			case "UPDATE":
				return UPDATE;
			default:
				return OTHER;
			}
		}
	};

	boolean _isSessionAware = false;

	public void makeSessionAware() {
		_isSessionAware = true;
	}

	public boolean isSessionAware() {
		return _isSessionAware;
	}

	private Set<HttpMethod> supportedMethods = new HashSet<>();

	public void addSupportedGET() {
		supportedMethods.add(HttpMethod.GET);
	}

	public void addSupportedPOST() {
		supportedMethods.add(HttpMethod.POST);
	}

	public void addSupportedGETPOST() {
		supportedMethods.add(HttpMethod.GET);
		supportedMethods.add(HttpMethod.POST);
	}

	public void addSupportedMethods(HttpMethod... method) {
		for (int i = 0; i < method.length; i++)
			supportedMethods.add(method[i]);
	}

	public Set<HttpMethod> getSupportedMethods() {
		return supportedMethods;
	}

	private String pathPrefix = null;

	public void setPathPrefix(String prefix) {
		pathPrefix = prefix;
	}

	public String getPathPrefix() {
		return pathPrefix;
	}

	private String defaultRequired = null;

	public void setDefautlRequired(String defaultReq) {
		this.defaultRequired = defaultReq;
	}

	public String getDefaultRequired() {
		return defaultRequired;
	}

	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		try {
			HttpMethod method = HttpMethod.fromString(httpExchange.getRequestMethod());
			URI uri = httpExchange.getRequestURI();
			String path = uri.getPath();
			String query = uri.getQuery();
			
			BSHttpContext context = new BSHttpContext(pathPrefix, httpExchange, path, query, method);

			if (context.requested == null && defaultRequired != null)
				context.requested = defaultRequired;

			if (!supportedMethods.contains(context.method)) {
				sendERR(httpExchange, "Unsupported method: " + method,
						HTTPErrorCodes.CLIENT_UNSUPPORTED_MEDIA_TYPE.getValue());
				throw new IOException("Unsupported method: " + method);
			}

			if (_isSessionAware) {
				context.initSession();
			}

			handle(context);
		} catch (Exception e) {
			sendERR(httpExchange, e.getMessage(), HTTPErrorCodes.SERVER_INTERNAL_SERVER_ERROR.getValue());
		}
	}

	protected static String getRequestBody(HttpExchange httpExchange) throws IOException {
		return IOUtils.toString(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
	}

	protected static boolean isMultipart(HttpExchange httpExchange) {
		Headers headers = httpExchange.getRequestHeaders();
		String contentType = headers.getFirst(HEADER_CONTENT_TYPE);
		return (contentType.startsWith(HEADER_CONTENT_TYPE_MULTIPART));
	}

	protected static Map<String, List<Optional<String>>> getGetParams(String query) throws IOException {
		return URIParameterDecode.splitQuery(query);
	}

	protected static void sendOK(HttpExchange httpExchange) throws IOException {
		sendOK(httpExchange, "OK");
	}

	protected static void sendOK(HttpExchange httpExchange, String message) throws IOException {
		sendOK(httpExchange, message, CONTENT_TYPE_TXT);
	}

	protected static void sendOK(HttpExchange httpExchange, String message, String contentType) throws IOException {
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

	protected static OutputStream getOutputStream(HttpExchange httpExchange) {
		return httpExchange.getResponseBody();
	}

	protected static void sendOKDocument(HttpExchange httpExchange, File document) throws IOException {
		if (!document.exists()) {
			log.severe(MessageFormat.format("File ''{0}'' not found", document.toString()));
			sendERR(httpExchange, "File not found.", HTTPErrorCodes.CLIENT_NOT_FOUND.getValue());
			return;
		}

		try (FileInputStream is = new FileInputStream(document)) {
			addCommonHeaders(httpExchange, MimeTypes.getMimeType(FilenameUtils.getExtension(document.getName())));
			addHeader(httpExchange, HEADER_CONTENT_DISPOSITION,
					HEADER_CONTENT_DISPOSITION_INLINE + "\"" + document.getName() + "\"");
			httpExchange.sendResponseHeaders(HTTPErrorCodes.OK.getValue(), document.length());
			OutputStream os = httpExchange.getResponseBody();
			IOUtils.copy(is, os);
			os.close();
		}
	}

	protected static void sendOKResource(HttpExchange httpExchange, Class<?> loader, String path) throws IOException {
		try (InputStream is = loader.getResourceAsStream(path)) {
			if (is == null) {
				log.severe(MessageFormat.format("Resource {0}:''{1}'' not found", loader.getName(), path));
				sendERR(httpExchange, "Resource not found.", HTTPErrorCodes.CLIENT_NOT_FOUND.getValue());
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

	protected static void sendHeaders(HttpExchange httpExchange, Integer code, Long length) throws IOException {
		httpExchange.sendResponseHeaders((code == null ? HTTPErrorCodes.OK.getValue() : code),
				(length == null ? 0l : length));
	}

	protected static void sendERR(HttpExchange httpExchange, String message, HTTPErrorCodes code) throws IOException {
		sendERR(httpExchange, message, CONTENT_TYPE_TXT, code.getValue());
	}

	protected static void sendERR(HttpExchange httpExchange, String message, Integer code) throws IOException {
		sendERR(httpExchange, message, CONTENT_TYPE_TXT, code);
	}

	protected static void sendERR(HttpExchange httpExchange, String message, String contentType, HTTPErrorCodes code)
			throws IOException {
		sendERR(httpExchange, message, contentType, code.getValue());
	}

	protected static void sendERR(HttpExchange httpExchange, String message, String contentType, Integer code)
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

	protected static void sendData(HttpExchange httpExchange, byte[] data, String contentType, String fileName)
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

	protected static void sendClasspathResource(HttpExchange httpExchange, Class<?> loader, String path)
			throws IOException {
		try (InputStream is = loader.getResourceAsStream(path)) {
			if (is == null) {
				log.severe(MessageFormat.format("Resource {0}:''{1}'' not found", loader.getName(), path));
				sendERR(httpExchange, "Resource not found.", HTTPErrorCodes.CLIENT_NOT_FOUND.getValue());
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

	protected static void sendFile(HttpExchange httpExchange, File file) throws IOException {
		if (!file.isFile()) {
			log.severe(MessageFormat.format("File ''{0}'' not found", file.toString()));
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

	protected static void addCommonHeaders(HttpExchange httpExchange, String contentType) {
		addHeader(httpExchange, HEADER_CONTENT_TYPE, contentType);
		addHeader(httpExchange, HEADER_ORIGIN, HEADER_ORIGIN_ANY);
	}

	private static void addHeader(HttpExchange httpExchange, String header, String value) {
		List<String> hVal = httpExchange.getResponseHeaders().get(header);
		if (hVal == null)
			hVal = new ArrayList<>(1);
		hVal.add(value);
		httpExchange.getResponseHeaders().put(header, hVal);
	}

	public static void addCookie(HttpExchange httpExchange, String name, String value, int validity) {
		addHeader(httpExchange, HEADER_SET_COOKIE,
				MessageFormat.format("{0}={1}; SameSite=Lax; Max-Age={2,number,#}", name, value, validity));
	}

	public static void addCookie(HttpExchange httpExchange, String name, String value) {
		addHeader(httpExchange, HEADER_SET_COOKIE, MessageFormat.format("{0}={1}; SameSite=Lax", name, value));
	}

	public static void addCookie(HttpExchange httpExchange, String name, String value, String attribs) {
		addHeader(httpExchange, HEADER_SET_COOKIE, MessageFormat.format("{0}={1}; {2}", name, value, attribs));
	}

	public static void addCookie(HttpExchange httpExchange, Cookie cookie) {
		addHeader(httpExchange, HEADER_SET_COOKIE, cookie.toString());
	}

	public void start() {
	}

	@Override
	public void close() {
	}

	public boolean canClose() {
		return true;
	}

	protected static boolean checkMandatoryPresent(Map<String, List<Optional<String>>> parameters, String paramName) {
		return getParameterFirstValue(parameters, paramName) == null;
	}

	protected static String getParameterFirstValue(Map<String, List<Optional<String>>> parameters, String paramName) {
		List<Optional<String>> vals = parameters.get(paramName);
		if (vals == null)
			return null;
		if (vals.get(0).isPresent())
			return vals.get(0).get();
		else
			return "";
	}

	protected static List<Optional<String>> getParameterValues(Map<String, List<Optional<String>>> parameters,
			String paramName) {
		List<Optional<String>> vals = parameters.get(paramName);
		return vals;
	}
}