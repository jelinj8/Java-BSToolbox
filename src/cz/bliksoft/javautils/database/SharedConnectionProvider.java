package cz.bliksoft.javautils.database;

import java.sql.Connection;

public class SharedConnectionProvider implements IDBConnectionProvider {

	private Connection connection;
	private Object lock;

	public SharedConnectionProvider(Connection connection) {
		this.connection = connection;
	}

	@Override
	public Connection getConnection(Object lockObject) throws Exception {
		if (this.lock != null && this.lock != lockObject) {
			throw new Exception("Shared connection is already locked by other consumer!");
		}
		this.lock = lockObject;
		if (connection.isClosed())
			throw new Exception("Shared connection is closed");
		return connection;
	}

	@Override
	public void release(Object lockObject) {
		if (this.lock != null && this.lock != lockObject) {
			;
		} else {
			lock = null;
		}
	}

}
