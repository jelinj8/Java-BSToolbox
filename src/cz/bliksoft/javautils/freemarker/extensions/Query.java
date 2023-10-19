package cz.bliksoft.javautils.freemarker.extensions;

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

	public Query(IDBConnectionProvider connectionProvider, IQueryProvider queryProvider) {
		this.connectionProvider = connectionProvider;
		this.queryProvider = queryProvider;
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

				try (PreparedStatement pstmnt = con.prepareStatement(query)) {
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

					if (log.isLoggable(Level.FINE))
						log.log(Level.FINE, "Executing query {0}", queryID);
					if (pstmnt.execute()) {
						if (log.isLoggable(Level.FINE))
							log.log(Level.FINE,
									MessageFormat.format("Fetching result for query {0} after {1,number,#}ms", queryID,
											System.currentTimeMillis() - timestamp));
						timestamp = System.currentTimeMillis();
						List<HashMap<String, Object>> result = new ArrayList<>();
						List<String> colNames = new ArrayList<>();
						List<String> colTypes = new ArrayList<>();
						String colType;
						boolean firstRow = true;
						phase = " fetching reseult";
						try (ResultSet rs = pstmnt.getResultSet()) {
							ResultSetMetaData md = rs.getMetaData();
							while (rs.next()) {
								HashMap<String, Object> row = new HashMap<>();
								for (int cID = 1; cID <= md.getColumnCount(); cID++) {
									Object val = null;
									switch (md.getColumnType(cID)) {
									case Types.INTEGER:
									case Types.TINYINT:
									case Types.SMALLINT:
										colType = "INTEGER";
										val = rs.getInt(cID);
										break;
									case Types.CHAR:
									case Types.CLOB:
									case Types.VARCHAR:
										colType = "STRING";
										val = rs.getString(cID);
										break;
									case Types.TIMESTAMP:
										colType = "TIMESTAMP";
										val = rs.getTimestamp(cID);
										break;
									case Types.DATE:
										colType = "DATE";
										val = rs.getDate(cID);
										break;
									case Types.NUMERIC:
									case Types.DECIMAL:
										colType = "DECIMAL";
										val = rs.getBigDecimal(cID);
										break;
									case Types.DOUBLE:
									case Types.FLOAT:
										colType = "DOUBLE";
										val = rs.getDouble(cID);
										break;
									case Types.LONGVARCHAR:
										try (Reader rdr = rs.getCharacterStream(cID)) {
											colType = "STRING";
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
										colType = "UNKNOWN";
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
											SQLXML x = rs.getSQLXML(cID);
											val = x; // .toString();// XmlUtils.convertStringToDocument(rdr);
											colType = "XML";
										} catch (Exception e) {
											throw new TemplateModelException("XML field read exception.", e);
										}
										break;
									default:
										colType = "UNKNOWN";
										val = "UNKNOWN COL TYPE " + md.getColumnType(cID) + ":"
												+ md.getColumnTypeName(cID);
										log.log(Level.SEVERE, MessageFormat.format("Unsupported column type: {0} ({1})",
												md.getColumnTypeName(cID), md.getColumnType(cID)));
									}
									String name = md.getColumnName(cID);
									row.put(name, val);
									if (firstRow) {
										colNames.add(name);
										colTypes.add(colType);
									}
								}
								result.add(row);
								firstRow = false;
							}
							if (log.isLoggable(Level.INFO))
								log.log(Level.INFO, MessageFormat.format("Result count: {0}, fetched in {1,number,#}ms",
										result.size(), System.currentTimeMillis() - timestamp));
							phase = " setting LastQuery vars";
							Map<String, Object> qParams = new HashMap<>();
							qParams.put("columns", colNames);
							qParams.put("columnTypes", colTypes);
							qParams.put("SQL", query);
							qParams.put("parameters", queryParameters);
							qParams.put("parameterTypes", queryParameterTypes);
							qParams.put("resultCount", result.size());
							// qParams.put("result", result);

							Environment.getCurrentEnvironment().setVariable("lastQuery",
									Environment.getCurrentEnvironment().getObjectWrapper().wrap(qParams));

							return result;
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
}
