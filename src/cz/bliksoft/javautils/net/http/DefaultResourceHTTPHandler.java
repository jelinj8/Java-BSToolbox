package cz.bliksoft.javautils.net.http;

import java.io.Closeable;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
public class DefaultResourceHTTPHandler extends BasicHTTPHandler implements Closeable {
	private static Logger log = Logger.getLogger(DefaultResourceHTTPHandler.class.getName());

	private String pages;
	private Class<?> loader;

	private boolean download = false;

	public DefaultResourceHTTPHandler(Class<?> loaderClass, String subpath) throws IOException {
		pages = subpath;
		loader = loaderClass;
	}

	public DefaultResourceHTTPHandler(Class<?> loaderClass) throws IOException {
		pages = null;
		loader = loaderClass;
	}

	public void setDownloadFile(boolean download) {
		this.download = download;
	}

	@Override
	public void handle(HttpExchange exchange, String path, String query, HttpMethod method) throws IOException {
		switch (method) {
		case GET:
		case POST:
			break;
		default:
			sendERR(exchange, "Unsupported method", HTTPErrorCodes.CLIENT_UNSUPPORTED_MEDIA_TYPE.getValue());
			throw new IOException("Unsupported method: " + method);
		}

		String fullPath = (pages != null ? path.replace(pages, "") : path);
		if (fullPath.startsWith("/"))
			fullPath = fullPath.substring(1);
		try {
			if (download)
				sendClasspathResource(exchange, loader, fullPath);
			else
				sendOKResource(exchange, loader, fullPath);
		} catch (IOException e) {
			log.severe(MessageFormat.format("Failed to serve resource {0}: {1}", fullPath, e.getMessage()));
			throw e;
		}
	}
}