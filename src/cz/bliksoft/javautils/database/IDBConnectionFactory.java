package cz.bliksoft.javautils.database;

import java.sql.Connection;

public interface IDBConnectionFactory {
	/**
	 * get a (new) connection, optionally specifying its purpose for logging 
	 * @param reason
	 * @return
	 * @throws Exception
	 */
	public Connection getConnection(String reason) throws Exception;
	
	void setAutoCommit(Boolean autoCommit);
}
