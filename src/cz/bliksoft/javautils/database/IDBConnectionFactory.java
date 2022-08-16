package cz.bliksoft.javautils.database;

import java.sql.Connection;

public interface IDBConnectionFactory {
	public Connection getConnection(String reason) throws Exception;
}
