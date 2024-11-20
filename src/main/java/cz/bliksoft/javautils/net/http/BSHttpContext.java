package cz.bliksoft.javautils.net.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.apache.commons.io.IOUtils;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;

import cz.bliksoft.javautils.ByteUtils;
import cz.bliksoft.javautils.DateUtils;
import cz.bliksoft.javautils.TimestampedObject;
import cz.bliksoft.javautils.collections.TimestampedHashMap;
import cz.bliksoft.javautils.net.http.BasicHTTPHandler.HttpMethod;
import cz.bliksoft.javautils.net.http.Cookie.SameSite;
import cz.bliksoft.javautils.net.http.MultiPart.PartType;

public class BSHttpContext extends HashMap<String, Object> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4992117986351783992L;

	public static final String CTX_COOKIES = "cookies";
	public static final String CTX_POST = "POST";
	public static final String CTX_GET = "GET";
	public static final String CTX_REQUEST = "REQUEST";
	public static final String CTX_BASEPATH = "basepath";
	public static final String CTX_PATH = "path";
	public static final String CTX_SESSION = "session";

	public HttpMethod method;
	public String path;
	public HttpExchange httpExchange;
	public HttpContext ctx;
	public Map<String, List<Optional<String>>> GET;
	public Map<String, Object> request;
	public Map<String, List<Optional<MultiPart>>> POST = null;
	public Map<String, String> cookies;
	public Map<String, Object> contextVariables = null;

	public boolean handled = false;

	private String sessionID;

	/**
	 * session time validity in minutes
	 */
	protected static int sessionValidity = 20;

	/**
	 * session values global storage
	 */
	protected static TimestampedHashMap<String, Map<String, Object>> sessionCache = new TimestampedHashMap<>(
			sessionValidity * 60000, new BiConsumer<String, TimestampedObject<Map<String, Object>>>() {

				@Override
				public void accept(String t, TimestampedObject<Map<String, Object>> u) {
					Map<String, Object> sessionToDelete = u.getValue();
					for (Object o : sessionToDelete.values()) {
						if (o instanceof AutoCloseable) {
							try {
								((AutoCloseable) o).close();
							} catch (Exception e) {
							}
						}
					}
				}
			});

	/**
	 * current session data
	 */
	private Map<String, Object> session = null;

	/**
	 * get current session data
	 * 
	 * @return
	 */
	public Map<String, Object> getSession() {
		return session;
	}

	/**
	 * initialize session context
	 * 
	 * @param contextName
	 * @param cookiePath
	 * @return
	 */
	public void initSession(String contextName, String cookiePath) {
		final String sessionKey = contextName + "_";
		sessionCache.cleanup();
		sessionID = cookies.get(sessionKey + BasicHTTPHandler.COOKIE_SESSION_ID);
		session = sessionCache.touch(sessionID);
		if (session == null) {
			sessionID = UUID.randomUUID().toString();
			session = new HashMap<>();
			session.put("id", sessionID);
			session.put("started", Instant.now());
			sessionCache.put(sessionID, session);
			cookies.put(BasicHTTPHandler.COOKIE_SESSION_ID, sessionID);
			BasicHTTPHandler.addCookie(httpExchange,
					Cookie.create(sessionKey + BasicHTTPHandler.COOKIE_SESSION_ID, sessionID).withSameSite(SameSite.Lax)
							.withPath(cookiePath));
			session.put("lastReq", DateUtils.XMLTimestampString());
		} else {
			session.put("lastReq", cookies.get(sessionKey + BasicHTTPHandler.COOKIE_PREVIOUS_REQ));
		}

		put(CTX_SESSION, session);

		BasicHTTPHandler.addCookie(httpExchange,
				Cookie.create(sessionKey + BasicHTTPHandler.COOKIE_PREVIOUS_REQ, DateUtils.XMLTimestampString())
						.withSameSite(SameSite.Lax).withPath(cookiePath));
	}

	/**
	 * clear all session data
	 */
	public void clearSession() {
		sessionCache.remove(sessionID);
		session.clear();
	}

	/**
	 * get POST data
	 * 
	 * @param httpExchange
	 * @return
	 * @throws IOException
	 */
	protected Map<String, List<Optional<MultiPart>>> getMultipartPostParams(HttpExchange httpExchange)
			throws IOException {
		Headers headers = httpExchange.getRequestHeaders();
		String contentType = headers.getFirst(BasicHTTPHandler.HEADER_CONTENT_TYPE);

		HashMap<String, List<Optional<MultiPart>>> result = new HashMap<>();

		if (contentType.startsWith(BasicHTTPHandler.HEADER_CONTENT_TYPE_MULTIPART)) {

			// found form data
			String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);
			// as of rfc7578 - prepend "\r\n--"
			byte[] boundaryBytes = ("\r\n--" + boundary).getBytes(StandardCharsets.UTF_8);
			byte[] payload = ByteUtils.getInputAsBinary(httpExchange.getRequestBody());

			List<Integer> offsets = ByteUtils.searchBytes(payload, boundaryBytes, 0, payload.length - 1);
			offsets.add(0, Integer.valueOf(0));
			for (int idx = 0; idx < offsets.size(); idx++) {
				int startPart = offsets.get(idx);
				int endPart = payload.length;
				if (idx < offsets.size() - 1) {
					endPart = offsets.get(idx + 1);
				}
				byte[] part = Arrays.copyOfRange(payload, startPart, endPart);
				// look for header
				int headerEnd = ByteUtils.indexOf(part, "\r\n\r\n".getBytes(StandardCharsets.UTF_8), 0,
						part.length - 1);
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
			Map<String, List<Optional<String>>> pars = URIParameterDecode
					.splitQuery(IOUtils.toString(httpExchange.getRequestBody(), StandardCharsets.UTF_8));
			for (Entry<String, List<Optional<String>>> entry : pars.entrySet()) {
				List<Optional<MultiPart>> i = new ArrayList<>();
				for (Optional<String> val : entry.getValue()) {
					MultiPart mp = null;
					if (val.isPresent()) {
						mp = new MultiPart();
						mp.name = entry.getKey();
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

	/**
	 * process all received cookies, mimic other servers - first occurrence of each
	 * received value is returned
	 * 
	 * @param httpExchange
	 * @return
	 */
	public Map<String, String> getCookies(HttpExchange httpExchange) {
		Map<String, String> cookies = new HashMap<>();

		List<String> cookieStrings = httpExchange.getRequestHeaders().get(BasicHTTPHandler.HEADER_COOKIE);
		if (cookieStrings != null)
			for (String cookieString : cookieStrings)
				if (cookieString != null) {
					String[] cookiePairs = cookieString.split("; ");
					for (int i = 0; i < cookiePairs.length; i++) {
						String[] cookieValue = cookiePairs[i].split("=");
						if (!cookies.containsKey(cookieValue[0]))
							cookies.put(cookieValue[0], cookieValue[1]);
					}
				}

		return cookies;
	}

	/**
	 * build a context from request
	 * 
	 * @param pathPrefix
	 * @param httpExchange
	 * @param path
	 * @param query
	 * @param method
	 * @throws IOException
	 */
	public BSHttpContext(String pathPrefix, HttpExchange httpExchange, String path, String query, HttpMethod method)
			throws IOException {
		this.method = method;
		this.httpExchange = httpExchange;

		this.path = path;
		put(CTX_PATH, path);

		ctx = httpExchange.getHttpContext();

		put(CTX_BASEPATH, ctx.getPath());

		GET = URIParameterDecode.splitQuery(query);
		put(CTX_GET, GET);

		request = new HashMap<>();
		put(CTX_REQUEST, request);
		for (Entry<String, List<Optional<String>>> kv : GET.entrySet()) {
			for (Optional<String> s : kv.getValue()) {
				request.putIfAbsent(kv.getKey(), s.isPresent() ? s.get() : null);
			}
		}

		if (method == HttpMethod.POST) {
			POST = getMultipartPostParams(httpExchange);
			put(CTX_POST, POST);
			for (Entry<String, List<Optional<MultiPart>>> kv : POST.entrySet()) {
				for (Optional<MultiPart> s : kv.getValue()) {
					if (s.get().type == PartType.TEXT)
						request.putIfAbsent(kv.getKey(), s.isPresent() ? s.get().value : null);
				}
			}
		}

		cookies = getCookies(httpExchange);
		put(CTX_COOKIES, cookies);
	}

	/**
	 * mark request as handled for handler chains
	 */
	public void setHandled() {
		handled = true;
	}

	/**
	 * check if request was already handled
	 * 
	 * @return
	 */
	public boolean isHandled() {
		return handled;
	}
}
