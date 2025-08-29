package cz.bliksoft.javautils.freemarker.extensions.query;

import java.io.File;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cz.bliksoft.javautils.streams.replacer.ITokenResolver;
import cz.bliksoft.javautils.streams.replacer.TokenReplacingReader;

/**
 * Reads a query from file. Parameter placeholders are in format ${DATATYPE}.
 * Supported types: VARCHAR, DATE, TIMESTAMP, INTEGER, TINYINT, NUMERIC, DOUBLE.
 * Unsupported datatype goes as VARCHAR.
 */
public class FileQueryProvider implements IQueryProvider {

	private final File basePath;

	HashMap<String, String> sqlCache = null;
	HashMap<String, List<Integer>> partypesCache = null;

	public FileQueryProvider(File basePath) {
		this.basePath = basePath;
	}

	class ParamTokenResolver implements ITokenResolver {

		public List<Integer> parameterTypes = new ArrayList<>();

		@Override
		public String resolveToken(String tokenName) {
			switch (String.valueOf(tokenName).toUpperCase()) {
			case "VARCHAR":
				parameterTypes.add(Types.VARCHAR);
				break;
			case "DATE":
				parameterTypes.add(Types.DATE);
				break;
			case "TIMESTAMP":
				parameterTypes.add(Types.TIMESTAMP);
				break;
			case "INTEGER":
				parameterTypes.add(Types.INTEGER);
				break;
			case "TINYINT":
				parameterTypes.add(Types.TINYINT);
				break;
			case "NUMERIC":
				parameterTypes.add(Types.NUMERIC);
				break;
			case "DOUBLE":
				parameterTypes.add(Types.DOUBLE);
				break;
			default:
				parameterTypes.add(Types.VARCHAR);
				break;
			}

			return "?";
		}

	}

	@Override
	public boolean createQuery(String name) throws Exception {
		ParamTokenResolver res = new ParamTokenResolver();
		String querySrc;
		try (TokenReplacingReader rdr = new TokenReplacingReader(new File(basePath, name), res)) {
			querySrc = rdr.readAsString();
		}

		sqlCache.put(name, querySrc);
		partypesCache.put(name, res.parameterTypes);

		return true;
	}

	@Override
	public String getSql(String name) {
		return sqlCache.get(name);
	}

	@Override
	public List<Integer> getArgumentTypes(String name) {
		return partypesCache.get(name);
	}

}
