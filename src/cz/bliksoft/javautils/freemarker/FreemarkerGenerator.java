package cz.bliksoft.javautils.freemarker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import cz.bliksoft.javautils.freemarker.extensions.Code128Encode;
import cz.bliksoft.javautils.freemarker.extensions.HtmlPreformat;
import cz.bliksoft.javautils.freemarker.extensions.ImageResource;
import cz.bliksoft.javautils.freemarker.extensions.Regroup;
import cz.bliksoft.javautils.freemarker.extensions.Remap;
import cz.bliksoft.javautils.freemarker.extensions.SwitchTemplate;
import cz.bliksoft.javautils.freemarker.extensions.TextReplacer;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 *
 * @author hroch
 */
public class FreemarkerGenerator {
	private Configuration cfg;

	private static TemplateLoader defaultTemplateLoader = null;

	public static void setDefaultTemplateLoader(TemplateLoader defaultTemplateLoader) {
		FreemarkerGenerator.defaultTemplateLoader = defaultTemplateLoader;
	}

	private void commonInit() {
		cfg = new Configuration(Configuration.VERSION_2_3_30);
		cfg.setEncoding(Locale.getDefault(), "UTF8");
		try {
			Class<?> j8api = Class.forName("no.api.freemarker.java8.Java8ObjectWrapper");
			if (j8api != null) {
				Constructor<?> ctor = j8api.getConstructor(String.class);
				Object object = ctor.newInstance(Configuration.VERSION_2_3_30);
				cfg.setObjectWrapper((ObjectWrapper) object);
			}
		} catch (Exception e) {

		}
	}

	/**
	 * generování pomocí předkonfigurovaného TemplateLoaderu
	 * 
	 * @throws Exception
	 */
	public FreemarkerGenerator() throws Exception {
		if (defaultTemplateLoader == null)
			throw new Exception("Default template loader must be set before first use.");
		commonInit();

		cfg.setTemplateLoader(defaultTemplateLoader);
		cfg.setLocalizedLookup(false);
	}

	/**
	 * generování pomocí šablon z filesystému
	 * 
	 * @throws Exception
	 */
	public FreemarkerGenerator(File basePath) throws Exception {
		commonInit();

		cfg.setTemplateLoader(new FileTemplateLoader(basePath));
		cfg.setLocalizedLookup(false);
	}

	/**
	 * generování pomocí šablon z classpath
	 * 
	 * @param templateLoaderClass
	 * @throws Exception
	 */
	public FreemarkerGenerator(Class<?> templateLoaderClass) {
		commonInit();
		cfg.setClassForTemplateLoading(templateLoaderClass, ".");
	}

	public String generate(String templateName) throws IOException, TemplateException {
		return generate(templateName, null);
	}

	public String generate(String templateName, Object data) throws IOException, TemplateException {

		Template temp = getTemplate(templateName);

		Map<String, Object> root = new HashMap<>();
		registerExtensions(root);
		if (data != null)
			root.put("data", data);
		registerVariables(root);
		StringWriter s = new StringWriter();
		temp.process(root, s);
		return s.toString();
	}

	public void generate(File outfile, String templateName) throws IOException, TemplateException {
		Template temp = getTemplate(templateName);

		Map<String, Object> root = new HashMap<>();
		registerExtensions(root);
		registerVariables(root);
		try (FileOutputStream fos = new FileOutputStream(outfile)) {
			try (OutputStreamWriter out = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) { // $NON-NLS-1$
				temp.process(root, out);
			}
		}
	}

	private Template getTemplate(String templateName) throws IOException {
		Template temp = null;
		temp = cfg.getTemplate(templateName);
		return temp;
	}

	private void registerExtensions(Map<String, Object> root) {
		root.put("formatAsHTML", new HtmlPreformat()); //$NON-NLS-1$
		root.put("switchTemplate", new SwitchTemplate()); //$NON-NLS-1$
		root.put("imgRes", new ImageResource()); //$NON-NLS-1$
		root.put("code128", new Code128Encode()); //$NON-NLS-1$

		root.put("toMap", new Remap());
		root.put("regroup", new Regroup());
		
		root.put("TXTTOHTML", //$NON-NLS-1$
				new TextReplacer("&", "&amp;", "<", "&lt;", ">", "&gt;", "\"", "&quot;", "'", "&#39;", "\n", "<br>\n"));
		root.put("TXTTOHTML_WHITESPACE", new TextReplacer("&", "&amp;", " ", "&nbsp;", "\t", "&nbsp;&nbsp;&nbsp;", "<", //$NON-NLS-1$
				"&lt;", ">", "&gt;", "\"", "&quot;", "'", "&#39;", "\n", "<br>\n"));
		root.put("TXTTOHTML_SAFE", //$NON-NLS-1$
				new TextReplacer("&", "&amp;", "<", "&lt;", ">", "&gt;", "\"", "&quot;", "'", "&#39;"));

		root.putAll(globalExtensions);
		root.putAll(localExtensions);
	}

	private void registerVariables(Map<String, Object> root) {
		for (Entry<String, Object> val : variables.entrySet()) {
			root.put(val.getKey(), val.getValue());
		}
	}

	private static HashMap<String, Object> globalExtensions = new HashMap<>();

	public static void addGlobalExtension(String key, Object ext) {
		globalExtensions.put(key, ext);
	}

	public static void removeGlobalExtension(String key) {
		globalExtensions.remove(key);
	}

	private HashMap<String, Object> localExtensions = new HashMap<>();

	public void addExtension(String key, Object ext) {
		localExtensions.put(key, ext);
	}

	public void removeExtension(String key) {
		localExtensions.remove(key);
	}

	private final HashMap<String, Object> variables = new HashMap<>();

	public void setVariable(String name, Object value) {
		variables.put(name, value);
	}

	public Object getVariable(String name) {
		return variables.get(name);
	}

	public void clearVariables() {
		variables.clear();
	}

}
