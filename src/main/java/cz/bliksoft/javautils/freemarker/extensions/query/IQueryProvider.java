package cz.bliksoft.javautils.freemarker.extensions.query;

import java.util.List;

public interface IQueryProvider {

	public enum ParamTypes {
		VARCHAR, DATE, TIMESTAMP, INTEGER, DOUBLE
	}

	/**
	 * return true if query creation was succesful
	 *
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public boolean createQuery(String name) throws Exception;

	/**
	 * return SQL text
	 *
	 * @param name
	 * @return
	 */
	public String getSql(String name);

	/**
	 * return SQLTypes list for binding
	 *
	 * @param name
	 * @return
	 */
	public List<Integer> getArgumentTypes(String name);
}
