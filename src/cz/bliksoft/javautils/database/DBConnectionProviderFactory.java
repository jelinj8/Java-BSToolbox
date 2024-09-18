package cz.bliksoft.javautils.database;

import java.io.File;
import java.util.Properties;

import cz.bliksoft.javautils.EnvironmentUtils;
import cz.bliksoft.javautils.PropertiesUtils;
import cz.bliksoft.javautils.exceptions.InitializationException;

/**
 * A factory to simplify configuration workflow for multiple database types
 */
public class DBConnectionProviderFactory {

	public static IDBConnectionFactory getConnectionProvider(File config) {
		if (!config.exists())
			throw new InitializationException("Database connection config file " + config + " does not exist.");
		String dbType = null;
		try {
			Properties properties = PropertiesUtils.loadFromFile(config,
					EnvironmentUtils.tryGetAllEnvironmentProperties());
			dbType = properties.getProperty("databaseType");
		} catch (Exception e) {
			throw new InitializationException("Failed to load connection properties file", e);
		}

		if (dbType == null)
			throw new InitializationException("databaseType not specified in connection configuration");

		switch (dbType.toUpperCase()) {
		case "MYSQL":
			try {
				return new MySQLConnection(config);
			} catch (Exception e) {
				throw new InitializationException("Failed to create MySQL connection factory", e);
			}
		case "MARIADB":
			try {
				return new MariaDbConnection(config);
			} catch (Exception e) {
				throw new InitializationException("Failed to create MariaDB connection factory", e);
			}
		case "ORACLE":
			try {
				return new OracleDbConnection(config);
			} catch (Exception e) {
				throw new InitializationException("Failed to create Oracle connection factory", e);
			}
		default:
			throw new InitializationException("Unknown databaseType requested: " + dbType);
		}
	}
}
