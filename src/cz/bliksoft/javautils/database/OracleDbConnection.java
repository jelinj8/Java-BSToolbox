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

public class OracleDbConnection implements IDBConnectionFactory {
	static Logger log = Logger.getLogger(OracleDbConnection.class.getName());

	private static OracleDbConnection singletonInstance = null;
	private File propertiesFile;
	private static File globalPropertiesFile;
	private static Class<?> driver = null;
	private Boolean autoCommit = null;

	public static OracleDbConnection getInstance() throws Exception {
		if (singletonInstance == null) {
			if (globalPropertiesFile == null)
				throw new Exception("setPropertiesFile not called!");
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

	private void init() throws ClassNotFoundException, GeneralSecurityException, IOException {
		processOptions();
		if (driver == null) {
			log.info("Loading OJDBC driver.");
			try {
				String driverName = "oracle.jdbc.driver.OracleDriver";// $NON-NLS-1$ //$NON-NLS-1$
				driver = Class.forName(driverName);
				log.info("OJDBC driver loaded.");
			} catch (ClassNotFoundException e) {
				log.log(Level.SEVERE, "Loading of OJDBC driver failed.", e);
				throw new ClassNotFoundException("Class oracle.jdbc.driver.OracleDriver not found.", e);
			}
			System.setProperty("oracle.jdbc.javaNetNio", "false");
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
		String serverString = getOraServerString();
		if (log.isLoggable(Level.INFO))
			log.info(MessageFormat.format("Connecting to {0} as {1} ({2})", serverString, oraUserName, reason));
		Connection c = DriverManager.getConnection(serverString, oraUserName, oraPassword);
		if (autoCommit != null)
			c.setAutoCommit(autoCommit);
		return c;
	}

	private String getOraServerString() {
		String dbUrl = "jdbc:oracle:thin:@"; //$NON-NLS-1$

		if (StringUtils.isEmpty(oraAddr)) {
			dbUrl += oraSID;
		} else {
			if ((oraServerPort == null) || (oraServerPort == 0)) {
				if (serviceName != null) {
					dbUrl += "//" + oraAddr + "/" + serviceName; // $NON-NLS-1$ //$NON-NLS-2$
				} else {
					dbUrl += oraAddr + ":" + oraSID; // $NON-NLS-1$ //$NON-NLS-2$
				}
			} else {
				if (serviceName != null)
					dbUrl += "//" + oraAddr + ":" + oraServerPort + "/" + serviceName; //$NON-NLS-1$ //$NON-NLS-2$
				else
					dbUrl += oraAddr + ":" + oraServerPort + ":" + oraSID; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return dbUrl;
	}

	private String oraAddr;
	private Integer oraServerPort;
	private String oraUserName;
	private String oraPassword;
	private String oraSID;
	private String serviceName;

	public static final String PROP_CLEAR_PWD = "oraPasswordClear";
	public static final String PROP_PWD = "oraPassword";
	public static final String PROP_PWD_ENC = "oraPassword_enc";

	private void processOptions() throws GeneralSecurityException, IOException {
		Properties properties = PropertiesUtils.loadFromFile(propertiesFile,
				EnvironmentUtils.tryGetAllEnvironmentProperties());

		oraPassword = properties.getProperty(PROP_CLEAR_PWD);
		if (oraPassword == null)
			oraPassword = CryptUtils.getPwdFromProperties(properties, PROP_PWD);

		oraSID = properties.getProperty("oraDatabase");
		serviceName = properties.getProperty("oraService");
		oraAddr = properties.getProperty("oraAddr");
		oraServerPort = Integer.parseInt(properties.getProperty("oraServerPort", "1521"));
		oraUserName = properties.getProperty("oraUserName");

		ProxySelectorRegistry.addProxyConfiguration(properties);

		if (CryptUtils.passwordRewritten()) {
			try {
				PropertiesUtils.saveProperties(properties, propertiesFile, "saved after encoding PWD", true);
			} catch (Exception e) {
				log.severe("Failed to save encrypted properties: " + e.getMessage());
			}
		}
	}

	public static String captureDbmsOut(Connection connection, String sql) throws SQLException {
		StringBuilder sb = new StringBuilder();
		try (Statement s = connection.createStatement()) {
			try {
				s.executeUpdate("begin dbms_output.enable(100000); end;");

				try (CallableStatement call = connection.prepareCall(sql)) {
					call.execute();
				}

				try (CallableStatement call = connection
						.prepareCall("declare num integer := 10000; begin dbms_output.get_lines(?, num); end;")) {
					call.registerOutParameter(1, Types.ARRAY, "DBMSOUTPUT_LINESARRAY");
					call.execute();

					Array array = null;
					try {
						array = call.getArray(1);
						Stream.of((Object[]) array.getArray()).forEach(a -> {
							if (a != null)
								sb.append(a);
							sb.append("\n");
						});
					} finally {
						if (array != null)
							array.free();
					}
				}
			} finally {
				s.executeUpdate("begin dbms_output.disable(); end;");
			}
		}
		return sb.toString();
	}

	public void setAutoCommit(Boolean ac) {
		autoCommit = ac;
	}

}
