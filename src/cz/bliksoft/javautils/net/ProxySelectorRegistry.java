package cz.bliksoft.javautils.net;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ProxySelectorRegistry extends ProxySelector {
	static Logger log = Logger.getLogger(ProxySelectorRegistry.class.getName());

	private static ProxySelectorRegistry _instance = null;

	public static ProxySelectorRegistry getInstance() {
		if (_instance == null)
			_instance = new ProxySelectorRegistry();
		return _instance;
	}

	public static void register() {
		ProxySelector.setDefault(getInstance());
		log.info("Proxy register installed");
	}

	public static void unregister() {
		ProxySelector.setDefault(null);
		log.info("Proxy register removed");
	}

	private ProxySelectorRegistry() {

	}

	private Map<String, Proxy> registeredProxies = new LinkedHashMap<>();
	private Proxy defaultProxy = null;

	public static void register(String pattern, Proxy proxy) {
		getInstance().registeredProxies.put(pattern, proxy);
	}

	public static void unregister(String pattern) {
		getInstance().registeredProxies.remove(pattern);
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

		List<Proxy> list = new ArrayList<Proxy>();
		if (defaultProxy != null)
			list.add(defaultProxy);
		return list;
	}

	@Override
	public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
		System.err.println("Connection to " + uri + " failed.");
	}

}
