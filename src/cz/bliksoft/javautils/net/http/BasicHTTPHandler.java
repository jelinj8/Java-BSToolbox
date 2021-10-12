package cz.bliksoft.javautils.net.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * HttpHandler to register with a path in BSHttpServer.
 *
 */
@SuppressWarnings("restriction")
abstract class BasicHTTPHandler implements HttpHandler, Closeable {
	private Logger log = Logger.getLogger(BasicHTTPHandler.class.getName());

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
	public abstract void handle(HttpExchange httpExchange, String path, String method,
			Map<String, List<Optional<String>>> params) throws IOException;

	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		String method = httpExchange.getRequestMethod().toUpperCase();

		String path = httpExchange.getRequestURI().getPath();
		String query = httpExchange.getRequestURI().getQuery();

		Map<String, List<Optional<String>>> params = URIParameterDecode.splitQuery(query);
		handle(httpExchange, path, method, params);
	}

	protected void sendOK(HttpExchange t) throws IOException {
		sendOK(t, "OK");
	}

	protected void sendOK(HttpExchange t, String message) throws IOException {
		List<String> hVal = new ArrayList<>(1);
		hVal.add("text/plain; charset=utf-8");
		t.getResponseHeaders().put("Content-Type", hVal);
		hVal = new ArrayList<>();
		hVal.add("*");
		t.getResponseHeaders().put("Access-Control-Allow-Origin", hVal);
		t.sendResponseHeaders(200, message == null ? 0 : message.getBytes().length);
		OutputStream os = t.getResponseBody();
		if (message != null)
			os.write(message.getBytes());
		os.close();
	}

	protected void sendERR(HttpExchange t, String message) throws IOException {
		List<String> hVal = new ArrayList<>(1);
		hVal.add("text/plain; charset=utf-8");
		t.getResponseHeaders().put("Content-Type", hVal);
		hVal = new ArrayList<>();
		hVal.add("*");
		t.getResponseHeaders().put("Access-Control-Allow-Origin", hVal);
		t.sendResponseHeaders(500, message.getBytes().length);
		OutputStream os = t.getResponseBody();
		os.write(message.getBytes());
		os.close();
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

	public String testRequest(String path, Map<String, List<Optional<String>>> parameters) {
		log.info("test");
		StringBuilder testResult = new StringBuilder();
		testResult.append("OK\n" + path + "\nParameters:\n-----\n");
		for (Entry<String, List<Optional<String>>> p : parameters.entrySet()) {
			String val = p.getKey() + "=";
			for (Optional<String> v : p.getValue()) {
				if (!v.isPresent())
					val += "<EMPTY>,";
				else
					val += "'" + v.get() + "',";
			}
			testResult.append(val);
			testResult.append("\n");
		}
		return testResult.toString();
	}
}