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
public class MySQLConnection extends AbstractDBConnection {
	private static final String driverName = "com.mysql.cj.jdbc.Driver";

	private static Class<?> driver = null;

	private static MySQLConnection singletonInstance = null;
	private static File globalPropertiesFile;

	public static MySQLConnection getInstance() throws Exception {
		if (singletonInstance == null) {
			if (globalPropertiesFile == null)
				throw new Exception("setPropertiesFile not called!");
			singletonInstance = new MySQLConnection(globalPropertiesFile);
			singletonInstance.init();
		}
		return singletonInstance;
	}

	public static void setPropertiesFile(File propertiesFile) {
		globalPropertiesFile = propertiesFile;
	}

	public MySQLConnection(File propertiesFile) throws Exception {
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
		String dbUrl = "jdbc:mysql:"; //$NON-NLS-1$

		dbUrl += "//" + dbAddr + ":" + dbServerPort + "/" + dbName; //$NON-NLS-1$ //$NON-NLS-2$
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
