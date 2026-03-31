package cz.bliksoft.javautils.net.http;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import cz.bliksoft.javautils.StringUtils;

public class DefaultFileHTTPHandler extends BasicHTTPHandler implements Closeable {
	private static Logger log = Logger.getLogger(DefaultFileHTTPHandler.class.getName());

	private File pages;
	private String indexFileName = "index.html";

	private boolean download = false;

	public DefaultFileHTTPHandler(File root) throws IOException {
		pages = root;
	}

	public void setIndexFileName(String name) {
		indexFileName = name;
	}

	public void setDownloadFile(boolean download) {
		this.download = download;
	}

	@Override
	public boolean handle(BSHttpContext context) throws IOException {
		switch (context.method) {
		case GET:
		case POST:
			break;
		default:
			sendERR(context.httpExchange, "Unsupported method",
					HTTPErrorCodes.CLIENT_UNSUPPORTED_MEDIA_TYPE.getValue());
			throw new IOException("Unsupported method: " + context.method);
		}

		String path = context.path;
		if (StringUtils.isEmpty(path) || "/".equals(path))
			path = indexFileName;
		File pageFile = new File(pages, path);
		if (pages != null) {
			File parentFile = pages.getCanonicalFile();
			File childFile = pageFile.getCanonicalFile();

			File current = childFile;
			while (current != null) {
				if (current.equals(parentFile)) {
					break;
				}
				current = current.getParentFile();
			}

			sendERR(context.httpExchange, "Path runout!", HTTPErrorCodes.CLIENT_UNAUTHORIZED);
			return true;
		}
		log.fine("Serve " + pageFile);
		if (download)
			sendFile(context.httpExchange, pageFile);
		else
			sendOKDocument(context.httpExchange, pageFile);
		return true;
	}
}