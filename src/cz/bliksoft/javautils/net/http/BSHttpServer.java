package cz.bliksoft.javautils.net.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

@SuppressWarnings("restriction")
public class BSHttpServer {
	private Logger log = Logger.getLogger(BSHttpServer.class.getName());

	private boolean running = false;

	protected HttpServer server;
	private Map<String, HttpHandler> httpHandlers;

	private int httpPort;

	// When a new request is submitted and fewer than CORE_POOL_SIZE threads are
	// running, a new thread is created to handle the request,
	// even if other worker threads are idle. If there are more than CORE_POOL_SIZE
	// but less than MAX_POOL_SIZE threads running,
	// a new thread will be created only if the queue is full.
	private int CORE_POOL_SIZE = 4;
	private int MAX_POOL_SIZE = 10;

	// After pool has MAX_POOL_SIZE threads, idle threads will be terminated if they
	// have been idle for more than the KEEP_ALIVE_TIME in seconds.
	private int KEEP_ALIVE_TIME = 30;

	// After thread pool at CORE_POOL_SIZE up to MAX_BLOCKING_QUEUE more requests
	// will be queued up before execution
	private int MAX_BLOCKING_QUEUE = 100;

	public int getPoolSize() {
		return CORE_POOL_SIZE;
	}

	public void setPoolSize(int corePoolSize) {
		CORE_POOL_SIZE = corePoolSize;
	}

	public int getMaxPoolSize() {
		return MAX_POOL_SIZE;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		MAX_POOL_SIZE = maxPoolSize;
	}

	public int getKeepAliveTime() {
		return KEEP_ALIVE_TIME;
	}

	public void setKeepAliveTime(int keepAliveTime) {
		KEEP_ALIVE_TIME = keepAliveTime;
	}

	public int getMaxBlockingQueue() {
		return MAX_BLOCKING_QUEUE;
	}

	public void setMaxBlockingQueue(int maxBlockingQueue) {
		MAX_BLOCKING_QUEUE = maxBlockingQueue;
	}

	private boolean isMultithreaded = false;

	public void setMultithreaded(boolean multithreaded) {
		if (running)
			throw new RuntimeException("Can't change multithreading on running server!");
		isMultithreaded = multithreaded;
	}

	private HttpsConfigurator httpsConfigurator = null;

	/**
	 * basic HTTP server.
	 * 
	 * @param port
	 * @throws Exception
	 */
	public BSHttpServer(int port) {
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
	public BSHttpServer(int port, HttpsConfigurator httpsConfigurator) {
		this.httpPort = port;
		this.httpsConfigurator = httpsConfigurator;
	}

	public void addHandler(String path, HttpHandler handler) {
		if (handler instanceof BasicHTTPHandler && ((BasicHTTPHandler) handler).getSupportedMethods().isEmpty())
			throw new RuntimeException(MessageFormat.format("Handler {0} has no supported methods!", handler));

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

		for (HttpHandler h : httpHandlers.values()) {
			if (h instanceof BasicHTTPHandler)
				if (!((BasicHTTPHandler) h).canClose())
					return false;
		}

		running = false;

		server.stop(2);

		for (HttpHandler h : httpHandlers.values()) {
			if (h instanceof BasicHTTPHandler)
				((BasicHTTPHandler) h).close();
		}

		server = null;

		return true;
	}

	public void start() throws IOException {
		if (running)
			return;
		log.info("Starting server on port " + httpPort);
		running = true;

		if (httpsConfigurator != null) {
			server = HttpsServer.create(new InetSocketAddress(httpPort), 0);
			((HttpsServer) server).setHttpsConfigurator(httpsConfigurator);
		} else {
			server = HttpServer.create(new InetSocketAddress(httpPort), 0);
		}
		for (Entry<String, HttpHandler> handlers : httpHandlers.entrySet()) {
			server.createContext(handlers.getKey(), handlers.getValue());
		}

		if (isMultithreaded) {
			ArrayBlockingQueue<Runnable> abq = new ArrayBlockingQueue<>(MAX_BLOCKING_QUEUE);
			ThreadPoolExecutor tpe = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
					TimeUnit.SECONDS, abq);
			tpe.setThreadFactory(new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r);
					t.setName("WebServerExecutorThread-" + t.getId());
					return t;
				}
			});
			server.setExecutor(tpe);
		} else
			server.setExecutor(null);

		beforeStart();

		server.start();

		for (HttpHandler h : httpHandlers.values()) {
			if (h instanceof BasicHTTPHandler)
				((BasicHTTPHandler) h).start();
		}
	}

	/**
	 * can block stopping of web server
	 * 
	 * @return
	 */
	public boolean beforeStop() {
		return true;
	}

	/**
	 * can be used e.g. to bind webservices
	 */
	public void beforeStart() {
	}

	public HttpServer getServer() {
		return server;
	}
}
