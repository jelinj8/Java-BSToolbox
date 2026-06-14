package cz.bliksoft.javautils.freemarker.extensions.local;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import cz.bliksoft.javautils.freemarker.FreemarkerGenerator;
import cz.bliksoft.javautils.freemarker.utils.TemplateParameterUtils;
import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * Template-callable {@code ${applyTemplateDefaults()}} function. Fills in
 * default values for variables declared via {@code {var|...}} in the main
 * template's source that are not already set in the current environment. No-op
 * if {@link FreemarkerGenerator#setResolveTemplateDefaults(boolean)} already
 * applied the defaults for this render.
 */
public class ApplyTemplateDefaults implements TemplateMethodModelEx {

	private final FreemarkerGenerator generator;

	public ApplyTemplateDefaults(FreemarkerGenerator generator) {
		this.generator = generator;
	}

	@Override
	public Object exec(@SuppressWarnings("rawtypes") java.util.List args) throws TemplateModelException {
		if (generator.isTemplateDefaultsApplied())
			return "";

		try {
			Environment env = Environment.getCurrentEnvironment();
			Template mainTemplate = env.getMainTemplate();
			String source = mainTemplate != null ? generator.readTemplateSource(mainTemplate.getName()) : null;
			if (source != null) {
				Map<String, Object> defaults = TemplateParameterUtils.extractDefaultVariables(source);
				for (Entry<String, Object> e : defaults.entrySet())
					if (env.getVariable(e.getKey()) == null)
						env.setVariable(e.getKey(), env.getObjectWrapper().wrap(e.getValue()));
			}
		} catch (IOException e) {
			throw new TemplateModelException("Failed to apply template defaults", e);
		} finally {
			generator.markTemplateDefaultsApplied();
		}

		return "";
	}
}
