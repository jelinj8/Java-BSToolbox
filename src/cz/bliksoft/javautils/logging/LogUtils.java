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
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import cz.bliksoft.javautils.streams.NoCloseOutputStream;
import cz.bliksoft.javautils.xml.XmlUtils;

public class LogUtils {

	private static Logger log;

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");

	// private static Properties props;
	private static String logDir = null;
	private static File logDirFile = null;
	private static String appName;

	// private static LogUtils _instance;
	public static void init(Properties configuration) {

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
		}

		File logProps = new File("logging.properties");
		if (logProps.exists()) {
			try {
				LogManager.getLogManager().readConfiguration(new FileInputStream(logProps));
			} catch (SecurityException | IOException e) {
				e.printStackTrace();
			}
		} else {
			System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT [%4$s] {%3$s} %5$s%6$s%n");
		}

		log = Logger.getLogger(LogUtils.class.toString());

		if ("true".equals(configuration.getProperty("logSSL", "false").toLowerCase()))
			setSSLLogging();
		if ("true".equals(configuration.getProperty("logSOAP", "false").toLowerCase()))
			setSOAPLogging();
		if ("true".equals(configuration.getProperty("logPKCS11", "false").toLowerCase()))
			setPKCSLogging();

		appName = configuration.getProperty("appName");
	}

	public static File getLogDir() {
		return logDirFile;
	}

	// public static void

	public static String getFileName(String name, String extension) {
		if (logDir == null)
			return null;

		if (extension == null)
			extension = ".log";

		Date curdate = new Date();
		String timestamp = sdf.format(curdate);
		String path = MessageFormat.format("{0}{1}_{3}.{4}", logDir + File.separatorChar, timestamp,
				(appName == null ? "" : (appName + "_")), name, extension);

		log.info("Log file: " + path);
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
			log.log(Level.INFO, "Error logging to file " + f, e);
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
			log.log(Level.INFO, "Error logging to file " + f, e);
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
		} catch (JAXBException e) {
			log.log(Level.INFO, "Error logging XMLObject file (Marshalling failed)", e);
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

	public static String traceToString(Exception ex) {
		StringWriter errors = new StringWriter();
		ex.printStackTrace(new PrintWriter(errors));
		return errors.toString();
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
}
