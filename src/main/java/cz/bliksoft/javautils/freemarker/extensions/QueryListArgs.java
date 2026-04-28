package cz.bliksoft.javautils.freemarker.extensions;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import cz.bliksoft.javautils.database.IDBConnectionProvider;
import cz.bliksoft.javautils.freemarker.extensions.query.IQueryProvider;
import freemarker.template.TemplateModelException;

/**
 * Call a SQL query. Sets LastQuery hashmap with. [columns, columnTypes, SQL,
 * parameters, resultCount]
 * 
 * @author jelinj8
 *
 */
public class QueryListArgs extends Query {

	Logger log = Logger.getLogger(QueryListArgs.class.getName());

	public QueryListArgs(IDBConnectionProvider connectionProvider, IQueryProvider queryProvider) {
		super(connectionProvider, queryProvider, false);
	}

	public QueryListArgs(IDBConnectionProvider connectionProvider, IQueryProvider queryProvider, boolean iterable) {
		super(connectionProvider, queryProvider, iterable);
	}

	@SuppressWarnings("rawtypes")
	public Object exec(List args) throws TemplateModelException {
		if (!args.isEmpty()) {
			List<Object> newArgs = new LinkedList<>();
			newArgs.add(args.get(0));
			Object argsList = args.get(1);
			if (argsList != null) {
				@SuppressWarnings("unchecked")
				Iterable<Object> i = (Iterable<Object>) argsList;
				i.forEach(a -> newArgs.add(a));
			}
			return super.exec(newArgs);
		} else {
			throw new TemplateModelException(
					"First parameter must be a Query identifier, optional second parameter an iterable list of arguments!");
		}
	}

}
