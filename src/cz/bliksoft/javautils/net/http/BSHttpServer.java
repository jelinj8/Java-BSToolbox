package cz.bliksoft.javautils.net.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;

@SuppressWarnings("restriction")
public class BSHttpServer {
	private Logger log = Logger.getLogger(BSHttpServer.class.getName());

	private boolean running = false;

	private HttpServer server;
	private Map<String, HttpHandler> httpHandlers;

	private int httpPort;

	private HttpsConfigurator httpsConfigurator = null;

	/**
	 * basic HTTP server.
	 * 
	 * @param port
	 * @throws Exception
	 */
	public BSHttpServer(int port) throws Exception {
		this.httpPort = port;
		httpHandlers = new HashMap<>();
	}

	/**
	 * HTTPS variant of the server, requires additional httpsConfigurator.
	 * 
	 * @param port
	 * @param httpsConfigurator
	 * @throws Exception
	 */
	public BSHttpServer(int port, HttpsConfigurator httpsConfigurator) throws Exception {
		this.httpsConfigurator = httpsConfigurator;
	}

	public void addHandler(String path, HttpHandler handler) {
		httpHandlers.put(path, handler);
		if (running) {
			server.createContext(path, handler);
			if (handler instanceof BasicHTTPHandler)
				((BasicHTTPHandler) handler).start();
		}
	}

	public void removeHandler(String path) {
		HttpHandler handler = httpHandlers.get(path);
		if (running) {
			server.removeContext(path);
			if (handler instanceof BasicHTTPHandler)
				((BasicHTTPHandler) handler).close();
		}
	}

	public boolean stop() throws Exception {
		if (!running)
			return true;

		log.info("Stopping server.");

		if (!beforeStop())
			return false;

		for (Object h : httpHandlers.values()) {
			if (h instanceof BasicHTTPHandler)
				if (!((BasicHTTPHandler) h).canClose())
					return false;
		}

		running = false;

		server.stop(2);

		for (Object h : httpHandlers.values()) {
			if (h instanceof BasicHTTPHandler)
				((BasicHTTPHandler) h).close();
		}

		return true;
	}

	public void start() {
		if (running)
			return;
		log.info("Starting server on port " + httpPort);
		running = true;
		try {
			if (httpsConfigurator != null) {
				server = HttpsServer.create(new InetSocketAddress(httpPort), 0);
				((HttpsServer) server).setHttpsConfigurator(httpsConfigurator);
			} else {
				server = HttpServer.create(new InetSocketAddress(httpPort), 0);
			}
			for (Entry<String, HttpHandler> var : httpHandlers.entrySet()) {
				server.createContext(var.getKey(), (HttpHandler) var.getValue());
			}

			server.setExecutor(null);
			server.start();

			for (Object h : httpHandlers.values()) {
				if (h instanceof BasicHTTPHandler)
					((BasicHTTPHandler) h).start();
			}
		} catch (IOException e) {
			log.severe("Failed to start HTTP server");
		}
	}

	public boolean beforeStop() {
		return true;
	};

}
