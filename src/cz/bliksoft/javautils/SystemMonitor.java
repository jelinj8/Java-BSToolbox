package cz.bliksoft.javautils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.javautils.binding.list.collections.LimitedList;
import cz.bliksoft.javautils.freemarker.FreemarkerGenerator;
import cz.bliksoft.javautils.freemarker.includes.BuiltinTemplateLoader;
import cz.bliksoft.javautils.logging.LogUtils;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class SystemMonitor {
	private static final Logger log = Logger.getLogger(SystemMonitor.class.getName());

	public static final int maxHistory = 240;
	private static LimitedList<SystemReport> systemReports = new LimitedList<>(maxHistory);

	public static final long systemReportPause = 1000 * 10;

	public static final int mb = 1024 * 1024;

	private static List<ThreadInfo> threadInfo = new ArrayList<>();
	private static Map<Long, Long> totalThreadCpuNanos = new HashMap<>();
	private static Map<Long, Long> lastThreadCpuMillis = new HashMap<>();

	private static Thread monitorThread = null;
	private static Map<String, Object> baseVariables = new HashMap<>();

	private static boolean keepThreads = false;

	public static long startupTimestamp = (new Date()).getTime();

	public static class SystemReport {
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
					long previousNanos = totalThreadCpuNanos.getOrDefault(threadID, 0l);
					long nanos = threadNanos - previousNanos;
					threadCpuMillis.put(threadID, nanos / 1000);
					cpuNanos += nanos;
				}
			}

			totalThreadCpuNanos = newThreadCpuNanos;
			lastThreadCpuMillis = threadCpuMillis;

			if (keepThreads)
				for (ThreadInfo ti : threadInfo) {
					Long threadID = ti.getThreadId();
					if (!newThreadInfo.containsKey(threadID)) {
						newThreadInfo.put(threadID, ti);
						threadCpuMillis.put(threadID, -1l);
					}
				}

			threadInfo = new ArrayList<>(newThreadInfo.values());
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

	public static Map<Long, Long> getThreadCpuNanos() {
		return totalThreadCpuNanos;
	}

	/**
	 * return copy of current threadInfo list
	 * 
	 * @return
	 */
	public static List<ThreadInfo> getThreadInfo() {
		return new ArrayList<>(threadInfo);
	}

	public static Map<Long, Long> getLastThreadCpuMillis() {
		return lastThreadCpuMillis;
	}

	public static Map<String, Object> getVariables() {
		Map<String, Object> currentVariables = new HashMap<>();
		currentVariables.putAll(baseVariables);

		SystemReport rep = new SystemReport();

		currentVariables.put("MEMORY_FREE", rep.getFree() / mb);
		currentVariables.put("MEMORY_USED", rep.getUsed() / mb);
		currentVariables.put("MEMORY_TOTAL", rep.getTotal() / mb);
		currentVariables.put("MEMORY_MAX", rep.getMax() / mb);

		List<SystemReport> reports;

		synchronized (systemReports) {
			reports = new ArrayList<SystemReport>(systemReports);
		}
		List<LocalDateTime> timestamps = new ArrayList<>();

		List<Map<String, Object>> memoryReports = new ArrayList<>();
		List<Map<String, Object>> cpuReports = new ArrayList<>();

		List<ThreadInfo> ti = getThreadInfo();

		for (SystemReport r : reports) {
			timestamps.add(r.timestamp);

			Map<String, Object> rMap = new HashMap<>();
			rMap.put("FREE", r.freeMemory);
			rMap.put("USED", r.usedMemory);
			rMap.put("TOTAL", r.totalMemory);
			rMap.put("MAX", r.maxMemory);
			memoryReports.add(rMap);

			Map<String, Object> cMap = new HashMap<>();
			ti.forEach(info -> {
				long id = info.getThreadId();
				cMap.put(id + ":" + info.getThreadName(), r.getThreadCpuMillis().getOrDefault(id, 0l) / 1000l);
			});

			cpuReports.add(cMap);
		}

		currentVariables.put("TS", timestamps);
		currentVariables.put("MEMORY", memoryReports);
		currentVariables.put("CPU", cpuReports);

		currentVariables.put("cpuNanos", rep.getCpuNanos());

		currentVariables.put("env", EnvironmentUtils.tryGetEnvironmentProperties());
		currentVariables.put("messages", LogUtils.getMessages());
		currentVariables.put("startupTime", startupTimestamp);

		Map<Long, Long> nanos = getLastThreadCpuMillis();

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
			currentVariables.put("threads", threads);
		} else {
			Map<Long, Map<String, String>> threads = new HashMap<>(0);
			currentVariables.put("threads", threads);
		}

		return currentVariables;
	}

	public static void startSystemMonitor() {
		if (monitorThread != null)
			return;

		String version = System.getProperty("java.version");

		if (version.startsWith("1.")) {
			version = version.substring(2, 3);
		} else {
			int dot = version.indexOf(".");
			if (dot != -1) {
				version = version.substring(0, dot);
			}
		}
		baseVariables.put("JAVA_VERSION", version);

		try {
			baseVariables.put("HOSTNAME", InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			log.severe("Failed to get hostname: " + e.getMessage());
		}
		baseVariables.put("OS", System.getProperty("os.name"));
		baseVariables.put("msBetweenSamples", systemReportPause);

		List<String> ips = new ArrayList<>();
		try {
			for (InetAddress ip : GeneralUtils.getIPsIncludingLoopback()) {
				ips.add(ip.getHostAddress());
			}
		} catch (SocketException e) {
			log.severe("Failed to get system IP list: " + e.getMessage());
		}
		baseVariables.put("IPs", ips);

		monitorThread = new Thread(new Runnable() {
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
		monitorThread.setDaemon(true);
		monitorThread.start();

	}

	public static void writeSystemReport(OutputStream os) {
		String path = "systemstatus.ftlh";

		FreemarkerGenerator generator = null;
		Template template = null;
		try {
			List<TemplateLoader> loaders = new ArrayList<>(2);
			loaders.add(BuiltinTemplateLoader.getBuiltinTemplateLoader());
			loaders.add(new ClassTemplateLoader(BuiltinTemplateLoader.class, "builtin"));

			generator = new FreemarkerGenerator(new MultiTemplateLoader(loaders.toArray(new TemplateLoader[] {})));

			try {
				generator.setVariable("environment", EnvironmentUtils.getEnvironmentProperties());
			} catch (Exception e) {
				log.fine("EnvironmentUtils not initialized.");
			}

			generator.setVariables(getVariables());

		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to create FreemarkerGenerator.", e);
			return;
		}

		try {
			template = generator.getTemplate(path);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to get template.", e);
			return;
		}

		try {
			generator.generate(os, template);
		} catch (TemplateException e) {
			log.log(Level.SEVERE, "Failed to process template", e);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to write template to output", e);
		}
	}

	public static void logSystemReport(String name) {
		logSystemReport(name, null);
	}

	public static void logSystemReport(String name, String messagesName) {
		if (monitorThread == null) {
			log.warning("System monitor not started, nothing to be reported.");
			return;
		}

		LogUtils.addMessage("save report: " + name);
		File reportFile = LogUtils.getFile(name, "html");
		log.info("Writing system report to file " + reportFile.toString());
		try (FileOutputStream fos = new FileOutputStream(reportFile)) {
			writeSystemReport(fos);
		} catch (IOException e) {
			log.severe("Failed to open file for system report.");
		}

		if (messagesName != null) {
			List<TimestampedObject<Object>> messages = new ArrayList<>(LogUtils.getMessages());
			if (messages.size() > 0) {
				File messagesFile = LogUtils.getFile(messagesName, "txt");
				try (FileWriter fw = new FileWriter(messagesFile)) {
					for (TimestampedObject<Object> msg : messages) {
						fw.write(MessageFormat.format("{0}|{1}\n", DateUtils.timestampFormat.format(msg.timestamp),
								msg.value));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void setKeepDeadThreads(boolean keep) {
		keepThreads = keep;
	}

}
