package cz.bliksoft.javautils.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import cz.bliksoft.javautils.streams.NoCloseOutputStream;
import cz.bliksoft.javautils.xml.XmlUtils;
//import jakarta.xml.bind.JAXBException;

public class LogUtils {

	private static Logger log;

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");

	// private static Properties props;
	private static String logDir = null;
	private static File logDirFile = null;
	private static String logName;

	private static File log4jConfigFile = null;

	public static void setLogName(String name) {
		logName = name;
	}

	public static void initLog4J(File configPath) {
		log4jConfigFile = configPath;
		if (log4jConfigFile == null)
			log4jConfigFile = new File("log4j2.xml");

		if (log4jConfigFile.exists()) {
			System.setProperty("log4j2.configurationFile", (log4jConfigFile.getPath()));

			try {
				Class<?> log4jConfiguratorClass = Class.forName("org.apache.logging.log4j.LogManager");
				if (log4jConfiguratorClass != null) {
					System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

					Method method = log4jConfiguratorClass.getMethod("getLogger");
					org.apache.logging.log4j.Logger logger = (org.apache.logging.log4j.Logger) method.invoke(null);

					if (!"org.apache.logging.log4j.jul.LogManager"
							.equals(LogManager.getLogManager().getClass().getName())) {

						logger.warn("Java LogManager instantiated as " + LogManager.getLogManager().getClass().getName()
								+ ", org.apache.logging.log4j.jul.LogManager not in place!");
					}
					logger.info("Log4J initialized with " + log4jConfigFile.getAbsoluteFile());
				}
			} catch (ClassNotFoundException e) {
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	// private static LogUtils _instance;
	public static void init(Properties configuration) {
		File logProps;
		if (configuration != null) {
			logDir = configuration.getProperty("logDir", null);
			if (logDir != null) {
				File f = new File(logDir);
				if (!f.exists()) {
					try {
						f.mkdirs();
					} catch (Exception e) {
						// log.severe("Unable to create log directory " + logDir);
					}
				}

				if (f.exists()) {
					logDir = f.getAbsolutePath();
					logDirFile = f;
				} else {
					logDir = null;
				}
				if (logDir != null)
					System.setProperty("logDir", logDir);
			}
			if (logName != null)
				System.setProperty("logName", logName);
			logProps = new File(configuration.getProperty("loggingProperties", "logging.properties"));
			String log4jConfigName = configuration.getProperty("log4j");
			if (log4jConfigName != null) {
				File log4jConfig = new File(log4jConfigName);
				if (log4jConfig.exists()) {
					initLog4J(log4jConfig);
				} else {
					initLog4J(null);
				}
			}
		} else {
			logProps = new File("logging.properties");
			initLog4J(null);
		}

		if (logProps.exists()) {
			try {
				LogManager.getLogManager().readConfiguration(new FileInputStream(logProps));
			} catch (SecurityException | IOException e) {
				e.printStackTrace();
			}
		} else {
			System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT [%4$s] {%3$s} %5$s%6$s%n");
		}

		log = Logger.getLogger(LogUtils.class.getName());

		if (log4jConfigFile != null && !log4jConfigFile.exists()) {
			log.severe("Missing configured Log4J config file: " + log4jConfigFile.getAbsolutePath());
		}

		if (configuration != null) {
			if ("true".equalsIgnoreCase(configuration.getProperty("logSSL", "false")))
				setSSLLogging();
			if ("true".equalsIgnoreCase(configuration.getProperty("logSOAP", "false")))
				setSOAPLogging();
			if ("true".equalsIgnoreCase(configuration.getProperty("logPKCS11", "false")))
				setPKCSLogging();

			logName = configuration.getProperty("appName");
		}
	}

	public static File getLogDir() {
		return logDirFile;
	}

	// public static void

	public static String getFileName(String name, String extension) {
		if (logDir == null) {
			log.severe("Log dir not set!");
			return null;
		}

		if (extension == null)
			extension = ".log";

		Date curdate = new Date();
		String timestamp = sdf.format(curdate);
		String path = MessageFormat.format("{0}{1}_{2}.{3}", logDir + File.separatorChar, timestamp, name, extension);

		log.fine("Log file: " + path);
		return path;
	}

	public static File getFile(String name, String extension) {
		String fname = getFileName(name, extension);

		if (fname == null)
			return null;
		else
			return new File(fname);
	}

	public static OutputStream createOutputStream(String name, String extension) {
		String fname = getFileName(name, extension);

		if (fname == null)
			return null;
		else {
			OutputStream result = null;
			try {
				result = new FileOutputStream(fname);
			} catch (FileNotFoundException e) {
				log.severe("Error creating log output stream.");
				result = new NoCloseOutputStream(System.out);
			}
			return result;
		}
	}

	public static void logFile(String message, String name, String extension) {

		if (logDir == null)
			return;

		File f = getFile(name, extension);
		// String dirPath = f.getAbsoluteFile().getParentFile().getAbsolutePath();

		try (FileWriter fw = new FileWriter(f)) {
			fw.write(message);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error logging to file " + f, e);
			e.printStackTrace();
		}
	}

	public static void logFile(byte[] message, String name, String extension) {

		if (logDir == null)
			return;

		File f = getFile(name, extension);

		try (FileOutputStream fos = new FileOutputStream(f)) {
			fos.write(message);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error logging to file " + f, e);
			e.printStackTrace();

		}
	}

	public static void logFile(Object annotatedObject, String name, String extension) {

		if (logDir == null)
			return;

		try (OutputStream os = LogUtils.createOutputStream(name, extension)) {
			try (PrintWriter out = new PrintWriter(os, true)) {
				XmlUtils.marshal(annotatedObject, out);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error logging XMLObject file (Marshalling failed)", e);
			e.printStackTrace();
		}
	}

	/**
	 * zapne logování SSL zabezpečení
	 */
	public static void setSSLLogging() {
		System.setProperty("javax.net.debug", "ssl");
	}

	/**
	 * zapne logování PKCS
	 */
	public static void setPKCSLogging() {
		System.setProperty("java.security.debug", "sunpkcs11");
	}

	/**
	 * zapne výpisy SOAP požadavků (i hodně dlouhých)
	 */
	public static void setSOAPLogging() {
		System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
		System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
		System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
		System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");

		System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dumpTreshold", "32760");
	}

	/**
	 * returns string content of Throwable's stack trace
	 * 
	 * @param throwable
	 * @return
	 */
	public static String traceToString(Throwable throwable) {
		StringWriter errors = new StringWriter();
		throwable.printStackTrace(new PrintWriter(errors));
		return errors.toString();
	}

	/**
	 * formats stack trace to a string
	 * 
	 * @param stack
	 * @return
	 */
	public static String traceToString(StackTraceElement[] stack) {
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement e : stack) {
			sb.append(MessageFormat.format("\t{0} ({1}:{2}) [{3}]\n", e.getClassName(), e.getFileName(),
					e.getLineNumber(), e.getMethodName()));
		}
		return sb.toString();
	}

	/**
	 * returns stack trace string representation
	 * 
	 * @param stack
	 * @param skip     count of elements from top to skip
	 * @param maxCount max printed elements count
	 * @return
	 */
	public static String traceToString(StackTraceElement[] stack, int skip, int maxCount) {
		StringBuilder sb = new StringBuilder();
		int counter = (maxCount > 0 ? maxCount : 1000);
		int toSkip = skip;
		for (StackTraceElement e : stack) {
			if (toSkip > 0) {
				toSkip--;
			} else {
				counter--;
				sb.append(MessageFormat.format("\t{0} ({1}:{2}) [{3}]\n", e.getClassName(), e.getFileName(),
						e.getLineNumber(), e.getMethodName()));
			}
			if (counter == 0)
				break;
		}
		return sb.toString();
	}

	/**
	 * returns stack trace string representation
	 * 
	 * @param stack
	 * @param skip     class to skip on top of stack (find first other flass)
	 * @param maxCount max printed elements count
	 * @return
	 */
	public static String traceToString(StackTraceElement[] stack, String skip, int maxCount) {
		StringBuilder sb = new StringBuilder();
		int counter = maxCount;
		boolean endSkippping = skip == null;
		for (StackTraceElement e : stack) {
			if (endSkippping || !e.getClassName().equals(skip)) {
				endSkippping = true;
				counter--;
				sb.append(MessageFormat.format("\t{0} ({1}:{2}) [{3}]\n", e.getClassName(), e.getFileName(),
						e.getLineNumber(), e.getMethodName()));
			}
			if (counter == 0)
				break;
		}
		return sb.toString();
	}

	public static String objectToString(Object o) {
		if (o instanceof Method) {
			Method method = (Method) o;
			StringBuilder sb = new StringBuilder();
			sb.append((method.getReturnType() == null ? "void" : method.getReturnType().getSimpleName()));
			sb.append(" ");
			sb.append(method.getName());
			sb.append("(");
			boolean next = false;
			for (Class<?> cls : method.getParameterTypes()) {
				if (next)
					sb.append(", ");
				sb.append(cls.getSimpleName());
				next = true;
			}
			sb.append(")");

			return sb.toString();
		} else {
			return o.toString();
		}
	}

	/**
	 * get current stack trace, skip the getting call + additional <code>skip</code>
	 * levels.
	 * 
	 * @param maxCount maximal total length of result (0 for unlimited)
	 * @param skip     additional levels to skip (0 for none)
	 * @return
	 */
	public static StackTraceElement[] getStackTrace(int maxCount, int skip) {
		int max = maxCount > 0 ? maxCount : 1000;
		StackTraceElement[] result = Thread.currentThread().getStackTrace();
		return Arrays.copyOfRange(result, 2 + skip, Math.min(result.length - 2, max));
	}

	public static void warnIfNotEDT(String message) {
		if (!SwingUtilities.isEventDispatchThread()) {
			log.severe(message + "\n" + LogUtils.traceToString(LogUtils.getStackTrace(0, 1)));
		}
	}
}
