package cz.bliksoft.javautils.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

public class SingleSharedConnectionProvider implements IDBConnectionProvider {
	Logger log = Logger.getLogger(SingleSharedConnectionProvider.class.getName());

	private Connection connection = null;
	private IDBConnectionFactory connectionProvider = null;
	private Object lock;
	private String currentReason;

	private Semaphore semaphore = new Semaphore(1);

	/**
	 * create instance with ready connection
	 * 
	 * @param connection
	 */
	public SingleSharedConnectionProvider(Connection connection) {
		this.connection = connection;
	}

	/**
	 * create instance with ready connection
	 * 
	 * @param connection
	 */
	public SingleSharedConnectionProvider(Connection connection, String name) {
		this.connection = connection;
		this.providerName = name;
	}

	/**
	 * create lazily fetched connection
	 * 
	 * @param connectionProvider
	 */
	public SingleSharedConnectionProvider(IDBConnectionFactory connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	/**
	 * create lazily fetched connection
	 * 
	 * @param connectionProvider
	 */
	public SingleSharedConnectionProvider(IDBConnectionFactory connectionProvider, String name) {
		this.connectionProvider = connectionProvider;
		this.providerName = name;
	}

	@Override
	public Connection getConnection(Object lockObject, String reason) throws Exception {
		if (connection == null && connectionProvider != null) {
			if (providerName == null)
				log.info("Creating DB connection");
			else
				log.info("Creating DB connection '" + providerName + "'");
			connection = connectionProvider.getConnection(reason);
		}
		if (connection == null)
			throw new Exception(
					"Requested a connection without providing underlying connection or connectionProvider.");
		if (connection.isClosed())
			throw new Exception("Shared connection is closed");
		if (semaphore.tryAcquire()) {
			lock = lockObject;
			currentReason = reason;
			return connection;
		} else {
			throw new Exception("Shared connection is already claimed!");
		}
	}

	@Override
	public void releaseConnection(Object lockObject) {
		if (lockObject != lock)
			throw new RuntimeException("Not matching lock object, locking reason: " + currentReason);
		if (semaphore.availablePermits() > 0) {
			return;
		}
		lock = null;
		currentReason = null;
		semaphore.release();
	}

	@Override
	public void close() throws IOException {
		if (connection != null) {
			if (providerName == null)
				log.info("Closing DB connection provider");
			else
				log.info("Closing DB connection provider '" + providerName + "'");

			try {
				connection.close();
			} catch (SQLException e) {
				throw new IOException("Failed to close shared connection", e);
			}
		} else {
			if (providerName == null)
				log.fine("Closing UNINITIALIZED DB connection provider");
			else
				log.fine("Closing UNINITIALIZED DB connection provider'" + providerName + "'");
		}
	}

	private String providerName = null;

	@Override
	public void setName(String name) {
		this.providerName = name;
	}

	@Override
	public String getName() {
		return providerName;
	}

}
