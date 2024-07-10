package cz.bliksoft.javautils.database;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import cz.bliksoft.javautils.CryptUtils;
import cz.bliksoft.javautils.EnvironmentUtils;
import cz.bliksoft.javautils.PropertiesUtils;
import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.net.ProxySelectorRegistry;

/**
 * {@link IDBConnectionFactory} for MariaDB/MySQL database connections
 */
public class MariaDbConnection implements IDBConnectionFactory {
	static Logger log = Logger.getLogger(MariaDbConnection.class.getName());

	private static MariaDbConnection singletonInstance = null;
	private File propertiesFile;
	private static File globalPropertiesFile;
	private static Class<?> driver = null;
	private Boolean autoCommit = null;

	public static MariaDbConnection getInstance() throws Exception {
		if (singletonInstance == null) {
			if (globalPropertiesFile == null)
				throw new Exception("setPropertiesFile not called!");
			singletonInstance = new MariaDbConnection(globalPropertiesFile);
			singletonInstance.init();
		}
		return singletonInstance;
	}

	public MariaDbConnection(File propertiesFile) throws Exception {
		this.propertiesFile = propertiesFile;
		init();
	}

	public static void setPropertiesFile(File propertiesFile) {
		globalPropertiesFile = propertiesFile;
	}

	private void init() throws ClassNotFoundException, GeneralSecurityException, IOException {
		processOptions();
		if (driver == null) {
			log.fine("Loading JDBC driver.");
			try {
				String driverName = "org.mariadb.jdbc.Driver";// $NON-NLS-1$ //$NON-NLS-1$
				driver = Class.forName(driverName);
				log.info("MariaDB JDBC driver loaded.");
			} catch (ClassNotFoundException e) {
				log.log(Level.SEVERE, "Loading of JDBC driver failed.", e);
				throw new ClassNotFoundException("Class org.mariadb.jdbc.Driver not found.", e);
			}
		}
	}

	public Connection getConnection() throws Exception {
		return getConnection(null);
	}

	@Override
	public Connection getConnection(String reason) throws Exception {
		if (reason == null) {
			StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
			if ("MariaDbConnection.java".equals(ste.getFileName())) {
				ste = Thread.currentThread().getStackTrace()[3];
			}
			reason = MessageFormat.format("{0}:{1} {2}", ste.getFileName(), ste.getLineNumber(), ste.getMethodName());
		}
		String serverString = getMysqlServerString();
		if (log.isLoggable(Level.INFO))
			log.info(MessageFormat.format("Connecting to {0} as {1} ({2})", serverString, dbUserName, reason));
		Connection c = DriverManager.getConnection(serverString, dbUserName, dbPassword);
		if (autoCommit != null)
			c.setAutoCommit(autoCommit);
		return c;
	}

	private String getMysqlServerString() {
		String dbUrl = "jdbc:mariadb:"; //$NON-NLS-1$

		dbUrl += "//" + dbAddr + ":" + dbServerPort + "/" + dbName + "?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true"; //$NON-NLS-1$ //$NON-NLS-2$
		return dbUrl;
	}

	private String dbAddr;
	private Integer dbServerPort;
	private String dbUserName;
	private String dbPassword;
	private String dbName;

	public static final String PROP_CLEAR_PWD = "dbPasswordClear";
	public static final String PROP_PWD = "dbPassword";
	public static final String PROP_PWD_ENC = "dbPassword_enc";

	private void processOptions() throws GeneralSecurityException, IOException {
		Properties properties = PropertiesUtils.loadFromFile(propertiesFile,
				EnvironmentUtils.tryGetAllEnvironmentProperties());

		dbPassword = properties.getProperty(PROP_CLEAR_PWD);
		if (dbPassword == null)
			dbPassword = CryptUtils.getPwdFromProperties(properties, PROP_PWD);

		dbName = properties.getProperty("dbName");
		dbAddr = properties.getProperty("dbAddr");
		dbServerPort = Integer.parseInt(properties.getProperty("dbServerPort", "3306"));
		dbUserName = properties.getProperty("dbUserName");

		ProxySelectorRegistry.addProxyConfiguration(properties);

		if (CryptUtils.passwordRewritten()) {
			try {
				PropertiesUtils.saveProperties(properties, propertiesFile, "saved after encoding PWD", true);
			} catch (Exception e) {
				log.severe("Failed to save encrypted properties: " + e.getMessage());
			}
		}
	}

	@Override
	public void setAutoCommit(Boolean ac) {
		autoCommit = ac;
	}

}
