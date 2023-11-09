package cz.bliksoft.javautils.net.http;

import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpServer;

import cz.bliksoft.javautils.GeneralUtils;
import cz.bliksoft.javautils.binding.list.collections.LimitedList;
import cz.bliksoft.javautils.freemarker.includes.BuiltinTemplateLoader;
import freemarker.cache.ClassTemplateLoader;

@SuppressWarnings("restriction")
public class SystemReportHTTPHandler extends DefaultFreemarkerHTTPHandler {

	private LimitedList<SystemReport> memoryReports = new LimitedList<>(maxHistory);

	private static final Logger log = Logger.getLogger(SystemReportHTTPHandler.class.getName());

	public static final long systemReportPause = 1000 * 10;
	public static final int maxHistory = 240;

	class SystemReport {
		public SystemReport() {
			Runtime instance = Runtime.getRuntime();
			totalMemory = instance.totalMemory();
			freeMemory = instance.freeMemory();
			usedMemory = (instance.totalMemory() - instance.freeMemory());
			maxMemory = instance.maxMemory();
			timestamp = LocalDateTime.now();
		}

		private long totalMemory;
		private long freeMemory;
		private long usedMemory;
		private long maxMemory;

		private LocalDateTime timestamp;

		public long getTotal() {
			return totalMemory;
		}

		public long getFree() {
			return freeMemory;
		}

		public long getUsed() {
			return usedMemory;
		}

		public long getMax() {
			return maxMemory;
		}

		public LocalDateTime getTimestamp() {
			return timestamp;
		}

	};

	public SystemReportHTTPHandler() {
		addSupportedGETPOST();
		setTemplateLoader(new ClassTemplateLoader(BuiltinTemplateLoader.class, "builtin"));

		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					synchronized (memoryReports) {
						memoryReports.add(new SystemReport());
					}

//					log.info("Status report has " + memoryReports.size() + " record(s).");

					try {
						Thread.sleep(systemReportPause);
					} catch (InterruptedException e) {
					}
				}
			}
		});
		t.setDaemon(true);
		t.start();
	}

	public static final int mb = 1024 * 1024;

	@Override
	public void handle(BSHttpContext context) throws IOException {

		Object gbc = context.request.get("collectGarbage");
		boolean collectGarbage = false;
		if (gbc != null) {
			collectGarbage = "true".equals(gbc.toString());
		}

		if (collectGarbage) {
			log.info("Forced GBC");
			System.gc();
		}
		variables.put("GARBAGE", collectGarbage);

		context.requested = "systemstatus.ftlh";

		String version = System.getProperty("java.version");

		if (version.startsWith("1.")) {
			version = version.substring(2, 3);
		} else {
			int dot = version.indexOf(".");
			if (dot != -1) {
				version = version.substring(0, dot);
			}
		}

		SystemReport rep = new SystemReport();

		variables.put("MEMORY_FREE", rep.getFree() / mb);
		variables.put("MEMORY_USED", rep.getUsed() / mb);
		variables.put("MEMORY_TOTAL", rep.getTotal() / mb);
		variables.put("MEMORY_MAX", rep.getMax() / mb);

		List<SystemReport> reports;

		synchronized (memoryReports) {
			reports = new ArrayList<SystemReport>(memoryReports);
		}
		List<Map<String, Object>> memoryReports = new ArrayList<>();
		for (SystemReport r : reports) {
			Map<String, Object> rMap = new HashMap<>();
			rMap.put("TS", r.timestamp);
			rMap.put("FREE", r.freeMemory);
			rMap.put("USED", r.usedMemory);
			rMap.put("TOTAL", r.totalMemory);
			rMap.put("MAX", r.maxMemory);
			memoryReports.add(rMap);
		}

		variables.put("MEMORY", memoryReports);

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

	public static void addEndpoint(HttpServer server) {
		server.createContext("/systeminfo", new SystemReportHTTPHandler());
	}
}
