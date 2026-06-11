package cz.bliksoft.javautils.net.http;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.singletons.Singletons;

public class BSHttpServer implements Closeable {
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

	private InetAddress bindAddress = null;

	/**
	 * basic HTTP server.
	 *
	 * @param port TCP port to listen on
	 */
	public BSHttpServer(int port) {
		this.httpPort = port;
		httpHandlers = new HashMap<>();
	}

	/**
	 * HTTPS variant of the server, requires additional httpsConfigurator.
	 *
	 * @param port              TCP port to listen on
	 * @param httpsConfigurator TLS configuration for the server
	 */
	public BSHttpServer(int port, HttpsConfigurator httpsConfigurator) {
		this.httpPort = port;
		httpHandlers = new HashMap<>();
		this.httpsConfigurator = httpsConfigurator;
	}

	/**
	 * HTTPS server built from a pre-configured {@link SSLContext} (server key
	 * managers, and for mutual TLS, client trust managers already initialized).
	 *
	 * @param port              TCP port to listen on
	 * @param sslContext        SSLContext with server key managers and (optionally)
	 *                          client-certificate trust managers
	 * @param requireClientAuth if {@code true}, clients must present a certificate
	 *                          trusted by {@code sslContext}'s trust managers
	 */
	public BSHttpServer(int port, SSLContext sslContext, boolean requireClientAuth) {
		this.httpPort = port;
		httpHandlers = new HashMap<>();
		this.httpsConfigurator = createHttpsConfigurator(sslContext, requireClientAuth);
	}

	/**
	 * Creates and starts a basic HTTP(S) server from a {@link FileObject}
	 * configuration, allowing it to be registered as a {@code /singletons} entry
	 * and reused via {@link #getSingleton()}.
	 *
	 * <p>
	 * Supported attributes:
	 * <ul>
	 * <li>{@code port} (mandatory) - TCP port to listen on</li>
	 * <li>{@code address} (optional) - IP address/hostname to bind to, e.g. to
	 * limit the server to {@code localhost}; if absent, listens on all
	 * interfaces</li>
	 * <li>{@code keystoreFile} (optional) - path to a keystore holding the server
	 * certificate/private key; if present, the server is started in HTTPS mode</li>
	 * <li>{@code keystorePassword} (mandatory if {@code keystoreFile} is set) -
	 * password protecting the keystore (and used as the key password unless
	 * {@code keyPassword} is also given)</li>
	 * <li>{@code keystoreType} (optional, default {@code PKCS12}) - keystore
	 * format, e.g. {@code JKS}</li>
	 * <li>{@code keyPassword} (optional) - password for the private key entry, if
	 * different from {@code keystorePassword}</li>
	 * <li>{@code keyAlias} (optional) - alias of the key entry to use as the server
	 * certificate; if absent, the {@link KeyManagerFactory} default (typically the
	 * keystore's first/only key entry) is used. Required if the keystore holds
	 * multiple key entries</li>
	 * <li>{@code truststoreFile} (optional) - path to a truststore of CA
	 * certificates; if present, the server requires clients to present a
	 * certificate trusted by this truststore (mutual TLS)</li>
	 * <li>{@code truststorePassword} (optional) - password protecting the
	 * truststore, if required by its type</li>
	 * <li>{@code truststoreType} (optional, default {@code PKCS12}) - truststore
	 * format, e.g. {@code JKS}</li>
	 * </ul>
	 *
	 * @param fo configuration node providing the attributes above
	 */
	public BSHttpServer(FileObject fo) {
		String portAttr = fo.getAttribute("port", null);
		if (portAttr == null)
			throw new IllegalArgumentException("BSHttpServer requires a 'port' attribute");
		this.httpPort = Integer.parseInt(portAttr);
		httpHandlers = new HashMap<>();

		String address = fo.getAttribute("address", null);
		if (address != null) {
			try {
				this.bindAddress = InetAddress.getByName(address);
			} catch (UnknownHostException e) {
				throw new RuntimeException("Invalid BSHttpServer address: " + address, e);
			}
		}

		String keystoreFile = fo.getAttribute("keystoreFile", null);
		if (keystoreFile != null) {
			boolean requireClientAuth = fo.getAttribute("truststoreFile", null) != null;
			this.httpsConfigurator = createHttpsConfigurator(createSSLContext(fo, keystoreFile), requireClientAuth);
		}

		try {
			start();
		} catch (IOException e) {
			throw new RuntimeException("Failed to start BSHttpServer on port " + httpPort, e);
		}
	}

