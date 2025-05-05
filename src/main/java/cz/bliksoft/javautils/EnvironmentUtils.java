package cz.bliksoft.javautils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import cz.bliksoft.javautils.exceptions.InitializationException;

/**
 * toolset for working with various configurations (environments) with a shared
 * base (global config), allowing usage of <code>${variable}</code> placeholders
 * replacement by environment variables and preloaded values
 */
public class EnvironmentUtils {
	private static File environmentConfigDir = null;
	private static File globalConfigDir = null;
	private static File environmentConfig = null;

	private static boolean initialized = false;

	/**
	 * common configuration directory (shared between switchable environments)
	 */
	public static final String PROP_GLOBAL_CONFIG_DIR = "configDir";
	/**
	 * switchable environment config directory
	 */
	public static final String PROP_ENVIRONMENT_CONFIG_DIR = "environmentConfigDir";
	/**
	 * environment confiruration file name
	 */
	public static final String PROP_ENVIRONMENT_PROPERTIES_FILE = "environmentConfig";

	/**
	 * placeholder property for formatteed timestamp, created at initialization
	 */
	public static final String PROP_TIMESTAMP = "timestamp";

	public static final String PROP_WORKDIR = "workdir";

	/**
	 * placeholder property for configured log directory
	 */
	public static final String LOG_DIR = "logDir";

	private static String getDefaultEnvDirName() throws IOException {
		String envDirName;
		File defaultEnvFile = new File("default.env");
		if (defaultEnvFile.exists()) {
			envDirName = FileUtils.readFileToString(defaultEnvFile, StandardCharsets.UTF_8);
			if (envDirName != null)
				envDirName = envDirName.trim();

			if (envDirName.startsWith("#"))
				envDirName = "env_config";
		} else {
			envDirName = "env_config";
		}
		return envDirName;
	}

	/**
	 * toolset initialization
	 * <ul>
	 * <li>"environmentConfigDir" from environment variable, preload or default file
	 * (default.env in root)
	 * <li>global "configDir" from environment variable, or preload
	 * <li>"environmentConfig" file from environment variable, or preload
	 * </ul>
	 * 
	 * @throws IOException
	 */
	public static void init() throws IOException {
		if (environmentConfigDir == null)
			environmentConfigDir = new File(StringUtils.hasTextDefault(System.getenv(PROP_ENVIRONMENT_CONFIG_DIR),
					environmentProperties.getOrDefault(PROP_ENVIRONMENT_CONFIG_DIR, getDefaultEnvDirName())));

		globalConfigDir = new File(StringUtils.hasTextDefault(System.getenv(PROP_GLOBAL_CONFIG_DIR),
				environmentProperties.getOrDefault(PROP_GLOBAL_CONFIG_DIR, "config")));

		environmentConfig = new File(environmentConfigDir,
				StringUtils.hasTextDefault(System.getenv(PROP_ENVIRONMENT_PROPERTIES_FILE),
						environmentProperties.getOrDefault(PROP_ENVIRONMENT_PROPERTIES_FILE, "env.properties")));

		commonInit();
	}

	/**
	 * toolset initialization
	 * <ul>
	 * <li>"environmentConfigDir" from environment variable, preload or default file
	 * (default.env in root)
	 * <li>global "configDir" from environment variable, or preload
	 * <li>"environmentConfig" file from environment variable, or preload
	 * </ul>
	 * Same as {@link EnvironmentUtils#init() init()}, just with another defaults
	 * level
	 * 
	 * @param props defaults between environment variables and preload values
	 * @throws IOException
	 */
	public static void init(Properties props) throws IOException {
		if (environmentConfigDir == null)
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
	 * (environment variables, then optional properties, then preloaded values, last
	 * resort default files
	 * 
	 * @param props values to be preloaded as replacable values
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

	public static void setEnvironmentPropertyIfInitialized(String name, String value) {
		if (!isInitialized())
			return;
		setEnvironmentProperty(name, value);
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
		environmentProperties.put(PROP_WORKDIR, new File(".").getAbsoluteFile().getParentFile().getAbsolutePath());

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
		initialized = true;
	}

	private static void checkInit() {
		if (globalConfigDir == null)
			throw new InitializationException(
					"EnvironmentUtils.init was not called to initialize PropertiesUtils functions.");
	}

	/**
	 * common configuration directory
	 * 
	 * @return
	 */
	public static File getConfigDir() {
		checkInit();
		return globalConfigDir;
	}

	/**
	 * a file in common configuration directory
	 * 
	 * @param subfile
	 * @return
	 */
	public static File getConfigDir(String subfile) {
		checkInit();
		return new File(globalConfigDir, subfile);
	}

	/**
	 * current switchable environment configuration directory, defaults to global
	 * config dir if it doesn't exist
	 * 
	 * @return
	 */
	public static File getEnvironmentConfigDir() {
		checkInit();
		if (environmentConfigDir.exists())
			return environmentConfigDir;
		else
			return globalConfigDir;
	}

	/**
	 * a file in current switchable environment configuration directory
	 * 
	 * @param subfile
	 * @return
	 */
	public static File getEnvironmentConfigDir(String subfile) {
		checkInit();
		if (environmentConfigDir.exists())
			return new File(environmentConfigDir, subfile);
		else
			return new File(getEnvironmentConfigDir(), subfile);
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
	 * 
	 * @return
	 */
	public static Map<String, String> tryGetAllEnvironmentProperties() {
		if (isInitialized())
			return getAllEnvironmentProperties();
		else
			return new HashMap<>();
	}

	/**
	 * returns properties except those starting with a dot (hidden properties),
	 * fails with InitializationException if not properly initialized.
	 * 
	 * @return
	 */
	public static Map<String, String> getEnvironmentProperties() {
		checkInit();
		return publicEnvironmentProperties;
	}

	/**
	 * returns properties except those starting with a dot (hidden properties),
	 * returns empty map if not properly initialized.
	 * 
	 * @return
	 */
	public static Map<String, String> tryGetEnvironmentProperties() {
		if (isInitialized())
			return getEnvironmentProperties();
		else
			return new HashMap<>();
	}

	/**
	 * checks if the toolset was properly initialized
	 * 
	 * @return
	 */
	public static boolean isInitialized() {
		return initialized;
	}

	/**
	 * Set a directory to be used as an environment config. Can be done only once,
	 * before initialization of EnvironmentUtils (if not called before,
	 * initialization will perform its logic)
	 * 
	 * @param directory
	 */
	public static void setEnvironmentConfigDirectory(File directory) {
		environmentConfigDir = directory;
	}

}
