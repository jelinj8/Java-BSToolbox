package cz.bliksoft.javautils.net.http;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpServer;

import cz.bliksoft.javautils.EnvironmentUtils;
import cz.bliksoft.javautils.GeneralUtils;
import cz.bliksoft.javautils.binding.list.collections.LimitedList;
import cz.bliksoft.javautils.freemarker.includes.BuiltinTemplateLoader;
import cz.bliksoft.javautils.logging.LogUtils;
import freemarker.cache.ClassTemplateLoader;

@SuppressWarnings("restriction")
public class SystemReportHTTPHandler extends DefaultFreemarkerHTTPHandler {

	private LimitedList<SystemReport> systemReports = new LimitedList<>(maxHistory);

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

			ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

			threadCpuMillis = new HashMap<>();
			Map<Long, Long> newThreadCpuNanos = new HashMap<>();
			Map<Long, ThreadInfo> newThreadInfo = new HashMap<>();

			for (Long threadID : threadMXBean.getAllThreadIds()) {
				ThreadInfo info = threadMXBean.getThreadInfo(threadID);
				newThreadInfo.put(threadID, info);
				Long threadNanos = threadMXBean.getThreadCpuTime(threadID);

				if (threadNanos >= 0) {
					newThreadCpuNanos.put(threadID, threadNanos);
					long previousNanos = SystemReportHTTPHandler.totalThreadCpuNanos.getOrDefault(threadID, 0l);
					long nanos = threadNanos - previousNanos;
					threadCpuMillis.put(threadID, nanos / 1000);
					cpuNanos += nanos;
				}
			}

			SystemReportHTTPHandler.totalThreadCpuNanos = newThreadCpuNanos;
			SystemReportHTTPHandler.lastThreadCpuMillis = threadCpuMillis;
			SystemReportHTTPHandler.threadInfo = new ArrayList<>(newThreadInfo.values());
		}

		private long totalMemory;
		private long freeMemory;
		private long usedMemory;
		private long maxMemory;

		Map<Long, Long> threadCpuMillis;

		private LocalDateTime timestamp;

		private long cpuNanos = 0l;

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

		public long getCpuNanos() {
			return cpuNanos;
		}

		public Map<Long, Long> getThreadCpuMillis() {
			return threadCpuMillis;
		}
	};

	private static List<ThreadInfo> threadInfo = new ArrayList<>();
	private static Map<Long, Long> totalThreadCpuNanos = new HashMap<>();
	private static Map<Long, Long> lastThreadCpuMillis = new HashMap<>();

	public SystemReportHTTPHandler() {
		addSupportedGETPOST();
		setTemplateLoader(new ClassTemplateLoader(BuiltinTemplateLoader.class, "builtin"));

		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					synchronized (systemReports) {
						systemReports.add(new SystemReport());
					}

					try {
						Thread.sleep(systemReportPause);
					} catch (InterruptedException e) {
					}
				}
			}
		}, "BSSystemMonitor");
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

		synchronized (systemReports) {
			reports = new ArrayList<SystemReport>(systemReports);
		}
		List<LocalDateTime> timestamps = new ArrayList<>();

		List<Map<String, Object>> memoryReports = new ArrayList<>();
		List<Map<String, Object>> cpuReports = new ArrayList<>();

		for (SystemReport r : reports) {
			timestamps.add(r.timestamp);

			Map<String, Object> rMap = new HashMap<>();
			rMap.put("FREE", r.freeMemory);
			rMap.put("USED", r.usedMemory);
			rMap.put("TOTAL", r.totalMemory);
			rMap.put("MAX", r.maxMemory);
			memoryReports.add(rMap);

			Map<String, Object> cMap = new HashMap<>();
			getThreadInfo().forEach(info -> {
				long id = info.getThreadId();
				cMap.put(id + ":" + info.getThreadName(), r.getThreadCpuMillis().getOrDefault(id, 0l) / 1000l);
			});

			cpuReports.add(cMap);
		}

		variables.put("TS", timestamps);
		variables.put("MEMORY", memoryReports);
		variables.put("CPU", cpuReports);

		variables.put("cpuNanos", rep.getCpuNanos());

		variables.put("JAVA_VERSION", version);

		variables.put("HOSTNAME", InetAddress.getLocalHost().getHostName());
		variables.put("OS", System.getProperty("os.name"));
		List<String> ips = new ArrayList<>();

		for (InetAddress ip : GeneralUtils.getIPsIncludingLoopback()) {
			ips.add(ip.getHostAddress());
		}
		variables.put("IPs", ips);

		variables.put("env", EnvironmentUtils.tryGetEnvironmentProperties());
		variables.put("messages", LogUtils.getMessages());
		variables.put("msBetweenSamples", systemReportPause);

		Map<Long, Long> nanos = getLastThreadCpuMillis();

		List<ThreadInfo> ti = getThreadInfo();

		if (ti != null) {
			ti.sort(new Comparator<ThreadInfo>() {
				@Override
				public int compare(ThreadInfo o1, ThreadInfo o2) {
					if (o1.getThreadId() < o2.getThreadId())
						return -1;
					else if (o1.getThreadId() > o2.getThreadId())
						return 1;
					else
						return 0;
				}
			});

			List<Map<String, Object>> threads = new ArrayList<>(getThreadInfo().size());
			ti.forEach(info -> {
				long id = info.getThreadId();
				Map<String, Object> t = new HashMap<>();
				t.put("name", info.getThreadName());
				t.put("status", info.getThreadState().toString());
				t.put("millis", nanos.get(id));
				t.put("id", id);
				threads.add(t);
			});
			variables.put("threads", threads);
		} else {
			Map<Long, Map<String, String>> threads = new HashMap<>(0);
			variables.put("threads", threads);
		}

		super.handle(context);
	}

	public static void addEndpoint(HttpServer server) {
		server.createContext("/systeminfo", new SystemReportHTTPHandler());
	}

	public Map<Long, Long> getThreadCpuNanos() {
		return SystemReportHTTPHandler.totalThreadCpuNanos;
	}

	public List<ThreadInfo> getThreadInfo() {
		return SystemReportHTTPHandler.threadInfo;
	}

	public Map<Long, Long> getLastThreadCpuMillis() {
		return SystemReportHTTPHandler.lastThreadCpuMillis;
	}

}
