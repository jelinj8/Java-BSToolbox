package cz.bliksoft.javautils.freemarker.includes;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;

/**
 * Provides access to the Freemarker templates bundled inside this library. Use
 * {@link #getTemplateLoader(TemplateLoader)} to compose the built-in loader
 * with an application-level loader so that application templates take
 * precedence.
 */
public class BuiltinTemplateLoader {

	private BuiltinTemplateLoader() {

	}

	private static TemplateLoader builtin = new ClassTemplateLoader(BuiltinTemplateLoader.class,
			"/cz/bliksoft/javautils/freemarker/includes");

	/**
	 * Returns the raw built-in {@link freemarker.cache.TemplateLoader} for the
	 * library's bundled templates.
	 */
	public static TemplateLoader getBuiltinTemplateLoader() {
		return builtin;
	}

	/**
	 * Creates a {@link freemarker.cache.MultiTemplateLoader} that tries
	 * {@code templateLoader} first, falling back to the built-in templates.
	 *
	 * @param templateLoader application-level template loader (takes precedence)
	 * @return composite loader
	 */
	public static TemplateLoader getTemplateLoader(TemplateLoader templateLoader) {
		return new MultiTemplateLoader(new TemplateLoader[] { templateLoader, builtin });
	}
}
