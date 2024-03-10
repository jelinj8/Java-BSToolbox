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
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import cz.bliksoft.javautils.EnvironmentUtils;
import cz.bliksoft.javautils.PropertiesUtils;
import cz.bliksoft.javautils.TimestampedObject;
import cz.bliksoft.javautils.binding.list.collections.LimitedList;
import cz.bliksoft.javautils.streams.NoCloseOutputStream;
import cz.bliksoft.javautils.streams.replacer.MapTokenResolver;
import cz.bliksoft.javautils.streams.replacer.TokenReplacingReader;
import cz.bliksoft.javautils.xml.XmlUtils;
//import jakarta.xml.bind.JAXBException;

public class LogUtils {

	private static Logger log;
	private static Logger messageLog;

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");

	// private static Properties props;
	private static String logDir = null;
	private static File logDirFile = null;
	//	private static String logName;

	private static File log4jConfigFile = null;

	private static LimitedList<TimestampedObject<Object>> messages = new LimitedList<>(100);

	public static final String PROP_LOG_SSL = "logSSL";
	public static final String PROP_LOG_SOAP = "logSOAP";
	public static final String PROP_LOG_PKCS11 = "logPKCS11";
	public static final String PROP_LOG_LOG_NAME = "logName";
	public static final String PROP_LOG_APP_NAME = "appName";

	/**
	 * backwards compatibility for {@link LogUtils#initLog4J(File, Map) optional
	 * variable replacement version}
	 * 
	 * @param configPath
	 */
	public static void initLog4J() {
		initLog4J(null, null);
	}

	/**
	 * backwards compatibility for {@link LogUtils#initLog4J(File, Map) optional
	 * variable replacement version}
	 * 
	 * @param configFile
	 */
	public static void initLog4J(File configFile) {
		initLog4J(configFile, null);
	}

	/**
	 * initialize Log4J2 system by a (default) config file
	 * 
	 * @param configPath
	 */
	public static void initLog4J(File configPath, Map<String, String> replacementValues) {
		try {
			Log4j2Utils.init(configPath, replacementValues);
		} catch (IOException e) {
		}
	}

	/**
	 * initialize LogUtils with properties
	 * 
	 * @param configuration
	 */
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
				if (logDir != null) {
					System.setProperty("logDir", logDir);
					EnvironmentUtils.setEnvironmentPropertyIfInitialized(EnvironmentUtils.LOG_DIR, logDir);
				}
			}
			String configuredLogName = configuration.getProperty(PROP_LOG_LOG_NAME);
			if (configuredLogName != null) {
				EnvironmentUtils.setEnvironmentPropertyIfInitialized(PROP_LOG_LOG_NAME, configuredLogName);
			}

			String configuredAppName = configuration.getProperty(PROP_LOG_APP_NAME);
			if (configuredAppName != null) {
				EnvironmentUtils.setEnvironmentPropertyIfInitialized(PROP_LOG_APP_NAME, configuredAppName);
			}

			logProps = new File(configuration.getProperty("loggingProperties", "logging.properties"));
			String log4jConfigName = configuration.getProperty("log4j");
			if (log4jConfigName != null) {
				log4jConfigFile = new File(log4jConfigName);
				if (EnvironmentUtils.isInitialized()) {
					initLog4J(log4jConfigFile, EnvironmentUtils.getEnvironmentProperties());
				} else {
					initLog4J(log4jConfigFile);
				}
			}
		} else {
			logProps = new File("logging.properties");
			if (EnvironmentUtils.isInitialized()) {
				initLog4J(null);
			} else {
				initLog4J(null, EnvironmentUtils.getEnvironmentProperties());
			}
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
		messageLog = Logger.getLogger("SystemMonitorMessages");

		if (log4jConfigFile != null && !log4jConfigFile.exists()) {
			log.severe("Missing configured Log4J config file: " + log4jConfigFile.getAbsolutePath());
		}

		if (configuration != null) {
			if (PropertiesUtils.isTrue(configuration, PROP_LOG_SSL, false))
				setSSLLogging();
			if (PropertiesUtils.isTrue(configuration, PROP_LOG_SOAP, false))
				setSOAPLogging();
			if (PropertiesUtils.isTrue(configuration, PROP_LOG_PKCS11, false))
				setPKCSLogging();
		}
	}

	public static File getLogDir() {
		return logDirFile;
	}

	/**
	 * get logging file name
	 * 
	 * @param name
	 * @param extension
	 * @return
	 */
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

	/**
	 * get logging file
	 * 
	 * @param name
	 * @param extension
	 * @return
	 */
	public static File getFile(String name, String extension) {
		String fname = getFileName(name, extension);

		if (fname == null)
			return null;
		else
			return new File(fname);
	}

	/**
	 * opens an outputStream to be used for logging
	 * 
	 * @param name
	 * @param extension
	 * @return
	 */
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

	/**
	 * writes a text to log file
	 * 
	 * @param message
	 * @param name
	 * @param extension
	 */
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

	/**
	 * logs binary data to a file
	 * 
	 * @param message
	 * @param name
	 * @param extension
	 */
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

	/**
	 * logs a XML annotated object as a XML file
	 * 
	 * @param annotatedObject
	 * @param name
	 * @param extension
	 */
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
	 * turns on logging of SSL security
	 */
	public static void setSSLLogging() {
		System.setProperty("javax.net.debug", "ssl");
	}

	/**
	 * turns on PKCS logging
	 */
	public static void setPKCSLogging() {
		System.setProperty("java.security.debug", "sunpkcs11");
	}

	/**
	 * turns on logging of SOAPMessages (even the very lenghty ones!)
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
	 * @param skip
	 *            count of elements from top to skip
	 * @param maxCount
	 *            max printed elements count
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
	 * @param skip
	 *            class to skip on top of stack (find first other flass)
	 * @param maxCount
	 *            max printed elements count
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

	/**
	 * generic coonversion of object to string (more descriptive than Java
	 * .toString)
	 * 
	 * @param o
	 * @return
	 */
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
	 * @param maxCount
	 *            maximal total length of result (0 for unlimited)
	 * @param skip
	 *            additional levels to skip (0 for none)
	 * @return
	 */
	public static StackTraceElement[] getStackTrace(int maxCount, int skip) {
		int max = maxCount > 0 ? maxCount : 1000;
		StackTraceElement[] result = Thread.currentThread().getStackTrace();
		return Arrays.copyOfRange(result, 2 + skip, Math.min(result.length - 2, max));
	}

	/**
	 * helper tool to log warning if not on EDThread
	 * 
	 * @param message
	 */
	public static void warnIfNotEDT(String message) {
		if (!SwingUtilities.isEventDispatchThread()) {
			log.severe(message + "\n" + LogUtils.traceToString(LogUtils.getStackTrace(0, 1)));
		}
	}

	/**
	 * set count of messages to remember, if smaller than previous, excess messages
	 * will be thrown away. Default 100 messages.
	 * 
	 * @param limit
	 */
	public static void setMessageHistoryLength(int limit) {
		LimitedList<TimestampedObject<Object>> oldMsgs = messages;
		messages = new LimitedList<>(limit);
		oldMsgs.forEach(m -> messages.add(m));
	}

	/**
	 * log message to limited string history
	 * 
	 * @param message
	 */
	public static void addMessage(Object message) {
		synchronized (messages) {
			messages.add(new TimestampedObject<>(message));
		}
		messageLog.info(String.valueOf(message));
	}

	/**
	 * get limited string message history
	 * 
	 * @return
	 */
	public static AbstractCollection<TimestampedObject<Object>> getMessages() {
		return messages;
	}
}
