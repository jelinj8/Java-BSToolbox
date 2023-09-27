package cz.bliksoft.javautils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.codec.Charsets;
import org.apache.commons.io.FileUtils;

import cz.bliksoft.javautils.exceptions.InitializationException;

public class EnvironmentUtils {
	private static File environmentConfigDir = null;
	private static File globalConfigDir = null;
	private static File environmentConfig = null;

	public static final String PROP_ENVIRONMENT_CONFIG_DIR = "environmentConfigDir";
	public static final String PROP_GLOBAL_CONFIG_DIR = "configDir";
	public static final String PROP_ENVIRONMENT_PROPERTIES_FILE = "environmentConfig";

	public static void init() throws IOException {
		String envDirName = "env_config";
		File defaultEnvFile = new File("default.env");
		if (defaultEnvFile.exists())
			envDirName = FileUtils.readFileToString(defaultEnvFile, Charsets.UTF_8);

		environmentConfigDir = new File(
				StringUtils.hasTextDefault(System.getenv(PROP_ENVIRONMENT_CONFIG_DIR), envDirName));

		globalConfigDir = new File(StringUtils.hasTextDefault(System.getenv(PROP_GLOBAL_CONFIG_DIR), "config"));

		environmentConfig = new File(environmentConfigDir,
				StringUtils.hasTextDefault(System.getenv(PROP_ENVIRONMENT_PROPERTIES_FILE), "env.properties"));

		commonInit();
	}

	public static void init(Properties props) throws IOException {
		environmentConfigDir = new File(StringUtils.hasTextDefault(System.getenv(PROP_ENVIRONMENT_CONFIG_DIR),
				props.getProperty(PROP_ENVIRONMENT_CONFIG_DIR, "env_config")));

		globalConfigDir = new File(StringUtils.hasTextDefault(System.getenv(PROP_GLOBAL_CONFIG_DIR),
				props.getProperty(PROP_GLOBAL_CONFIG_DIR, "config")));

		environmentConfig = new File(environmentConfigDir,
				StringUtils.hasTextDefault(System.getenv(PROP_ENVIRONMENT_PROPERTIES_FILE),
						props.getProperty(PROP_ENVIRONMENT_PROPERTIES_FILE, "env.properties")));

		commonInit();
	}

	/**
	 * preload values directly, to be overriden by those loaded in proper init
	 * 
	 * @param props
	 * @throws IOException
	 */
	public static void preinit(Properties props) throws IOException {
		props.forEach((key, value) -> {
			String k = (String) key;
			if (k.startsWith("."))
				k = k.substring(1);

			String envValue = System.getenv(k);
			String val = StringUtils.hasTextDefault(envValue, (String) value);
			importVal((String) key, val);
		});
	}

	private static void importVal(String key, String value) {
		if (StringUtils.hasLength(value)) {
			if("#OPTIONAL#".equals(value))
				return;
			
			if (environmentProperties.putIfAbsent(key, ("#EMPTY#".equals(value) ? "" : value)) != null) {
				throw new InitializationException("Requested environment value " + key + " duplicity.");
			}
		} else {
			throw new InitializationException(
					"Requested environment value " + key + " is not set and no default was specified.");
		}
	}

	private static void commonInit() throws IOException {
		environmentProperties.put(PROP_GLOBAL_CONFIG_DIR, globalConfigDir.getPath());
		environmentProperties.put(PROP_ENVIRONMENT_CONFIG_DIR, environmentConfigDir.getPath());
		environmentProperties.put(PROP_ENVIRONMENT_PROPERTIES_FILE, environmentConfig.getPath());

		if (!environmentConfig.exists())
			throw new InitializationException(
					"Environment configuration file " + environmentConfig.getAbsolutePath() + " does not exist!");

		Properties envP = PropertiesUtils.loadFromFile(environmentConfig, environmentProperties);
		envP.forEach((key, value) -> {
			switch ((String) key) {
			case PROP_ENVIRONMENT_CONFIG_DIR:
			case PROP_GLOBAL_CONFIG_DIR:
			case PROP_ENVIRONMENT_PROPERTIES_FILE:
				break;
			default:
				String k = (String) key;
				if (k.startsWith("."))
					k = k.substring(1);

				String envValue = System.getenv(k);
				String val = StringUtils.hasTextDefault(envValue, (String) value);
				importVal((String) key, val);
			}
		});

		environmentProperties.forEach((k, v) -> {
			if (!k.startsWith("."))
				publicEnvironmentProperties.put(k, v);
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

	private static Map<String, String> environmentProperties = new HashMap<>();
	private static Map<String, String> publicEnvironmentProperties = new HashMap<>();

	/**
	 * returns all properties, including those marked with dot on beginning (hidden
	 * properties)
	 * 
	 * @return
	 */
	public static Map<String, String> getAllEnvironmentProperties() {
		checkInit();
		return environmentProperties;
	}

	/**
	 * returns properties except those starting with a dot (hidden properties)
	 * 
	 * @return
	 */
	public static Map<String, String> getEnvironmentProperties() {
		checkInit();
		return publicEnvironmentProperties;
	}

}
