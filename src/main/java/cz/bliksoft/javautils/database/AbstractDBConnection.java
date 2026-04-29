package cz.bliksoft.javautils.database;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.javautils.CryptUtils;
import cz.bliksoft.javautils.EnvironmentUtils;
import cz.bliksoft.javautils.PropertiesUtils;
import cz.bliksoft.javautils.net.ProxySelectorRegistry;

public abstract class AbstractDBConnection implements IDBConnectionFactory {
	static Logger log = Logger.getLogger(AbstractDBConnection.class.getName());

	protected String dbAddr;
	protected Integer dbServerPort;
	protected String dbUserName;
	protected String dbPassword;
	protected String dbName;
	protected String timezone;

	protected Properties properties;

	protected File propertiesFile;
	protected Boolean autoCommit = null;

	public static final String PROP_USER = "dbUserName";
	public static final String PROP_CLEAR_PWD = "dbPasswordClear";
	public static final String PROP_PWD = "dbPassword";
	public static final String PROP_PWD_ENC = "dbPassword_enc";
	public static final String PROP_TIMEZONE = "dbTimezone";

	public static final String PROP_ADDR = "dbAddr";
	public static final String PROP_PORT = "dbServerPort";
	public static final String PROP_DB = "dbName";

	public Connection getConnection() throws Exception {
		return getConnection(null);
	}

	@Override
	public void setAutoCommit(Boolean ac) {
		autoCommit = ac;
	}

	protected AbstractDBConnection(File propertiesFile) throws Exception {
		this.propertiesFile = propertiesFile;
		init();
	}

	protected abstract String getDriverName();

	/**
	 * Don't fordet to call super! A place to load JDBC driver on first call, with
	 * optional config steps.
	 *
	 * @throws ClassNotFoundException
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	protected void init() throws ClassNotFoundException, GeneralSecurityException, IOException {
		properties = PropertiesUtils.loadFromFile(propertiesFile, EnvironmentUtils.tryGetAllEnvironmentProperties());
		processOptions();
		afterPprocessOptions();
	}

	/**
	 * Load a JDBC driver by classname from {@link #getDriverName()}.
	 *
	 * @return
	 * @throws ClassNotFoundException
	 */
	protected Class<?> loadDriver() throws ClassNotFoundException {
		log.fine("Loading JDBC driver.");
		try {
			Class<?> driver = Class.forName(getDriverName());
			log.info("MySQL JDBC driver loaded.");
			return driver;
		} catch (ClassNotFoundException e) {
			log.log(Level.SEVERE, "Loading of JDBC driver failed.", e);
			throw new ClassNotFoundException("Class " + getDriverName() + " not found.", e);
		}
	}

	/**
	 * default options loading. Called from {@link #init()}.
	 *
	 * @throws GeneralSecurityException
	 */
	protected void processOptions() throws GeneralSecurityException {
		dbPassword = properties.getProperty(PROP_CLEAR_PWD);
		if (dbPassword == null)
			dbPassword = CryptUtils.getPwdFromProperties(properties, PROP_PWD);

		dbName = properties.getProperty(PROP_DB);
		dbAddr = properties.getProperty(PROP_ADDR);
		dbServerPort = Integer.parseInt(properties.getProperty(PROP_PORT, "3306"));
		dbUserName = properties.getProperty(PROP_USER);
	}

	/**
	 * common processes after loading options (called after processOptions from
	 * {@link #init()})
	 *
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	protected void afterPprocessOptions() throws GeneralSecurityException, IOException {
		timezone = properties.getProperty(PROP_TIMEZONE);

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
	public Connection getConnection(String reason) throws Exception {
		if (reason == null) {
			StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
			if ("AbstractDBConnection.java".equals(ste.getFileName())) {
				ste = Thread.currentThread().getStackTrace()[3];
			}
			reason = MessageFormat.format("{0}:{1} {2}", ste.getFileName(), ste.getLineNumber(), ste.getMethodName());
		}
		String serverString = getServerString();
		if (log.isLoggable(Level.INFO))
			log.info(MessageFormat.format("Connecting to {0} as {1} ({2})", serverString, dbUserName, reason));
		Connection c = DriverManager.getConnection(serverString, dbUserName, dbPassword);

		if (autoCommit != null)
			c.setAutoCommit(autoCommit);

		sessionSetup(c);
		return c;
	}

	/**
	 * implement session setup (set timezone if specified + optionally other),
	 * called for each new connection.
	 *
	 * @param c
	 * @throws SQLException
	 */
	protected abstract void sessionSetup(Connection c) throws SQLException;

	/**
	 * override to construct JDBC connection string
	 *
	 * @return
	 */
	protected abstract String getServerString();
}
