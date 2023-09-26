package cz.bliksoft.javautils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import cz.bliksoft.javautils.exceptions.InitializationException;

public class EnvironmentUtils {
	private static File environmentConfigDir = null;
	private static File globalConfigDir = null;
	private static File environmentConfig = null;

	public static final String PROP_ENVIRONMENT_CONFIG_DIR = "environmentConfigDir";
	public static final String PROP_GLOBAL_CONFIG_DIR = "configDir";
	public static final String PROP_ENVIRONMENT_PROPERTIES_FILE = "environmentConfig";

	public static void init() throws IOException {
		environmentConfigDir = new File(
				StringUtils.hasTextDefault(System.getenv(PROP_ENVIRONMENT_CONFIG_DIR), "env_config"));

		globalConfigDir = new File(StringUtils.hasTextDefault(System.getenv(PROP_GLOBAL_CONFIG_DIR), "config"));

		environmentConfig = new File(
				StringUtils.hasTextDefault(System.getenv(PROP_ENVIRONMENT_PROPERTIES_FILE), "env.properties"));

		commonInit();
	}

	public static void init(Properties props) throws IOException {
		environmentConfigDir = new File(StringUtils.hasTextDefault(System.getenv(PROP_ENVIRONMENT_CONFIG_DIR),
				props.getProperty(PROP_ENVIRONMENT_CONFIG_DIR, "env_config")));

		globalConfigDir = new File(StringUtils.hasTextDefault(System.getenv(PROP_GLOBAL_CONFIG_DIR),
				props.getProperty(PROP_GLOBAL_CONFIG_DIR, "config")));

		environmentConfig = new File(StringUtils.hasTextDefault(System.getenv(PROP_ENVIRONMENT_PROPERTIES_FILE),
				props.getProperty(PROP_ENVIRONMENT_PROPERTIES_FILE, "env.properties")));

		commonInit();
	}

	private static void commonInit() throws IOException {
		environmentProperties = new HashMap<>();
		environmentProperties.put(PROP_GLOBAL_CONFIG_DIR, globalConfigDir.getName());
		environmentProperties.put(PROP_ENVIRONMENT_CONFIG_DIR, environmentConfigDir.getName());
		environmentProperties.put(PROP_ENVIRONMENT_PROPERTIES_FILE, environmentConfig.getName());

		Properties envP = PropertiesUtils.loadFromFile(environmentConfig, environmentProperties);
		envP.forEach((key, value) -> {
			switch ((String) key) {
			case PROP_ENVIRONMENT_CONFIG_DIR:
			case PROP_GLOBAL_CONFIG_DIR:
			case PROP_ENVIRONMENT_PROPERTIES_FILE:
				break;
			default:
				String envValue = System.getenv((String) key);
				String val = StringUtils.hasTextDefault(envValue, (String) value);

				if (StringUtils.hasLength(val)) {
					if (environmentProperties.putIfAbsent((String) key, val) != null) {
						throw new InitializationException("Requested environment value " + key + " duplicity.");
					}
				} else {
					throw new InitializationException(
							"Requested environment value " + key + " is not set and no default was specified.");
				}
			}
		});
	}

	private static void checkInit() {
		if (globalConfigDir == null)
			throw new InitializationException(
					"EnvironmentUtils.init was not called to initialize PropertiesUtils functions.");
	}

	public static File getConfigDir() {
		checkInit();
		return globalConfigDir;
	}

	public static File getEnvironmentConfigDir() {
		checkInit();
		if (environmentConfigDir.exists())
			return environmentConfigDir;
		else
			return globalConfigDir;
	}

	private static Map<String, String> environmentProperties = null;

	public static Map<String, String> getEnvironmentProperties() {
		if (environmentProperties == null)
			throw new InitializationException(
					"EnvironmentUtils.init was not called to initialize PropertiesUtils functions.");
		return environmentProperties;
	}

}
