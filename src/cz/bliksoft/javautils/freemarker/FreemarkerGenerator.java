package cz.bliksoft.javautils.freemarker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import cz.bliksoft.javautils.freemarker.extensions.TextReplacer;
import cz.bliksoft.javautils.freemarker.extensions.global.Code128Encode;
import cz.bliksoft.javautils.freemarker.extensions.global.Code128Width;
import cz.bliksoft.javautils.freemarker.extensions.global.GUIPrompt;
import cz.bliksoft.javautils.freemarker.extensions.global.HtmlPreformat;
import cz.bliksoft.javautils.freemarker.extensions.global.IdentifyObjectType;
import cz.bliksoft.javautils.freemarker.extensions.global.ImageResource;
import cz.bliksoft.javautils.freemarker.extensions.global.ParseXml;
import cz.bliksoft.javautils.freemarker.extensions.global.PrettyPrintXml;
import cz.bliksoft.javautils.freemarker.extensions.global.Regroup;
import cz.bliksoft.javautils.freemarker.extensions.global.Reindex;
import cz.bliksoft.javautils.freemarker.extensions.local.AnchorNumberer;
import cz.bliksoft.javautils.freemarker.extensions.local.VariableRegistrator;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 *
 * @author hroch
 */
public class FreemarkerGenerator {
	Logger log = Logger.getLogger(FreemarkerGenerator.class.getName());

	private Configuration cfg;

	private static TemplateLoader defaultTemplateLoader = null;

	public static void setDefaultTemplateLoader(TemplateLoader defaultTemplateLoader) {
		FreemarkerGenerator.defaultTemplateLoader = defaultTemplateLoader;
	}

	public static void setDefaultTemplateLoader(File templateRootFolder) throws IOException {
		FreemarkerGenerator.defaultTemplateLoader = new FileTemplateLoader(templateRootFolder);
	}

	public static void setDefaultTemplateLoader(Class<?> templateClassNeighbour) {
		FreemarkerGenerator.defaultTemplateLoader = new ClassTemplateLoader(templateClassNeighbour, "");
	}

	private static boolean skipJ8TimeAPI = false;
	private static Object java8TimeAPIWrapper = null;
	private boolean storeEnvironment = false;
	private Environment lastEnvironment = null;

	public void storeEnvironment() {
		this.storeEnvironment = true;
	}

	public Environment getLastEnvironment() {
		return lastEnvironment;
	}

	public Configuration getConfiguration() {
		return cfg;
	}

