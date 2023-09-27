package cz.bliksoft.javautils.net;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.Proxy.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import cz.bliksoft.javautils.PropertiesUtils;
import cz.bliksoft.javautils.StringUtils;

public class ProxySelectorRegistry extends ProxySelector {
	static Logger log = Logger.getLogger(ProxySelectorRegistry.class.getName());

	private static ProxySelectorRegistry _instance = null;

	private static ProxySelector originalProxySelector = null;

	public static ProxySelectorRegistry getInstance() {
		if (_instance == null)
			_instance = new ProxySelectorRegistry();
		return _instance;
	}

	public static void register() {
		if (ProxySelector.getDefault() != getInstance()) {
			originalProxySelector = ProxySelector.getDefault();
			ProxySelector.setDefault(getInstance());
			log.info("Proxy register installed");
		}
	}

	public static void unregister() {
		if (ProxySelector.getDefault() == getInstance()) {
			ProxySelector.setDefault(originalProxySelector);
			log.info("Proxy register removed");
		}
	}

	private ProxySelectorRegistry() {

	}

	private Map<String, Proxy> registeredProxies = new LinkedHashMap<>();
	private Proxy defaultProxy = null;

	public static void register(String pattern, Proxy proxy) {
		getInstance().registeredProxies.put(pattern, proxy);
	}

	public static void registerUrl(String url, Proxy proxy) {
		getInstance().registeredProxies.put(Pattern.quote(url), proxy);
	}

	public static void unregister(String pattern) {
		getInstance().registeredProxies.remove(pattern);
	}

	public static void unregisterUrl(String url) {
		getInstance().registeredProxies.remove(Pattern.quote(url));
	}

	public static void setDefault(Proxy defaultProxy) {
		getInstance().defaultProxy = defaultProxy;
	}

	@Override
	public List<Proxy> select(URI uri) {
		log.fine("Looking for proxy for URI " + uri.toString());
		for (Entry<String, Proxy> p : registeredProxies.entrySet()) {
			if (Pattern.matches(p.getKey(), uri.toString())) {
				List<Proxy> list = new ArrayList<Proxy>();
				list.add(p.getValue());
				return list;
			}
		}

		if (defaultProxy != null) {
			List<Proxy> list = new ArrayList<Proxy>();
			list.add(defaultProxy);
			return list;
		}

		log.info("Proxy not selected for " + uri);

		if (originalProxySelector != null)
			return originalProxySelector.select(uri);
		else {
			List<Proxy> list = new ArrayList<Proxy>();
			list.add(Proxy.NO_PROXY);
			return list;
		}
	}

	@Override
	public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
		log.log(Level.SEVERE, "Connection to " + uri + " failed.", ioe);
	}

	public static void addProxyConfiguration(File propertiesF) throws IOException {
		addProxyConfiguration(PropertiesUtils.loadFromFile(propertiesF));
	}

	public static void addProxyConfiguration(Properties properties) {
		String proxyUrl = properties.getProperty("proxyUrl");
		String proxyMatch = properties.getProperty("proxyMatch");
		String proxyTarget = properties.getProperty("proxyTarget");

		if (StringUtils.hasLength(proxyUrl) && (proxyTarget != null || proxyMatch != null)) {
			int proxyPort = Integer.valueOf(properties.getProperty("proxyPort"));

			ProxySelectorRegistry.register();

			if (StringUtils.hasLength(proxyMatch))
				ProxySelectorRegistry.register(proxyMatch,
						new Proxy(Type.SOCKS, new InetSocketAddress(proxyUrl, proxyPort)));
			else
				ProxySelectorRegistry.registerUrl(proxyTarget,
						new Proxy(Type.SOCKS, new InetSocketAddress(proxyUrl, proxyPort)));
		}
	}
}
