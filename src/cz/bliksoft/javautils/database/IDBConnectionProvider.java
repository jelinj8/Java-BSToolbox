package cz.bliksoft.javautils.database;

import java.io.Closeable;
import java.sql.Connection;

public interface IDBConnectionProvider extends Closeable {
	public Connection getConnection(Object lockObject) throws Exception;

	public void releaseConnection(Object lockObject);

	public void setName(String name);

	public String getName();

}
