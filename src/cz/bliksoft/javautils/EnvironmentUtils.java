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

	public static final String PROP_TIMESTAMP = "timestamp";

	private static String getDefaultEnvDirName() throws IOException {
		String envDirName;
		File defaultEnvFile = new File("default.env");
		if (defaultEnvFile.exists()) {
			envDirName = FileUtils.readFileToString(defaultEnvFile, Charsets.UTF_8);
			if (envDirName != null)
				envDirName = envDirName.trim();

			if (envDirName.startsWith("#"))
				envDirName = "env_config";
		} else {
			envDirName = "env_config";
		}
		return envDirName;
	}

	public static void init() throws IOException {
		environmentConfigDir = new File(StringUtils.hasTextDefault(System.getenv(PROP_ENVIRONMENT_CONFIG_DIR),
				environmentProperties.getOrDefault(PROP_ENVIRONMENT_CONFIG_DIR, getDefaultEnvDirName())));

		globalConfigDir = new File(StringUtils.hasTextDefault(System.getenv(PROP_GLOBAL_CONFIG_DIR),
				environmentProperties.getOrDefault(PROP_GLOBAL_CONFIG_DIR, "config")));

		environmentConfig = new File(environmentConfigDir,
				StringUtils.hasTextDefault(System.getenv(PROP_ENVIRONMENT_PROPERTIES_FILE),
						environmentProperties.getOrDefault(PROP_ENVIRONMENT_PROPERTIES_FILE, "env.properties")));

		commonInit();
	}

	public static void init(Properties props) throws IOException {
		environmentConfigDir = new File(StringUtils.hasTextDefault(System.getenv(PROP_ENVIRONMENT_CONFIG_DIR),
				props.getProperty(PROP_ENVIRONMENT_CONFIG_DIR,
						environmentProperties.getOrDefault(PROP_ENVIRONMENT_CONFIG_DIR, getDefaultEnvDirName()))));

		globalConfigDir = new File(StringUtils.hasTextDefault(System.getenv(PROP_GLOBAL_CONFIG_DIR), props.getProperty(
				PROP_GLOBAL_CONFIG_DIR, environmentProperties.getOrDefault(PROP_GLOBAL_CONFIG_DIR, "config"))));

		environmentConfig = new File(environmentConfigDir,
				StringUtils.hasTextDefault(System.getenv(PROP_ENVIRONMENT_PROPERTIES_FILE), props.getProperty(
						PROP_ENVIRONMENT_PROPERTIES_FILE,
						environmentProperties.getOrDefault(PROP_ENVIRONMENT_PROPERTIES_FILE, "env.properties"))));

		commonInit();
	}

	/**
	 * preload values directly, to be overridden by those loaded in proper init
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

	public static void setEnvironmentProperty(String name, String value) {
		if (value == null)
			environmentProperties.remove(name);
		else
			environmentProperties.put(name, value);

		if (!name.startsWith(".")) {
			if (value == null)
				publicEnvironmentProperties.remove(name);
			else
				publicEnvironmentProperties.put(name, value);
		}
	}

	private static void importVal(String key, String value) {
		if (StringUtils.hasLength(value)) {
			if ("#OPTIONAL#".equals(value))
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
		environmentProperties.put(PROP_TIMESTAMP, DateUtils.TimestampString());

		//		if (!environmentConfig.exists())
		//			throw new InitializationException(
		//					"Environment configuration file " + environmentConfig.getAbsolutePath() + " does not exist!");

		if (environmentConfig.exists()) {
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
		}

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

	public static File getConfigDir(String subfile) {
		checkInit();
		return new File(globalConfigDir, subfile);
	}

	public static File getEnvironmentConfigDir() {
		checkInit();
		if (environmentConfigDir.exists())
			return environmentConfigDir;
		else
			return globalConfigDir;
	}

	public static File getEnvironmentConfigDir(String subfile) {
		checkInit();
		if (environmentConfigDir.exists())
			return new File(environmentConfigDir, subfile);
		else
			return new File(globalConfigDir, subfile);
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

	public static Map<String, String> tryGetAllEnvironmentProperties() {
		if (isInitialized())
			return getAllEnvironmentProperties();
		else
			return new HashMap<>();
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

	public static Map<String, String> tryGetEnvironmentProperties() {
		if (isInitialized())
			return getEnvironmentProperties();
		else
			return new HashMap<>();
	}

	public static boolean isInitialized() {
		return environmentConfigDir != null;
	}

}
