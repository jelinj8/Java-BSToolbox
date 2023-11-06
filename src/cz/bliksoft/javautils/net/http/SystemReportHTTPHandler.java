package cz.bliksoft.javautils.net.http;

import java.io.IOException;
import java.net.InetAddress;

import cz.bliksoft.javautils.freemarker.includes.BuiltinTemplateLoader;

public class SystemReportHTTPHandler extends DefaultFreemarkerHTTPHandler {

	public SystemReportHTTPHandler() {
		addSupportedGETPOST();
		setTemplateLoader(BuiltinTemplateLoader.getBuiltinTemplateLoader());
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

		super.handle(context);
	}
}
