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
import java.util.regex.Pattern;

public class ProxySelectorRegistry extends ProxySelector {

	private static ProxySelectorRegistry _instance = null;

	public static ProxySelectorRegistry getInstance() {
		if (_instance == null)
			_instance = new ProxySelectorRegistry();
		return _instance;
	}

	public static void register() {
		ProxySelector.setDefault(getInstance());
	}

	public static void unregister() {
		ProxySelector.setDefault(null);
	}

	private ProxySelectorRegistry() {

	}

	private Map<String, Proxy> registeredProxies = new LinkedHashMap<>();
	private static Proxy defaultProxy = null;

	public void register(String pattern, Proxy proxy) {
		registeredProxies.put(pattern, proxy);
	}

	public void setDefault(Proxy defaultProxy) {
		ProxySelectorRegistry.defaultProxy = defaultProxy;
	}

	@Override
	public List<Proxy> select(URI uri) {
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
