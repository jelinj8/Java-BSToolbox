package cz.bliksoft.javautils.net.http;

import java.io.Closeable;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Logger;

public class DefaultResourceHTTPHandler extends BasicHTTPHandler implements Closeable {
	private static Logger log = Logger.getLogger(DefaultResourceHTTPHandler.class.getName());

	private String pages;
	private Class<?> loader;

	private boolean download = false;

	public DefaultResourceHTTPHandler(Class<?> loaderClass, String subpath) throws IOException {
		pages = subpath;
		loader = loaderClass;
		addSupportedMethods(HttpMethod.GET);
	}

	public DefaultResourceHTTPHandler(Class<?> loaderClass) throws IOException {
		pages = null;
		loader = loaderClass;
		addSupportedMethods(HttpMethod.GET);
	}

	public void setDownloadFile(boolean download) {
		this.download = download;
	}

	@Override
	public boolean handle(BSHttpContext context) throws IOException {
		String fullPath = (pages != null ? context.path.replace(pages, "") : context.path);
		if (fullPath.startsWith("/"))
			fullPath = fullPath.substring(1);
		try {
			if (download)
				sendClasspathResource(context.httpExchange, loader, fullPath);
			else
				sendOKResource(context.httpExchange, loader, fullPath);
			return true;
		} catch (IOException e) {
			log.fine(MessageFormat.format("Failed to serve resource {0}: {1}", fullPath, e.getMessage()));
			throw e;
		}
	}
}