	/**
	 * Builds a server-side {@link SSLContext} from the {@code keystoreFile} /
	 * {@code keystorePassword} / {@code keystoreType} / {@code keyPassword}
	 * attributes of {@code fo} (see {@link #BSHttpServer(FileObject)}).
	 */
	private static SSLContext createSSLContext(FileObject fo, String keystoreFile) {
		String keystorePassword = fo.getAttribute("keystorePassword", null);
		if (keystorePassword == null)
			throw new IllegalArgumentException(
					"BSHttpServer requires a 'keystorePassword' attribute when 'keystoreFile' is set");
		String keystoreType = fo.getAttribute("keystoreType", "PKCS12");
		String keyPassword = fo.getAttribute("keyPassword", keystorePassword);
		String keyAlias = fo.getAttribute("keyAlias", null);

		try (FileInputStream is = new FileInputStream(keystoreFile)) {
			KeyStore keyStore = KeyStore.getInstance(keystoreType);
			keyStore.load(is, keystorePassword.toCharArray());

			if (keyAlias != null) {
				keyStore = extractKeyEntry(keyStore, keyAlias, keyPassword);
			}

			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, keyPassword.toCharArray());

			TrustManager[] trustManagers = null;
			String truststoreFile = fo.getAttribute("truststoreFile", null);
			if (truststoreFile != null) {
				String truststorePassword = fo.getAttribute("truststorePassword", null);
				String truststoreType = fo.getAttribute("truststoreType", "PKCS12");
				trustManagers = loadTrustManagers(truststoreFile, truststorePassword, truststoreType);
			}

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), trustManagers, null);
			return sslContext;
		} catch (Exception e) {
			throw new RuntimeException("Failed to load BSHttpServer keystore " + keystoreFile, e);
		}
	}

	/**
	 * Loads CA certificates from {@code truststoreFile} and returns
	 * {@link TrustManager}s for validating client certificates (mutual TLS).
	 *
	 * @param truststorePassword may be {@code null} for truststore types that don't
	 *                           require one
	 */
	private static TrustManager[] loadTrustManagers(String truststoreFile, String truststorePassword,
			String truststoreType) throws Exception {
		try (FileInputStream is = new FileInputStream(truststoreFile)) {
			KeyStore trustStore = KeyStore.getInstance(truststoreType);
			trustStore.load(is, truststorePassword != null ? truststorePassword.toCharArray() : null);

			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(trustStore);
			return tmf.getTrustManagers();
		}
	}

	/**
	 * Wraps {@code sslContext} in an {@link HttpsConfigurator} that, when
	 * {@code requireClientAuth} is {@code true}, requires clients to present a
	 * certificate trusted by {@code sslContext}'s trust managers (mutual TLS).
	 */
	private static HttpsConfigurator createHttpsConfigurator(SSLContext sslContext, boolean requireClientAuth) {
		return new HttpsConfigurator(sslContext) {
			@Override
			public void configure(HttpsParameters params) {
				SSLParameters sslParams = getSSLContext().getDefaultSSLParameters();
				sslParams.setNeedClientAuth(requireClientAuth);
				params.setSSLParameters(sslParams);
			}
		};
	}

	/**
	 * Returns a new in-memory keystore (same type as {@code keyStore}) containing
	 * only the key entry {@code keyAlias}, so a {@link KeyManagerFactory}
	 * initialized from it cannot pick a different entry from a multi-certificate
	 * keystore.
	 */
	private static KeyStore extractKeyEntry(KeyStore keyStore, String keyAlias, String keyPassword) throws Exception {
		if (!keyStore.isKeyEntry(keyAlias))
			throw new IllegalArgumentException("Keystore has no key entry with alias '" + keyAlias + "'");

		java.security.Key key = keyStore.getKey(keyAlias, keyPassword.toCharArray());
		java.security.cert.Certificate[] chain = keyStore.getCertificateChain(keyAlias);

		KeyStore result = KeyStore.getInstance(keyStore.getType());
		result.load(null, null);
		result.setKeyEntry(keyAlias, key, keyPassword.toCharArray(), chain);
		return result;
	}

	/**
	 * Returns the {@link BSHttpServer} registered as a {@code /singletons} entry,
	 * if any.
	 *
	 * @return the shared server singleton, or {@code null} if none is registered
	 */
	public static BSHttpServer getSingleton() {
		return Singletons.getSingleton(BSHttpServer.class);
	}

	/**
	 * Registers a handler for {@code path}. If the server is already running (e.g.
	 * a shared singleton looked up via {@link #getSingleton()}), the handler is
	 * attached immediately; otherwise it is attached when {@link #start()} runs.
	 *
	 * @param path    context path, e.g. {@code "/upload"}
	 * @param handler handler to invoke for requests under {@code path}
	 */
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

	/**
	 * Unregisters the handler for {@code path}, if any. Idempotent and safe to call
	 * regardless of run state — a no-op if {@code path} was never registered, or if
	 * the server is already stopped (e.g. the shared {@link #getSingleton()
	 * singleton} was already closed by {@link Singletons#cleanup()}).
	 *
	 * @param path context path previously passed to {@link #addHandler}
	 */
	public void removeHandler(String path) {
		HttpHandler handler = httpHandlers.remove(path);
		if (handler == null)
			return;
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

		InetSocketAddress address = bindAddress != null ? new InetSocketAddress(bindAddress, httpPort)
				: new InetSocketAddress(httpPort);

		if (httpsConfigurator != null) {
			server = HttpsServer.create(address, 0);
			((HttpsServer) server).setHttpsConfigurator(httpsConfigurator);
		} else {
			server = HttpServer.create(address, 0);
		}
		for (Entry<String, HttpHandler> handlers : httpHandlers.entrySet()) {
			server.createContext(handlers.getKey(), handlers.getValue());
		}

		if (isMultithreaded) {
			LinkedBlockingDeque<Runnable> abq = new LinkedBlockingDeque<>(MAX_BLOCKING_QUEUE);
			ThreadPoolExecutor tpe = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
					TimeUnit.SECONDS, abq);
			tpe.setThreadFactory(new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r);
					t.setName("WebServerExecutorThread");
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

	/**
	 * Stops the server, allowing {@link BSHttpServer} to be registered as a
	 * {@code /singletons} entry and cleaned up via {@link Singletons#cleanup()}.
	 * Safe to call multiple times / when not running.
	 */
	@Override
	public void close() throws IOException {
		try {
			stop();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
