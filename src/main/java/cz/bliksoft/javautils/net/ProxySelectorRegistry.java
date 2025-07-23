package cz.bliksoft.javautils.net;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.text.MessageFormat;
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

	public static final String PROP_PROXY_PORT = "proxyPort";

	public static final String PROP_PROXY_TARGET = "proxyTarget";

	public static final String PROP_PROXY_MATCH = "proxyMatch";

	public static final String PROP_PROXY_URL = "proxyUrl";

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
		log.fine("Searching proxy for URI " + uri.toString());
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

		if (registeredProxies.size() > 0)
			log.info(MessageFormat
					.format("Proxy not found for {0} and no default was specified, using direct connection.", uri));

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

	/**
	 * load proxy config from properties file
	 * 
	 * @param propertiesF
	 * @throws IOException
	 */
	public static void addProxyConfiguration(File propertiesF) throws IOException {
		addProxyConfiguration(PropertiesUtils.loadFromFile(propertiesF));
	}

	/**
	 * load proxy config from loaded properties
	 * 
	 * @param properties
	 */
	public static void addProxyConfiguration(Properties properties) {
		addProxyConfiguration(properties, null);
	}

	/**
	 * load proxy config from loaded properties, use specified prefix for property
	 * names
	 * 
	 * @param properties
	 * @param configPrefix
	 */
	public static void addProxyConfiguration(Properties properties, String configPrefix) {
		boolean hasPrefix = StringUtils.hasText(configPrefix);

		String proxyUrl = properties
				.getProperty(hasPrefix ? StringUtils.camelPrefix(PROP_PROXY_URL, configPrefix) : PROP_PROXY_URL);
		String proxyMatch = properties
				.getProperty(hasPrefix ? StringUtils.camelPrefix(PROP_PROXY_MATCH, configPrefix) : PROP_PROXY_MATCH);
		String proxyTarget = properties
				.getProperty(hasPrefix ? StringUtils.camelPrefix(PROP_PROXY_TARGET, configPrefix) : PROP_PROXY_TARGET);
		int proxyPort = -1;

		if (StringUtils.hasLength(proxyUrl)) {
			try {
				proxyPort = Integer.valueOf(properties.getProperty(
						hasPrefix ? StringUtils.camelPrefix(PROP_PROXY_PORT, configPrefix) : PROP_PROXY_PORT));
			} catch (NumberFormatException e) {
				log.severe("Missing or malformed 'proxyPort' in properties");
				throw e;
			}

			addProxyConfiguration(proxyUrl, proxyPort, proxyTarget, proxyMatch);
		}
	}

	/**
	 * register a proxy configuration. If neither proxyTarget nor proxyMatch is
	 * specified, sets a default proxy
	 * 
	 * @param proxyUrl    proxy url to be used
	 * @param proxyPort   proxy port
	 * @param proxyTarget specific URL or comma separated list of URLs to redirect
	 *                    trough proxy
	 * @param proxyMatch  regex or comma separated regex list to match against url
	 */
	public static void addProxyConfiguration(String proxyUrl, Integer proxyPort, String proxyTarget,
			String proxyMatch) {
		if (StringUtils.hasLength(proxyUrl)) {
			ProxySelectorRegistry.register();

			if (StringUtils.hasLength(proxyTarget) || StringUtils.hasLength(proxyMatch)) {
				if (StringUtils.hasLength(proxyMatch)) {
					if (proxyMatch.contains(",")) {
						for (String targetREGEX : proxyMatch.split(",")) {
							log.info(MessageFormat.format("Adding proxy {0}:{1,number,#} for target matching regex {2}",
									proxyUrl, proxyPort, targetREGEX));
							ProxySelectorRegistry.register(targetREGEX,
									new Proxy(Type.SOCKS, new InetSocketAddress(proxyUrl, proxyPort)));
						}
					} else {
						log.info(MessageFormat.format("Adding proxy {0}:{1,number,#} for target matching regex {2}",
								proxyUrl, proxyPort, proxyMatch));
						ProxySelectorRegistry.register(proxyMatch,
								new Proxy(Type.SOCKS, new InetSocketAddress(proxyUrl, proxyPort)));
					}
				}

				if (StringUtils.hasLength(proxyTarget)) {
					if (proxyTarget.contains(",")) {
						for (String targetURI : proxyTarget.split(",")) {
							log.info(MessageFormat.format("Adding proxy {0}:{1,number,#} for target {2}", proxyUrl,
									proxyPort, targetURI));
							ProxySelectorRegistry.registerUrl(targetURI,
									new Proxy(Type.SOCKS, new InetSocketAddress(proxyUrl, proxyPort)));
						}
					} else {
						log.info(MessageFormat.format("Adding proxy {0}:{1,number,#} for target {2}", proxyUrl,
								proxyPort, proxyTarget));
						ProxySelectorRegistry.registerUrl(proxyTarget,
								new Proxy(Type.SOCKS, new InetSocketAddress(proxyUrl, proxyPort)));
					}
				}
			} else {
				log.info(MessageFormat.format(
						"No 'proxyMatch' or 'proxyTarget' specified, setting default proxy {0}:{1,number,#}", proxyUrl,
						proxyPort));
				ProxySelectorRegistry.setDefault(new Proxy(Type.SOCKS, new InetSocketAddress(proxyUrl, proxyPort)));
			}
		}
	}
}
