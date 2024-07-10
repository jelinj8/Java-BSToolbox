package cz.bliksoft.javautils.freemarker.extensions;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.javautils.database.IDBConnectionProvider;
import cz.bliksoft.javautils.freemarker.extensions.query.IQueryProvider;
import freemarker.core.Environment;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * Call a SQL query. Sets LastQuery hashmap with. [columns, columnTypes, SQL,
 * parameters, resultCount]
 * 
 * @author jakub
 *
 */
public class Query implements TemplateMethodModelEx {

	Logger log = Logger.getLogger(Query.class.getName());

	private IDBConnectionProvider connectionProvider = null;
	private IQueryProvider queryProvider = null;

	private long timestamp;

	private boolean iterable;

	public Query(IDBConnectionProvider connectionProvider, IQueryProvider queryProvider) {
		this(connectionProvider, queryProvider, false);
	}

	public Query(IDBConnectionProvider connectionProvider, IQueryProvider queryProvider, boolean iterable) {
		this.connectionProvider = connectionProvider;
		this.queryProvider = queryProvider;
		this.iterable = iterable;
	}

	public class IterableQueryResult implements Iterator<Map<String, Object>>, Closeable {
		PreparedStatement pstmnt;
		ResultSet rs;
		ResultSetMetaData md;
		List<String> colNames;
		long fetchedCount = 0l;
		long timestamp = System.currentTimeMillis();
		String queryID;

		IterableQueryResult(PreparedStatement pstmnt, ResultSet rs, ResultSetMetaData md, List<String> colNames,
				String queryID) {
			this.rs = rs;
			this.md = md;
			this.pstmnt = pstmnt;
			this.colNames = colNames;
			this.queryID = queryID;
		}

		@Override
		public boolean hasNext() {
			try {
				if (rs.isLast()) {
					log.log(Level.INFO,
							MessageFormat.format(
									"Fetched last record of {2}. Total count: {0}, fetched in {1,number,#}ms",
									fetchedCount, System.currentTimeMillis() - timestamp, queryID));
					try {
						close();
					} catch (IOException e) {
						log.log(Level.SEVERE, "Failed to close iterable query result resources", e);
						e.printStackTrace();
					}
					return false;
				} else {
					return true;
				}
			} catch (SQLException e) {
				log.log(Level.SEVERE, "Failed to check rs.next", e);
				return false;
			}
		}

		@Override
		public Map<String, Object> next() {
			try {
				if (rs.next()) {
					fetchedCount++;
					HashMap<String, Object> row = new HashMap<>();
					for (int cID = 1; cID <= md.getColumnCount(); cID++) {
						Object val = getColumnData(rs, md, cID);
						String name = colNames.get(cID - 1);
						row.put(name, val);
					}
					return row;
				} else {
					return null;
				}
			} catch (SQLException | TemplateModelException e) {
				log.log(Level.SEVERE, "Failed to get rs.next", e);
				return null;
			}
		}

