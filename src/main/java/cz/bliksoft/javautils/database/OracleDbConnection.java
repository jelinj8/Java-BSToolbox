package cz.bliksoft.javautils.database;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.logging.Logger;
import java.util.stream.Stream;

import cz.bliksoft.javautils.CryptUtils;
import cz.bliksoft.javautils.StringUtils;

/**
 * {@link IDBConnectionFactory} for creating Oracle database connections
 */
public class OracleDbConnection extends AbstractDBConnection {
	static Logger log = Logger.getLogger(OracleDbConnection.class.getName());

	private static final String driverName = "oracle.jdbc.driver.OracleDriver";

	private static OracleDbConnection singletonInstance = null;
	private static File globalPropertiesFile;
	private static Class<?> driver = null;

	public static OracleDbConnection getInstance() throws Exception {
		if (singletonInstance == null) {
			if (globalPropertiesFile == null)
				throw new Exception("setPropertiesFile not called!");
			singletonInstance = new OracleDbConnection(globalPropertiesFile);
			singletonInstance.init();
		}
		return singletonInstance;
	}

	public static void setPropertiesFile(File propertiesFile) {
		globalPropertiesFile = propertiesFile;
	}

	public OracleDbConnection(File propertiesFile) throws Exception {
		super(propertiesFile);
	}

	@Override
	protected String getDriverName() {
		return driverName;
	}

	@Override
	protected void init() throws ClassNotFoundException, GeneralSecurityException, IOException {
		super.init();
		if (driver == null) {
			driver = loadDriver();
			System.setProperty("oracle.jdbc.javaNetNio", "false");
		}
	}

	@Override
	protected void sessionSetup(Connection c) throws SQLException {
		if (timezone != null)
			try (PreparedStatement pstmnt = c.prepareStatement("alter session set time_zone='" + timezone + "'")) {
				pstmnt.execute();
			}
	}

	@Override
	protected String getServerString() {
		String dbUrl = "jdbc:oracle:thin:@"; //$NON-NLS-1$

		if (StringUtils.isEmpty(dbAddr)) {
			dbUrl += oraSID;
		} else {
			if ((dbServerPort == null) || (dbServerPort == 0)) {
				if (dbName != null) {
					dbUrl += "//" + dbAddr + "/" + dbName; // $NON-NLS-1$ //$NON-NLS-2$
				} else {
					dbUrl += dbAddr + ":" + oraSID; // $NON-NLS-1$ //$NON-NLS-2$
				}
			} else {
				if (dbName != null)
					dbUrl += "//" + dbAddr + ":" + dbServerPort + "/" + dbName; //$NON-NLS-1$ //$NON-NLS-2$
				else
					dbUrl += dbAddr + ":" + dbServerPort + ":" + oraSID; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return dbUrl;
	}

	private String oraSID;

	public static final String PROP_CLEAR_PWD = "oraPasswordClear";
	public static final String PROP_PWD = "oraPassword";
	public static final String PROP_PORT = "oraServerPort";
	public static final String PROP_PWD_ENC = "oraPassword_enc";

	public static final String PROP_SID = "oraDatabase";
	public static final String PROP_SERVICE = "oraService";
	public static final String PROP_SERVER = "oraAddr";
	public static final String PROP_USER = "oraUserName";

	/**
	 * a bit complicated for backwards compatibilitz + Oracle specific SID/SERVICE
	 * config
	 */
	@Override
	protected void processOptions() throws GeneralSecurityException {
		dbPassword = properties.getProperty(AbstractDBConnection.PROP_CLEAR_PWD,
				properties.getProperty(PROP_CLEAR_PWD));

		if (dbPassword == null)
			dbPassword = CryptUtils.getPwdFromProperties(properties, AbstractDBConnection.PROP_PWD,
					CryptUtils.getPwdFromProperties(properties, PROP_PWD));

		oraSID = properties.getProperty(PROP_SID);
		dbName = properties.getProperty(AbstractDBConnection.PROP_DB, properties.getProperty(PROP_SERVICE));

		dbAddr = properties.getProperty(AbstractDBConnection.PROP_ADDR, properties.getProperty(PROP_SERVER));
		dbServerPort = Integer.parseInt(
				properties.getProperty(AbstractDBConnection.PROP_PORT, properties.getProperty(PROP_PORT, "1521")));
		dbUserName = properties.getProperty(AbstractDBConnection.PROP_USER, properties.getProperty(PROP_USER));
	}

	/**
	 * execute SQL while capturing DBMS_OUTPUT
	 *
	 * @param connection
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
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
}
