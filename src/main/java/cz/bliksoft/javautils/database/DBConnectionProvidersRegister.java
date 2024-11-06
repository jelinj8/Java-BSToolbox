package cz.bliksoft.javautils.database;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * database connection registry for multiDB applications
 */
public class DBConnectionProvidersRegister implements Closeable {

	IDBConnectionProvider defaultProvider = null;

	private Map<String, IDBConnectionProvider> providers = new HashMap<>();

	public void add(String name, IDBConnectionProvider provider) throws Exception {
		IDBConnectionProvider originalProvider = providers.putIfAbsent(name, provider);
		if (originalProvider != null)
			throw new Exception("Already registered provider with name " + name);
	}

	public void addDefault(String name, IDBConnectionProvider provider) throws Exception {
		add(name, provider);
		defaultProvider = provider;
	}

	public IDBConnectionProvider getProvider(String name) {
		return providers.get(name);
	}

	public IDBConnectionProvider removeProvider(String name) {
		return providers.remove(name);
	}

	@Override
	public void close() throws IOException {
		for (IDBConnectionProvider provider : providers.values()) {
			provider.close();
		}
	}

	public Map<String, IDBConnectionProvider> getProviders() {
		return providers;
	}
}
