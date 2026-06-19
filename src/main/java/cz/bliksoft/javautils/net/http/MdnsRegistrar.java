package cz.bliksoft.javautils.net.http;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class MdnsRegistrar implements Closeable {
	private static final Logger log = Logger.getLogger(MdnsRegistrar.class.getName());

	private final InetAddress localAddress;
	private final Map<String, JmDNS> instances = new HashMap<>();

	public MdnsRegistrar() throws IOException {
		localAddress = InetAddress.getLocalHost();
	}

	public Object registerService(String serviceType, String name, int port, String path) throws IOException {
		JmDNS jmdns = instances.get(name);
		if (jmdns == null) {
			jmdns = JmDNS.create(localAddress, name + ".local");
			instances.put(name, jmdns);
		}
		Map<String, String> props;
		if (path != null) {
			props = new HashMap<>();
			props.put("path", path);
		} else {
			props = Collections.emptyMap();
		}
		ServiceInfo info = ServiceInfo.create(serviceType, name, port, 0, 0, props);
		jmdns.registerService(info);
		log.info("Registered mDNS service: " + name + ".local (" + serviceType + ") on port " + port);
		return new MdnsRegistration(jmdns, info);
	}

	public void unregisterService(Object handle) {
		if (handle instanceof MdnsRegistration) {
			MdnsRegistration reg = (MdnsRegistration) handle;
			reg.jmdns.unregisterService(reg.info);
		}
	}

	@Override
	public void close() throws IOException {
		for (JmDNS jmdns : instances.values()) {
			jmdns.unregisterAllServices();
			jmdns.close();
		}
		instances.clear();
	}

	static class MdnsRegistration {
		final JmDNS jmdns;
		final ServiceInfo info;

		MdnsRegistration(JmDNS jmdns, ServiceInfo info) {
			this.jmdns = jmdns;
			this.info = info;
		}
	}
}