		@Override
		public void close() throws IOException {
			try {
				rs.close();
				pstmnt.close();
			} catch (SQLException e) {
				throw new IOException("Failed to close iterable query result", e);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public Object exec(List args) throws TemplateModelException {
		Connection con;

		if (!args.isEmpty()) {
			String queryID = args.get(0).toString();
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, "Preparing Query {0}", queryID);
			try {
				con = connectionProvider.getConnection(this, "Freemarker Query " + queryID);
			} catch (Exception e) {
				throw new TemplateModelException("Failed to get SQL Connection for query " + queryID, e);
			}
			try {
				try {
					queryProvider.createQuery(queryID);
				} catch (Exception e) {
					throw new TemplateModelException("Can't create query '" + queryID + "': " + e.getMessage(), e);
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
						case Types.BIGINT:
							arguments.append("BIGINT");
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

				String query = queryProvider.getSql(queryID);
				List<Object> queryParameters = new ArrayList<>();
				List<String> queryParameterTypes = new ArrayList<>();
				String phase = "setting arguments";
				StringBuilder paramsToPrint = new StringBuilder();
				String argType = null;
				try {
					PreparedStatement pstmnt = con.prepareStatement(query);
					int pID = 1;
					if (queryProvider.getArgumentTypes(queryID).size() > 0) {
						paramsToPrint.append("arguments:");
					} else {
						paramsToPrint.append("NO arguments.");
					}
					for (Integer parType : queryProvider.getArgumentTypes(queryID)) {
						phase = "setting argument " + pID;
						Object val = null;

						switch (parType) {
						case Types.VARCHAR:
							val = args.get(pID).toString();
							argType = "VARCHAR";
							break;
						case Types.DATE:
							val = Date.valueOf(args.get(pID).toString());
							argType = "TIMESTAMP";
							break;
						case Types.TIMESTAMP:
							val = Timestamp.valueOf(args.get(pID).toString());
							argType = "TIMESTAMP";
							break;
						case Types.NUMERIC:
							val = Long.valueOf(args.get(pID).toString());
							argType = "NUMERIC";
							break;
						case Types.TINYINT:
							val = Integer.valueOf(args.get(pID).toString());
							argType = "TINYINT";
							break;
						case Types.INTEGER:
							val = Integer.valueOf(args.get(pID).toString());
							argType = "INTEGER";
							break;
						case Types.BIGINT:
							val = Long.valueOf(args.get(pID).toString());
							argType = "BIGINT";
							break;
						case Types.DOUBLE:
							val = Double.valueOf(args.get(pID).toString());
							argType = "DOUBLE";
							break;
						default:
							val = null;
						}

						if (val == null) {
							pstmnt.setNull(pID, parType);
							paramsToPrint.append("\n\t[" + pID + "] " + argType + " = NULL");
						} else {
							pstmnt.setObject(pID, val, parType);
							paramsToPrint.append("\n\t[" + pID + "] " + argType + " = '" + val + "'");
						}
						queryParameters.add(val);
						queryParameterTypes.add(argType);

						pID++;
					}

					phase = "executing query";
					timestamp = System.currentTimeMillis();

					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE,
								MessageFormat.format(
										"Executing query \nvvv ''{0}'' vvv\n{1}\n----\n{2}\n^^^ QUERY ^^^\n", queryID,
										query, paramsToPrint));
					}
					if (pstmnt.execute()) {
						if (log.isLoggable(Level.FINE))
							log.log(Level.FINE,
									MessageFormat.format("Fetching result for query {0} after {1,number,#}ms", queryID,
											System.currentTimeMillis() - timestamp));
						timestamp = System.currentTimeMillis();
						List<HashMap<String, Object>> result = new ArrayList<>();
						List<String> colNames;
						List<String> colTypes;
						phase = " processing metaddata";

						ResultSet rs = pstmnt.getResultSet();
						// try
						{
							String lastColType;
							ResultSetMetaData md = rs.getMetaData();
							colNames = new ArrayList<>(md.getColumnCount());
							colTypes = new ArrayList<>(md.getColumnCount());

							for (int cID = 1; cID <= md.getColumnCount(); cID++) {
								switch (md.getColumnType(cID)) {
								case Types.INTEGER:
								case Types.TINYINT:
								case Types.SMALLINT:
									lastColType = "INTEGER";
									break;
								case Types.BIGINT:
									lastColType = "LONG";
									break;
								case Types.CHAR:
								case Types.CLOB:
								case Types.VARCHAR:
									lastColType = "STRING";
									break;
								case Types.TIMESTAMP:
									lastColType = "TIMESTAMP";
									break;
								case Types.DATE:
									lastColType = "DATE";
									break;
								case Types.NUMERIC:
								case Types.DECIMAL:
									lastColType = "DECIMAL";
									break;
								case Types.DOUBLE:
								case Types.FLOAT:
									lastColType = "DOUBLE";
									break;
								case Types.LONGVARCHAR:
									lastColType = "STRING";
									break;
								case 2007: // Oracle XMLType
									lastColType = "UNKNOWN";
									// val = "Oracle XMLType, not supported";
									break;
								case Types.SQLXML:
									lastColType = "XML";
									break;
								default:
									lastColType = "UNKNOWN";
									log.log(Level.SEVERE, MessageFormat.format("Unsupported column type: {0} ({1})",
											md.getColumnTypeName(cID), md.getColumnType(cID)));
								}
								colNames.add(md.getColumnName(cID));
								colTypes.add(lastColType);
							}

							Map<String, Object> qParams = new HashMap<>();
							phase = " setting LastQuery vars";
							qParams.put("columns", colNames);
							qParams.put("columnTypes", colTypes);
							qParams.put("SQL", query);
							qParams.put("parameters", queryParameters);
							qParams.put("parameterTypes", queryParameterTypes);

							Environment.getCurrentEnvironment().setVariable("lastQuery",
									Environment.getCurrentEnvironment().getObjectWrapper().wrap(qParams));

							if (iterable) {
								phase = " preparing iterable";
								return new IterableQueryResult(pstmnt, rs, md, colNames, queryID);
							} else {
								phase = " fetching reseult";
								while (rs.next()) {
									HashMap<String, Object> row = new HashMap<>();
									for (int cID = 1; cID <= md.getColumnCount(); cID++) {
										Object val = getColumnData(rs, md, cID);
										String name = md.getColumnName(cID);
										row.put(name, val);
									}
									result.add(row);
								}

								rs.close();
								pstmnt.close();

								if (log.isLoggable(Level.INFO))
									log.log(Level.INFO,
											MessageFormat.format("{2} result count: {0}, fetched in {1,number,#}ms",
													result.size(), System.currentTimeMillis() - timestamp, queryID));
								if (!iterable)
									qParams.put("resultCount", result.size());

								return result;
							}
						}
					} else {
						throw new TemplateModelException("No result");
					}
				} catch (SQLException e) {
					throw new TemplateModelException("SQL Exception.\n" + e.getSQLState() + ": " + e.getMessage()
							+ "\nvvv QUERY vvv\n" + query + "\n----\n" + paramsToPrint + "\n^^^ QUERY ^^^\n", e);
				} catch (Exception e) {
					throw new TemplateModelException(
							"Generic exception while processing query " + queryID + " while " + phase, e);
				}
			} finally {
				connectionProvider.releaseConnection(this);
			}
		} else {
			throw new TemplateModelException("First parameter must be a Query identifier!");
		}
	}

