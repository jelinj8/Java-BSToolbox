package cz.bliksoft.javautils.freemarker.includes;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;

public class BuiltinTemplateLoader {

	private BuiltinTemplateLoader() {

	}

	public static TemplateLoader getTemplateLoader(TemplateLoader templateLoader) {
		return new MultiTemplateLoader(
				new TemplateLoader[] { templateLoader, new ClassTemplateLoader(BuiltinTemplateLoader.class, "./") });
	}
}
