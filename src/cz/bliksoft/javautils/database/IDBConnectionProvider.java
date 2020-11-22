package cz.bliksoft.javautils.database;

import java.sql.Connection;

public interface IDBConnectionProvider {
	public Connection getConnection(Object lockObject) throws Exception;
	public void release(Object lockObject);
}