	private Object getColumnData(ResultSet rs, ResultSetMetaData md, int colIndex)
			throws TemplateModelException, SQLException {
		Object val = null;
		switch (md.getColumnType(colIndex)) {
		case Types.INTEGER:
		case Types.TINYINT:
		case Types.SMALLINT:
			val = rs.getInt(colIndex);
			break;
		case Types.BIGINT:
			val = rs.getLong(colIndex);
			break;
		case Types.CHAR:
		case Types.CLOB:
		case Types.VARCHAR:
			val = rs.getString(colIndex);
			break;
		case Types.TIMESTAMP:
			val = rs.getTimestamp(colIndex);
			break;
		case Types.DATE:
			val = rs.getDate(colIndex);
			break;
		case Types.NUMERIC:
		case Types.DECIMAL:
			val = rs.getBigDecimal(colIndex);
			break;
		case Types.DOUBLE:
		case Types.FLOAT:
			val = rs.getDouble(colIndex);
			break;
		case Types.LONGVARCHAR:
			try (Reader rdr = rs.getCharacterStream(colIndex)) {
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
		case 2007: // Oracle XMLType
			val = "Oracle XMLType, not supported";
			// not working
			// try {
			// val = XmlUtils.convertStringToDocument(rs.getString(cID));
			// colType = "XML";
			// } catch (Exception e) {
			// throw new TemplateModelException("XML field read exception.", e);
			// }
			// break;

			// case 2007: // Oracle XMLType, does not work
			// val = String.valueOf(rs.getObject(cID));
			// colType = "XML";
			break;
		case Types.SQLXML:
			try {
				SQLXML x = rs.getSQLXML(colIndex);
				val = x; // .toString();// XmlUtils.convertStringToDocument(rdr);
			} catch (Exception e) {
				throw new TemplateModelException("XML field read exception.", e);
			}
			break;
		default:
			val = "UNKNOWN COL TYPE " + md.getColumnType(colIndex) + ":" + md.getColumnTypeName(colIndex);
		}
		return val;
	}
}
