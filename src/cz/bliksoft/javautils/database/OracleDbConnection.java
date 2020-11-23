package cz.bliksoft.javautils.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Logger;

//import org.apache.commons.cli.Options;

import cz.bliksoft.javautils.CryptUtils;
import cz.bliksoft.javautils.PropertiesUtils;

public class OracleDbConnection {
	static Logger log = Logger.getLogger(OracleDbConnection.class.toString());

	private static OracleDbConnection singletonInstance = null;
	private File propertiesFile;
	private static File globalPropertiesFile;

	public static OracleDbConnection getInstance() throws Exception {
		if (singletonInstance == null) {
			if (globalPropertiesFile == null)
				throw new Exception("setProipertiesFile not called!");
			singletonInstance = new OracleDbConnection(globalPropertiesFile);
			singletonInstance.init();
		}
		return singletonInstance;
	}

	public OracleDbConnection(File propertiesFile) throws Exception {
		this.propertiesFile = propertiesFile;
		init();
	}

	public static void setPropertiesFile(File propertiesFile) {
		globalPropertiesFile = propertiesFile;
	}

	private void init() throws Exception {
		processOptions();
		log.info("Loading OJDBC driver.");

		try {
			String driverName = "oracle.jdbc.driver.OracleDriver";// $NON-NLS-1$ //$NON-NLS-1$
			Class.forName(driverName);
			log.info("OJDBC driver loaded.");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			log.severe("Loading of OJDBC driver failed.");
			throw new Exception("Loading of OJDBC driver failed.");
		}
	}

	public Connection getConnection() throws Exception {
		return getConnection(null);
	}

	public Connection getConnection(String reason) throws Exception {
		if (reason == null) {
			StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
			if ("OracleDbConnection.java".equals(ste.getFileName())) {
				ste = Thread.currentThread().getStackTrace()[3];
			}
			reason = MessageFormat.format("{0}:{1} {2}", ste.getFileName(), ste.getLineNumber(), ste.getMethodName());
		}
		log.info(MessageFormat.format("Connecting to {0} as {1} ({2})", getOraServerString(), oraUserName, reason));
		Connection connection = DriverManager.getConnection(getOraServerString(), oraUserName, oraPassword);
		return connection;
	}

	private String getOraServerString() {
		String dbUrl = "jdbc:oracle:thin:@"; //$NON-NLS-1$

		if ((oraAddr == null) || (oraAddr.length() == 0)) {
			dbUrl += oraDatabase;
		} else {
			if ((oraServerPort == null) || (oraServerPort == 0))
				dbUrl += oraAddr + oraDatabase; // $NON-NLS-1$ //$NON-NLS-2$
			else
				dbUrl += oraAddr + ":" + oraServerPort + ":" + oraDatabase; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return dbUrl;
	}

	private String oraAddr;
	private Integer oraServerPort;
	private String oraUserName;
	private String oraPassword;
	private String oraDatabase;

	private void processOptions() throws GeneralSecurityException {
		Properties properties = PropertiesUtils.loadFromFile(propertiesFile);

		oraPassword = CryptUtils.getPwdFromProperties(properties, "oraPassword");
		oraDatabase = properties.getProperty("oraDatabase");
		oraAddr = properties.getProperty("oraAddr");
		oraServerPort = Integer.parseInt(properties.getProperty("oraServerPort", "1521"));
		oraUserName = properties.getProperty("oraUserName");

		if (CryptUtils.lastPwdModified) {
			try {
				PropertiesUtils.saveProperties(properties, propertiesFile, "saved after encoding PWD", true);
			} catch (Exception e) {
				log.severe("Faiiled to save encrypted properties: " + e.getMessage());
			}
		}
	}
}
