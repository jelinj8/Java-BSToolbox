package cz.bliksoft.javautils.net.http;

import java.io.ByteArrayOutputStream;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
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
	private Logger log = Logger.getLogger(BasicHTTPHandler.class.getName());

	public static final String CONTENT_TYPE_TXT = "text/plain; charset=utf-8";
	public static final String CONTENT_TYPE_HTML = "text/html; charset=utf-8";
	public static final String CONTENT_TYPE_XML = "text/xml; charset=utf-8";
	public static final String CONTENT_TYPE_DATA = "application/octet-stream";
	public static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	public static final String HEADER_CONTENT_TYPE_URLENCODED = "application/x-www-form-urlencoded";
	public static final String HEADER_CONTENT_TYPE_MULTIPART = "multipart/form-data";
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
	 * @param path
	 *            URI, starts with '/'
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

	protected boolean isMultipart(HttpExchange httpExchange) {
		Headers headers = httpExchange.getRequestHeaders();
		String contentType = headers.getFirst(HEADER_CONTENT_TYPE);
		return (contentType.startsWith(HEADER_CONTENT_TYPE_MULTIPART));
	}

	protected Map<String, List<Optional<MultiPart>>> getMultipartPostParams(HttpExchange httpExchange)
			throws IOException {
		Headers headers = httpExchange.getRequestHeaders();
		String contentType = headers.getFirst(HEADER_CONTENT_TYPE);

		HashMap<String, List<Optional<MultiPart>>> result = new HashMap<>();

		if (contentType.startsWith(HEADER_CONTENT_TYPE_MULTIPART)) {

			// found form data
			String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);
			// as of rfc7578 - prepend "\r\n--"
			byte[] boundaryBytes = ("\r\n--" + boundary).getBytes(StandardCharsets.UTF_8);
			byte[] payload = getInputAsBinary(httpExchange.getRequestBody());

			List<Integer> offsets = searchBytes(payload, boundaryBytes, 0, payload.length - 1);
			offsets.add(0, Integer.valueOf(0));
			for (int idx = 0; idx < offsets.size(); idx++) {
				int startPart = offsets.get(idx);
				int endPart = payload.length;
				if (idx < offsets.size() - 1) {
					endPart = offsets.get(idx + 1);
				}
				byte[] part = Arrays.copyOfRange(payload, startPart, endPart);
				// look for header
				int headerEnd = indexOf(part, "\r\n\r\n".getBytes(StandardCharsets.UTF_8), 0, part.length - 1);
				if (headerEnd > 0) {
					MultiPart p = new MultiPart();
					byte[] head = Arrays.copyOfRange(part, 0, headerEnd);
					String header = new String(head);
					// extract name from header
					int nameIndex = header.indexOf("\r\nContent-Disposition: form-data; name=");
					if (nameIndex >= 0) {
						int startMarker = nameIndex + 39;
						// check for extra filename field
						int fileNameStart = header.indexOf("; filename=");
						if (fileNameStart >= 0) {
							String filename = header.substring(fileNameStart + 11,
									header.indexOf("\r\n", fileNameStart));
							p.filename = filename.replace('"', ' ').replace('\'', ' ').trim();
							p.name = header.substring(startMarker, fileNameStart).replace('"', ' ').replace('\'', ' ')
									.trim();
							p.type = MultiPart.PartType.FILE;
						} else {
							int endMarker = header.indexOf("\r\n", startMarker);
							if (endMarker == -1)
								endMarker = header.length();
							p.name = header.substring(startMarker, endMarker).replace('"', ' ').replace('\'', ' ')
									.trim();
							p.type = MultiPart.PartType.TEXT;
						}
					} else {
						// skip entry if no name is found
						continue;
					}
					// extract content type from header
					int typeIndex = header.indexOf("\r\nContent-Type:");
					if (typeIndex >= 0) {
						int startMarker = typeIndex + 15;
						int endMarker = header.indexOf("\r\n", startMarker);
						if (endMarker == -1)
							endMarker = header.length();
						p.contentType = header.substring(startMarker, endMarker).trim();
					}

					// handle content
					if (p.type == MultiPart.PartType.TEXT) {
						// extract text value
						byte[] body = Arrays.copyOfRange(part, headerEnd + 4, part.length);
						p.value = new String(body);
					} else {
						// must be a file upload
						p.bytes = Arrays.copyOfRange(part, headerEnd + 4, part.length);
					}
					List<Optional<MultiPart>> l = result.get(p.name);
					if (l == null) {
						l = new ArrayList<>();
						result.put(p.name, l);
					}
					l.add(Optional.of(p));
				}
			}

			return result;
		} else {
			Map<String, List<Optional<String>>> pars = getPostParams(httpExchange);
			for (Entry<String, List<Optional<String>>> entry : pars.entrySet()) {
				List<Optional<MultiPart>> i = new ArrayList<>();
				for (Optional<String> val : entry.getValue()) {
					MultiPart mp = null;
					if (val.isPresent()) {
						mp = new MultiPart();
						mp.value = val.get();
						mp.type = MultiPart.PartType.TEXT;
					}
					i.add(Optional.of(mp));
				}
				result.put(entry.getKey(), i);
			}
		}
		return result;
	}

	private Map<String, List<Optional<String>>> getPostParams(HttpExchange httpExchange) throws IOException {
		return URIParameterDecode.splitQuery(getRequestBody(httpExchange));
	}

	public static byte[] getInputAsBinary(InputStream requestStream) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			byte[] buf = new byte[100000];
			int bytesRead = 0;
			while ((bytesRead = requestStream.read(buf)) != -1) {
				// while (requestStream.available() > 0) {
				// int i = requestStream.read(buf);
				bos.write(buf, 0, bytesRead);
			}
			requestStream.close();
			bos.close();
		} catch (IOException e) {
			Logger log = Logger.getLogger(BasicHTTPHandler.class.getName());
			log.log(Level.SEVERE, "error while decoding http input stream", e);
		}
		return bos.toByteArray();
	}

	/**
	 * Search bytes in byte array returns indexes within this byte-array of all
	 * occurrences of the specified(search bytes) byte array in the specified range
	 * borrowed from
	 * https://github.com/riversun/finbin/blob/master/src/main/java/org/riversun/finbin/BinarySearcher.java
	 *
	 * @param srcBytes
	 * @param searchBytes
	 * @param searchStartIndex
	 * @param searchEndIndex
	 * @return result index list
	 */
	public List<Integer> searchBytes(byte[] srcBytes, byte[] searchBytes, int searchStartIndex, int searchEndIndex) {
		final int destSize = searchBytes.length;
		final List<Integer> positionIndexList = new ArrayList<Integer>();
		int cursor = searchStartIndex;
		while (cursor < searchEndIndex + 1) {
			int index = indexOf(srcBytes, searchBytes, cursor, searchEndIndex);
			if (index >= 0) {
				positionIndexList.add(index);
				cursor = index + destSize;
			} else {
				cursor++;
			}
		}
		return positionIndexList;
	}

	/**
	 * Returns the index within this byte-array of the first occurrence of the
	 * specified(search bytes) byte array.<br>
	 * Starting the search at the specified index, and end at the specified index.
	 * borrowed from
	 * https://github.com/riversun/finbin/blob/master/src/main/java/org/riversun/finbin/BinarySearcher.java
	 *
	 * @param srcBytes
	 * @param searchBytes
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	public int indexOf(byte[] srcBytes, byte[] searchBytes, int startIndex, int endIndex) {
		if (searchBytes.length == 0 || (endIndex - startIndex + 1) < searchBytes.length) {
			return -1;
		}
		int maxScanStartPosIdx = srcBytes.length - searchBytes.length;
		final int loopEndIdx;
		if (endIndex < maxScanStartPosIdx) {
			loopEndIdx = endIndex;
		} else {
			loopEndIdx = maxScanStartPosIdx;
		}
		int lastScanIdx = -1;
		label: // goto label
		for (int i = startIndex; i <= loopEndIdx; i++) {
			for (int j = 0; j < searchBytes.length; j++) {
				if (srcBytes[i + j] != searchBytes[j]) {
					continue label;
				}
				lastScanIdx = i + j;
			}
			if (endIndex < lastScanIdx || lastScanIdx - i + 1 < searchBytes.length) {
				// it becomes more than the last index
				// or less than the number of search bytes
				return -1;
			}
			return i;
		}
		return -1;
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

	protected void sendOKResource(HttpExchange httpExchange, Class<?> loader, String path) throws IOException {
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

	protected void sendERR(HttpExchange httpExchange, String message, HTTPErrorCodes code) throws IOException {
		sendERR(httpExchange, message, CONTENT_TYPE_TXT, code.getValue());
	}

	protected void sendERR(HttpExchange httpExchange, String message, Integer code) throws IOException {
		sendERR(httpExchange, message, CONTENT_TYPE_TXT, code);
	}

	protected void sendERR(HttpExchange httpExchange, String message, String contentType, HTTPErrorCodes code)
			throws IOException {
		sendERR(httpExchange, message, contentType, code.getValue());
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

	protected void sendFile(HttpExchange httpExchange, File file) throws IOException {
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