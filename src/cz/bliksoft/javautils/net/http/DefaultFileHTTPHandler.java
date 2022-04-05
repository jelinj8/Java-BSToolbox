package cz.bliksoft.javautils.net.http;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange;

import cz.bliksoft.javautils.StringUtils;

@SuppressWarnings("restriction")
public class DefaultFileHTTPHandler extends BasicHTTPHandler implements Closeable {
	private static Logger log = Logger.getLogger(DefaultFileHTTPHandler.class.getName());

	private File pages;
	private String indexFileName = "index.html";

	public DefaultFileHTTPHandler(File root) throws IOException {
		pages = root;
	}

	public void setIndexFileName(String name) {
		indexFileName = name;
	}

	@Override
	public void handle(HttpExchange exchange, String path, String query, String method) throws IOException {
		switch (method) {
		case "GET":
		case "POST":
			break;
		default:
			sendERR(exchange, "Unsupported method", HTTPErrorCodes.CLIENT_UNSUPPORTED_MEDIA_TYPE.getValue());
			throw new IOException("Unsupported method: " + method);
		}

		if (StringUtils.isEmpty(path) || "/".equals(path))
			path = indexFileName;
		File pageFile = new File(pages, path);
		log.fine("Serve " + pageFile);
		if (pageFile.exists()) {
			sendOKDocument(exchange, pageFile);
		} else {
			sendERR(exchange, "Not found", HTTPErrorCodes.CLIENT_NOT_FOUND.getValue());
		}
	}
}