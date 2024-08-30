package cz.bliksoft.javautils.database;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * {@link IDBConnectionFactory} for MariaDB/MySQL database connections
 */
public class MariaDbConnection extends AbstractDBConnection {
	private static final String driverName = "org.mariadb.jdbc.Driver";

	private static Class<?> driver = null;

	private static MariaDbConnection singletonInstance = null;
	private static File globalPropertiesFile;

	public static MariaDbConnection getInstance() throws Exception {
		if (singletonInstance == null) {
			if (globalPropertiesFile == null)
				throw new Exception("setPropertiesFile not called!");
			singletonInstance = new MariaDbConnection(globalPropertiesFile);
			singletonInstance.init();
		}
		return singletonInstance;
	}

	public static void setPropertiesFile(File propertiesFile) {
		globalPropertiesFile = propertiesFile;
	}

	public MariaDbConnection(File propertiesFile) throws Exception {
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
		}
	}

	@Override
	protected String getServerString() {
		String dbUrl = "jdbc:mariadb:"; //$NON-NLS-1$

		dbUrl += "//" + dbAddr + ":" + dbServerPort + "/" + dbName //$NON-NLS-1$ //$NON-NLS-2$
				+ "?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true";
		return dbUrl;
	}

	@Override
	protected void sessionSetup(Connection c) throws SQLException {
		if (timezone != null)
			try (PreparedStatement pstmnt = c.prepareStatement("SET time_zone='" + timezone + "'")) {
				pstmnt.execute();
			}
	}
}
