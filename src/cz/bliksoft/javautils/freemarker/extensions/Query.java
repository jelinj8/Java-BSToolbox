package cz.bliksoft.javautils.freemarker.extensions;

import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.javautils.database.IDBConnectionProvider;
import cz.bliksoft.javautils.freemarker.extensions.query.IQueryProvider;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class Query implements TemplateMethodModelEx {

	Logger log = Logger.getLogger(Query.class.getName());

	private IDBConnectionProvider connectionProvider = null;
	private IQueryProvider queryProvider = null;

	public Query(IDBConnectionProvider connectionProvider, IQueryProvider queryProvider) {
		this.connectionProvider = connectionProvider;
		this.queryProvider = queryProvider;
	}

	@SuppressWarnings("rawtypes")
	public Object exec(List args) throws TemplateModelException {
		Connection con;

		try {
			con = connectionProvider.getConnection(null);
		} catch (Exception e) {
			throw new TemplateModelException("Failed to get SQL Connection", e);
		}
		try {
			if (!args.isEmpty()) {
				String queryID = args.get(0).toString();
				log.log(Level.INFO, "Executing Query {0}", queryID);
				try {
					queryProvider.createQuery(queryID);
				} catch (Exception e) {
					throw new TemplateModelException("Can't create query " + queryID, e);
				}

				if (queryProvider.getArgumentTypes(queryID).size() != args.size() - 1) {
					StringBuilder arguments = new StringBuilder();
					for (Integer parType : queryProvider.getArgumentTypes(queryID)) {
						if (arguments.length() != 0)
							arguments.append(", ");
						switch (parType) {
						case Types.VARCHAR:
							arguments.append("VARCHAR");
							break;
						case Types.DATE:
							arguments.append("DATE");
							break;
						case Types.TIMESTAMP:
							arguments.append("TIMESTAMP");
							break;
						case Types.TINYINT:
							arguments.append("TINYINT");
							break;
						case Types.INTEGER:
							arguments.append("INTEGER");
							break;
						case Types.NUMERIC:
							arguments.append("NUMERIC");
							break;
						case Types.DOUBLE:
							arguments.append("DOUBLE");
							break;
						default:
							throw new TemplateModelException("Unsupported SQL parameter type " + parType);
						}
					}
					throw new TemplateModelException(
							"Wrong number of query parameters (" + (args.size() - 1) + "), expected "
									+ queryProvider.getArgumentTypes(queryID).size() + ": " + arguments.toString());
				}

				try (PreparedStatement pstmnt = con.prepareStatement(queryProvider.getSql(queryID))) {
					int pID = 1;
					for (Integer parType : queryProvider.getArgumentTypes(queryID)) {
						Object val = null;

						switch (parType) {
						case Types.VARCHAR:
							val = args.get(pID).toString();
							break;
						case Types.DATE:
						case Types.TIMESTAMP:
							val = null;
							break;
						case Types.NUMERIC:
							val = Long.valueOf(args.get(pID).toString());
							break;
						case Types.TINYINT:
							val = Integer.valueOf(args.get(pID).toString());
							break;
						case Types.INTEGER:
							val = Integer.valueOf(args.get(pID).toString());
							break;
						case Types.DOUBLE:
							val = Double.valueOf(args.get(pID).toString());
							break;
						default:
							val = null;
						}

						if (val == null)
							pstmnt.setNull(pID, parType);
						else
							pstmnt.setObject(pID, val, parType);

						pID++;
					}

					if (pstmnt.execute()) {
						List<HashMap<String, Object>> result = new ArrayList<>();
						try (ResultSet rs = pstmnt.getResultSet()) {
							ResultSetMetaData md = rs.getMetaData();
							while (rs.next()) {
								HashMap<String, Object> row = new HashMap<>();
								for (int cID = 1; cID <= md.getColumnCount(); cID++) {
									Object val = null;
									switch (md.getColumnType(cID)) {
									case Types.INTEGER:
										val = rs.getInt(cID);
										break;
									case Types.TINYINT:
										val = rs.getInt(cID);
										break;
									case Types.CHAR:
									case Types.CLOB:
									case Types.VARCHAR:
										val = rs.getString(cID);
										break;
									case Types.TIMESTAMP:
										val = rs.getTimestamp(cID);
										break;
									case Types.DATE:
										val = rs.getDate(cID);
										break;
									case Types.NUMERIC:
										val = rs.getLong(cID);
										break;
									case Types.LONGVARCHAR:
										try (Reader rdr = rs.getCharacterStream(cID)) {
											final int buflen = 8 * 1024;
											char[] arr = new char[buflen];
											StringBuilder buffer = new StringBuilder();
											int numCharsRead;
											while ((numCharsRead = rdr.read(arr, 0, buflen)) != -1) {
												buffer.append(arr, 0, numCharsRead);
											}
											val = buffer.toString();
										} catch (Exception e) {
											throw new TemplateModelException("LONG field read exception.", e);
										}
										break;
									default:
										val = "UNKNOWN COL TYPE " + md.getColumnType(cID) + ":"
												+ md.getColumnTypeName(cID);
										log.log(Level.SEVERE, "Unsupported column type: {0}",
												md.getColumnTypeName(cID));
									}
									String name = md.getColumnName(cID);
									row.put(name, val);
								}
								result.add(row);
							}
							log.log(Level.INFO, "Result count: {0}", result.size());
							return result;
						}
					} else {
						throw new TemplateModelException("No result");
					}
				} catch (SQLException e) {
					throw new TemplateModelException("SQL Exception. " + e.getSQLState(), e);
				} catch (Exception e) {
					throw new TemplateModelException("Generic exception while processing query " + queryID, e);
				}
			} else {
				throw new TemplateModelException("First parameter must be a Query identifier!");
			}

		} finally {
			connectionProvider.release(this);
		}
	}
}
