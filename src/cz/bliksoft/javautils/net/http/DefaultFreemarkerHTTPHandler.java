package cz.bliksoft.javautils.net.http;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange;

import cz.bliksoft.javautils.EnvironmentUtils;
import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.freemarker.FreemarkerGenerator;
import cz.bliksoft.javautils.freemarker.includes.BuiltinTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;

@SuppressWarnings("restriction")
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

	private String prefixPath = null;

	public DefaultFreemarkerHTTPHandler() {
	}

	public DefaultFreemarkerHTTPHandler(File root) {
		rootFolder = root;
	}

	public void setRootFolder(File root) {
		rootFolder = root;
	}

	public DefaultFreemarkerHTTPHandler(TemplateLoader loader) {
		this.templateLoader = loader;
	}

	public void setTemplateLoader(TemplateLoader loader) {
		this.templateLoader = loader;
	}

	public void setPrefixPath(String prefix) {
		prefixPath = prefix;
	}

	public void setIndexFileName(String name) {
		indexFileName = name;
	}

	@Override
	public void handle(HttpExchange exchange, String path, String query, String method) throws IOException {
		switch (method) {
		case "GET":
		case "POST":
			break;
		default:
			sendERR(exchange, "Unsupported method", HTTPErrorCodes.CLIENT_UNSUPPORTED_MEDIA_TYPE.getValue());
			throw new IOException("Unsupported method: " + method);
		}

		if (prefixPath != null && path != null)
			path = path.replace(prefixPath, "");

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

			Map<String, String> propsMap = new HashMap<>();
			Map<String, List<Optional<String>>> params = URIParameterDecode.splitQuery(query);
			params.forEach((k, v) -> {
				if (v.get(0).isPresent())
					propsMap.put(k, v.get(0).get());
			});
			generator.setVariable("parameters", params);

			generator.setVariable("environment", EnvironmentUtils.getEnvironmentProperties());

			for (Entry<String, Object> ext : extensions.entrySet()) {
				generator.addExtension(ext.getKey(), ext.getValue());
			}

			for (Entry<String, Object> var : variables.entrySet()) {
				generator.setVariable(var.getKey(), var.getValue());
			}

		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to create FreemarkerGenerator.", e);
			sendERR(exchange, "Failed to create FreeemarkerGenerator",
					HTTPErrorCodes.SERVER_INTERNAL_SERVER_ERROR.getValue());
		}

		try {
			sendOK(exchange, generator.generate(path), CONTENT_TYPE_HTML);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to process template.", e);
			sendERR(exchange, "Failed to process template", HTTPErrorCodes.SERVER_INTERNAL_SERVER_ERROR.getValue());
		}

		File pageFile = new File(rootFolder, path);
		log.fine("Serve " + pageFile);
		sendOKDocument(exchange, pageFile);

	}
}