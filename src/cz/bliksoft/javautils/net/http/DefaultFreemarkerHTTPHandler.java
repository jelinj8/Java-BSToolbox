package cz.bliksoft.javautils.net.http;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.javautils.EnvironmentUtils;
import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.freemarker.FreemarkerGenerator;
import cz.bliksoft.javautils.freemarker.includes.BuiltinTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class DefaultFreemarkerHTTPHandler extends BasicHTTPHandler implements Closeable {
	private static Logger log = Logger.getLogger(DefaultFreemarkerHTTPHandler.class.getName());

	private File rootFolder = null;
	private TemplateLoader templateLoader = null;
	private String indexFileName = "index.ftlh";
	private List<TemplateLoader> additionalTemplateLoaders = new ArrayList<>();

	public List<TemplateLoader> getAdditionalLoadersList() {
		return additionalTemplateLoaders;
	}

	public Map<String, Object> extensions = new HashMap<>();

	/**
	 * global FreemarkerHttpHandler variables for all FreemarkerHandlers
	 */
	public static Map<String, Object> globalVariables = new HashMap<>();

	/**
	 * FreemarkerHttpHandler instance variables
	 */
	public Map<String, Object> variables = new HashMap<>();

	public DefaultFreemarkerHTTPHandler() {
		addSupportedGETPOST();
	}

	public DefaultFreemarkerHTTPHandler(File root) {
		this();
		rootFolder = root;
	}

	public void setRootFolder(File root) {
		rootFolder = root;
	}

	public DefaultFreemarkerHTTPHandler(TemplateLoader loader) {
		this();
		this.templateLoader = loader;
	}

	public void setTemplateLoader(TemplateLoader loader) {
		this.templateLoader = loader;
	}

	/**
	 * set default index report
	 * 
	 * @param indexName
	 */
	public void setIndexFileName(String indexName) {
		indexFileName = indexName;
	}

	/**
	 * get default index report
	 * 
	 * @return
	 */
	public String getIndexFileName() {
		return indexFileName;
	}

	@SuppressWarnings("restriction")
	@Override
	public boolean handle(BSHttpContext context) throws IOException {
		String path = getRequestedPath(context);

		if (StringUtils.isEmpty(path) || "/".equals(path))
			path = indexFileName;

		FreemarkerGenerator generator = null;
		Template template = null;
		try {
			List<TemplateLoader> loaders = new ArrayList<>();
			loaders.add(BuiltinTemplateLoader.getBuiltinTemplateLoader());
			if (rootFolder != null)
				loaders.add(new FileTemplateLoader(rootFolder));
			if (templateLoader != null)
				loaders.add(templateLoader);

			loaders.addAll(additionalTemplateLoaders);

			generator = new FreemarkerGenerator(new MultiTemplateLoader(loaders.toArray(new TemplateLoader[] {})));

			generator.setVariable("http", context);

			try {
				generator.setVariable("environment", EnvironmentUtils.getEnvironmentProperties());
			} catch (Exception e) {
				log.fine("EnvironmentUtils not initialized.");
			}

			generator.addExtensions(extensions);

			generator.setVariables(globalVariables);
			generator.setVariables(variables);
			generator.setVariables(context.contextVariables);

		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to create FreemarkerGenerator.", e);
			sendERR(context.httpExchange, "Failed to create FreeemarkerGenerator",
					HTTPErrorCodes.SERVER_INTERNAL_SERVER_ERROR.getValue());
			return true;
		}

		try {
			template = generator.getTemplate(path);
			addCommonHeaders(context.httpExchange, CONTENT_TYPE_HTML);
			sendHeaders(context.httpExchange, null, null);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to get template.", e);
			sendERR(context.httpExchange, "Failed to get template",
					HTTPErrorCodes.SERVER_INTERNAL_SERVER_ERROR.getValue());
			return true;
		}

		try (OutputStream os = context.httpExchange.getResponseBody()) {
			generator.generate(os, template);
		} catch (TemplateException e) {
			log.log(Level.SEVERE, "Failed to process template, sending response already started", e);
		}
		return true;
	}

	public void setVariable(String name, Properties values) {
		Map<String, String> vars = new HashMap<>();
		values.forEach((p, v) -> {
			vars.put((String) p, (String) v);
		});

		variables.put(name, vars);
	}
}