	private void commonInit() {
		cfg = new Configuration(Configuration.VERSION_2_3_30);
		cfg.setEncoding(Locale.getDefault(), "UTF8");
		setNumberFormat(NumberFormats.COMPUTER);
		if (!skipJ8TimeAPI) {
			if (java8TimeAPIWrapper == null) {
				try {
					Class<?> j8api = Class.forName("no.api.freemarker.java8.Java8ObjectWrapper");
					if (j8api != null) {
						Constructor<?> ctor = j8api.getConstructor(String.class);
						java8TimeAPIWrapper = ctor.newInstance(Configuration.VERSION_2_3_30);
						log.info("no.api.freemarker.java8.Java8ObjectWrapper loaded, registering ObjectWrapper");
					}
				} catch (ClassNotFoundException e) {
					log.info("no.api.freemarker.java8.Java8ObjectWrapper not present");
					skipJ8TimeAPI = true;
				} catch (Exception e) {
					;
				}
			}
			if (java8TimeAPIWrapper != null)
				cfg.setObjectWrapper((ObjectWrapper) java8TimeAPIWrapper);
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
	}

	/**
	 * generování pomocí TemplateLoaderu
	 * 
	 * @param templateLoader
	 * @throws Exception
	 */
	public FreemarkerGenerator(TemplateLoader templateLoader) {
		commonInit();
		cfg.setTemplateLoader(templateLoader);
	}

	/**
	 * generování pomocí TemplateLoaderu
	 * 
	 * @param templateLoader
	 * @throws Exception
	 */
	public FreemarkerGenerator(TemplateLoader... templateLoaders) {
		commonInit();
		cfg.setTemplateLoader(new MultiTemplateLoader(templateLoaders));
	}

	/**
	 * generování pomocí šablon z filesystému
	 * 
	 * @throws Exception
	 */
	public FreemarkerGenerator(File basePath) throws Exception {
		commonInit();

		cfg.setTemplateLoader(new FileTemplateLoader(basePath));
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

	/**
	 * generování pomocí šablon z filesystému a z classpath
	 * 
	 * @param templateLoaderClass
	 * @throws IOException
	 */
	public FreemarkerGenerator(File templatesBasePath, Class<?> templateLoaderClass) throws IOException {
		commonInit();
		MultiTemplateLoader mtl = new MultiTemplateLoader(new TemplateLoader[] {
				new FileTemplateLoader(templatesBasePath), new ClassTemplateLoader(templateLoaderClass, "") });
		cfg.setTemplateLoader(mtl);
	}

	public void disableLocalizedTemplateLookup() {
		cfg.setLocalizedLookup(false);
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
		if (storeEnvironment) {
			lastEnvironment = temp.createProcessingEnvironment(root, s);
			lastEnvironment.process(); // process the template
		} else {
			temp.process(root, s);
		}
		return s.toString();
	}

	public void generate(File outfile, String templateName) throws IOException, TemplateException {
		try (FileOutputStream fos = new FileOutputStream(outfile)) {
			generate(fos, templateName);
		}
	}

	public void generate(OutputStream out, String templateName) throws IOException, TemplateException {
		Template temp = getTemplate(templateName);

		Map<String, Object> root = new HashMap<>();
		registerExtensions(root);
		registerVariables(root);
		generate(out, temp, root);
	}

	private void generate(OutputStream outfile, Template tpl, Map<String, Object> root)
			throws IOException, TemplateException {
		try (OutputStreamWriter out = new OutputStreamWriter(outfile, StandardCharsets.UTF_8)) {
			if (storeEnvironment) {
				lastEnvironment = tpl.createProcessingEnvironment(root, out);
				lastEnvironment.process(); // process the template
			} else {
				tpl.process(root, out);
			}
		}
	}

	private Template getTemplate(String templateName) throws IOException {
		Template temp = null;
		temp = cfg.getTemplate(templateName);
		return temp;
	}

	private static Map<String, Object> getDefaultGlobalExtensions() {
		Map<String, Object> res = new HashMap<>();
		res.put("formatAsHTML", new HtmlPreformat()); //$NON-NLS-1$
		res.put("identifyObjectType", new IdentifyObjectType()); //$NON-NLS-1$
		res.put("imgRes", new ImageResource()); //$NON-NLS-1$
		res.put("code128", new Code128Encode()); //$NON-NLS-1$
		res.put("code128width", new Code128Width()); //$NON-NLS-1$

		res.put("regroup", new Regroup()); //$NON-NLS-1$
		res.put("reindex", new Reindex()); //$NON-NLS-1$
		res.put("prettyXML", new PrettyPrintXml()); //$NON-NLS-1$
		res.put("parseXML", new ParseXml()); //$NON-NLS-1$

		res.put("GUIPrompt", new GUIPrompt()); //$NON-NLS-1$

		res.put("TXTTOHTML", //$NON-NLS-1$
				new TextReplacer("&", "&amp;", "<", "&lt;", ">", "&gt;", "\"", "&quot;", "'", "&#39;", "\n", "<br>\n"));
		res.put("TXTTOHTML_WHITESPACE", //$NON-NLS-1$
				new TextReplacer("&", "&amp;", " ", "&nbsp;", "\t", "&nbsp;&nbsp;&nbsp;", "<", "&lt;", ">", "&gt;",
						"\"", "&quot;", "'", "&#39;", "\n", "<br>\n"));
		//		res.put("TXTTOHTML_SAFE", //$NON-NLS-1$
		//				new TextReplacer("&", "&amp;", "<", "&lt;", ">", "&gt;", "\"", "&quot;", "'", "&#39;"));
		return res;
	}

	private Map<String, Object> getDefaultLocalExtensions() {
		Map<String, Object> res = new HashMap<>();
		res.put("registerVariable", new VariableRegistrator(this));
		res.put("anchorNumberer", new AnchorNumberer());

		return res;
	}

	private void registerExtensions(Map<String, Object> root) {
		root.putAll(globalExtensions);
		root.putAll(localExtensions);
	}

	private void registerVariables(Map<String, Object> root) {
		for (Entry<String, Object> val : variables.entrySet()) {
			root.put(val.getKey(), val.getValue());
		}
	}

	private static Map<String, Object> globalExtensions = getDefaultGlobalExtensions();

	public static void addGlobalExtension(String key, Object ext) {
		globalExtensions.put(key, ext);
	}

	public static void removeGlobalExtension(String key) {
		globalExtensions.remove(key);
	}

	private Map<String, Object> localExtensions = getDefaultLocalExtensions();

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

	@SuppressWarnings("unchecked")
	public void setVariables(Properties props) {
		Enumeration<String> enums = (Enumeration<String>) props.propertyNames();
		while (enums.hasMoreElements()) {
			String key = enums.nextElement();
			String value = props.getProperty(key);
			setVariable(key, value);
		}
	}

	public Object getVariable(String name) {
		return variables.get(name);
	}

	public void clearVariables() {
		variables.clear();
	}

	public void setNumberFormat(NumberFormats format) {
		switch (format) {
		case COMPUTER:
			cfg.setNumberFormat("computer");
			break;
		case CURRENCY:
			cfg.setNumberFormat("currency");
			break;
		case DEFAULT:
			cfg.setNumberFormat("number");
			break;
		case PERCENT:
			cfg.setNumberFormat("percent");
			break;
		}
	}

	public enum NumberFormats {
		DEFAULT, PERCENT, COMPUTER, CURRENCY
	}

}
