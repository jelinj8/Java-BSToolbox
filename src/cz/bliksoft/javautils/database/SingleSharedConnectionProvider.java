package cz.bliksoft.javautils.database;

import java.sql.Connection;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

public class SingleSharedConnectionProvider implements IDBConnectionProvider {
	Logger log = Logger.getLogger(SingleSharedConnectionProvider.class.getName());

	private Connection connection;
	private Object lock;

	private Semaphore semaphore = new Semaphore(1);

	public SingleSharedConnectionProvider(Connection connection) {
		this.connection = connection;
	}

	@Override
	public Connection getConnection(Object lockObject) throws Exception {
		if (connection.isClosed())
			throw new Exception("Shared connection is closed");
		if (semaphore.tryAcquire()) {
			lock = lockObject;
			return connection;
		} else {
			throw new Exception("Shared connection is already claimed!");
		}
	}

	@Override
	public void releaseConnection(Object lockObject) {
		if (lockObject != lock)
			throw new RuntimeException("Inapropriate lock object");
		if (semaphore.availablePermits() > 0) {
			return;
		}
		lock = null;
		semaphore.release();
	}

}
