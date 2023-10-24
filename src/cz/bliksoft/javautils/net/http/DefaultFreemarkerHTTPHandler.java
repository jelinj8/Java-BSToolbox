package cz.bliksoft.javautils.net.http;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

	@Override
	public void handle(BSHttpContext context) throws IOException {
		String path = context.requested;

		if (StringUtils.isEmpty(path) || "/".equals(path))
			path = indexFileName;

		FreemarkerGenerator generator = null;
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

			generator.setVariable("environment", EnvironmentUtils.getEnvironmentProperties());

			for (Entry<String, Object> ext : extensions.entrySet()) {
				generator.addExtension(ext.getKey(), ext.getValue());
			}

			for (Entry<String, Object> var : variables.entrySet()) {
				generator.setVariable(var.getKey(), var.getValue());
			}

		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to create FreemarkerGenerator.", e);
			sendERR(context.httpExchange, "Failed to create FreeemarkerGenerator",
					HTTPErrorCodes.SERVER_INTERNAL_SERVER_ERROR.getValue());
		}

		try {
			sendOK(context.httpExchange, generator.generate(path), CONTENT_TYPE_HTML);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to process template.", e);
			sendERR(context.httpExchange, "Failed to process template",
					HTTPErrorCodes.SERVER_INTERNAL_SERVER_ERROR.getValue());
		}

		File pageFile = new File(rootFolder, path);
		log.fine("Serve " + pageFile);
		sendOKDocument(context.httpExchange, pageFile);
	}

	public void setVariable(String name, Properties values) {
		Map<String, String> vars = new HashMap<>();
		values.forEach((p, v) -> {
			vars.put((String) p, (String) v);
		});

		variables.put(name, vars);
	}
}