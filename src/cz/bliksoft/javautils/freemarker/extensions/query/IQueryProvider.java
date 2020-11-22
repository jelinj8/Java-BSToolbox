package cz.bliksoft.javautils.freemarker.extensions.query;

import java.util.List;

import freemarker.template.TemplateModelException;

public interface IQueryProvider {

	public enum ParamTypes {
		VARCHAR, DATE, TIMESTAMP, INTEGER, DOUBLE
	}

	public boolean createQuery(String name) throws TemplateModelException;

	public String getSql(String name);

	public List<Integer> getArgumentTypes(String name);
}
