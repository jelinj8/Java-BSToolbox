package cz.bliksoft.javautils.freemarker;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;

public class BuiltinTemplateLoader {

	private BuiltinTemplateLoader() {

	}

	private static TemplateLoader builtin = new ClassTemplateLoader(BuiltinTemplateLoader.class, "includes");

	public static TemplateLoader getBuiltinTemplateLoader() {
		return builtin;
	}

	public static TemplateLoader getTemplateLoader(TemplateLoader templateLoader) {
		return new MultiTemplateLoader(new TemplateLoader[] { templateLoader, builtin });
	}
}
