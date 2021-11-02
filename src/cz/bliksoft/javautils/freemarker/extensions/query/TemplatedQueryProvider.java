package cz.bliksoft.javautils.freemarker.extensions.query;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import cz.bliksoft.javautils.freemarker.FreemarkerGenerator;
import cz.bliksoft.javautils.freemarker.extensions.StringCollector;
import freemarker.template.TemplateModelException;

public class TemplatedQueryProvider implements IQueryProvider {
	Logger log = Logger.getLogger(TemplatedQueryProvider.class.getName());

	public static final String TEMPLATE_PARAMETER_PLACEHOLDER = "sqlParameter";

	String basePath;
	FreemarkerGenerator generator;
	HashMap<String, String> sqlCache = null;
	HashMap<String, List<Integer>> partypesCache = null;

	private String lastSql = null;
	private List<Integer> lastParameters = null;

	public TemplatedQueryProvider(FreemarkerGenerator generator, boolean cache) {
		this(generator, null, cache);
	}

	public TemplatedQueryProvider(FreemarkerGenerator generator, String basePath, boolean cache) {
		this.basePath = basePath;
		this.generator = generator;
		if (cache) {
			sqlCache = new HashMap<>();
			partypesCache = new HashMap<>();
		}
	}

	@Override
	public boolean createQuery(String name) throws TemplateModelException {
		if (sqlCache != null && sqlCache.containsKey(name)) {
			lastSql = sqlCache.get(name);
			lastParameters = partypesCache.get(name);
			return true;
		}

		try {
			StringCollector params = new StringCollector("?");
			generator.addExtension(TEMPLATE_PARAMETER_PLACEHOLDER, params);
			lastSql = generator.generate(basePath == null ? name : (basePath + name));
			generator.removeExtension(TEMPLATE_PARAMETER_PLACEHOLDER);

			lastParameters = new ArrayList<>();
			for (String valType : params.getValues()) {
				switch (String.valueOf(valType).toUpperCase()) {
				case "VARCHAR":
					lastParameters.add(Types.VARCHAR);
					break;
				case "DATE":
					lastParameters.add(Types.DATE);
					break;
				case "TIMESTAMP":
					lastParameters.add(Types.TIMESTAMP);
					break;
				case "INTEGER":
					lastParameters.add(Types.INTEGER);
					break;
				case "TINYINT":
					lastParameters.add(Types.TINYINT);
					break;
				case "NUMERIC":
					lastParameters.add(Types.NUMERIC);
					break;
				case "DOUBLE":
					lastParameters.add(Types.DOUBLE);
					break;
				default:
					throw new TemplateModelException("Unsupported SQL parameter type: " + valType);
				}
			}

			if (sqlCache != null) {
				sqlCache.put(name, lastSql);
				partypesCache.put(name, lastParameters);
			}

			return true;
		} catch (Exception e) {
			throw new TemplateModelException("Failed to create query: " + e.getMessage(), e);
		}
	}

	@Override
	public String getSql(String name) {
		return lastSql;
	}

	@Override
	public List<Integer> getArgumentTypes(String name) {
		return lastParameters;
	}

}
