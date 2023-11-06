package cz.bliksoft.javautils.net.http;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import cz.bliksoft.javautils.GeneralUtils;
import cz.bliksoft.javautils.freemarker.includes.BuiltinTemplateLoader;
import freemarker.cache.ClassTemplateLoader;

public class SystemReportHTTPHandler extends DefaultFreemarkerHTTPHandler {

	public SystemReportHTTPHandler() {
		addSupportedGETPOST();
		setTemplateLoader(new ClassTemplateLoader(BuiltinTemplateLoader.class, "builtin"));

	}

	public static final int mb = 1024 * 1024;;

	@Override
	public void handle(BSHttpContext context) throws IOException {
		context.requested = "systemstatus.ftlh";

		Runtime instance = Runtime.getRuntime();
		variables.put("MEM_TOTAL", instance.totalMemory() / mb);
		variables.put("MEM_FREE", instance.freeMemory() / mb);
		variables.put("MEM_USED", (instance.totalMemory() - instance.freeMemory()) / mb);
		variables.put("MEM_MAX", instance.maxMemory() / mb);

		String version = System.getProperty("java.version");

		if (version.startsWith("1.")) {
			version = version.substring(2, 3);
		} else {
			int dot = version.indexOf(".");
			if (dot != -1) {
				version = version.substring(0, dot);
			}
		}
		variables.put("JAVA_VERSION", version);

		variables.put("HOSTNAME", InetAddress.getLocalHost().getHostName());
		variables.put("OS", System.getProperty("os.name"));
		List<String> ips = new ArrayList<>();

		for (InetAddress ip : GeneralUtils.getIPsIncludingLoopback()) {
			ips.add(ip.getHostAddress());
		}
		variables.put("IPs", ips);

		super.handle(context);
	}
}
