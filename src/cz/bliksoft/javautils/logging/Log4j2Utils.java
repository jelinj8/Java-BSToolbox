package cz.bliksoft.javautils.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.LogManager;

import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

import cz.bliksoft.javautils.EnvironmentUtils;
import cz.bliksoft.javautils.streams.replacer.MapTokenResolver;
import cz.bliksoft.javautils.streams.replacer.TokenReplacingReader;

public class Log4j2Utils {
	public static void init(File configFile, Map<String, String> tokensToReplace)
			throws FileNotFoundException, IOException {
		System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

		if (configFile != null) {
			if (tokensToReplace != null) {
				try (TokenReplacingReader trdr = new TokenReplacingReader(configFile,
						new MapTokenResolver(EnvironmentUtils.getEnvironmentProperties()))) {

					try (InputStream is = trdr.toInputStream(StandardCharsets.UTF_8)) {
						ConfigurationSource configurationSource = new ConfigurationSource(is);
						Configurator.initialize(null, configurationSource);
					}
				}
			} else {
				try (InputStream is = new FileInputStream(configFile)) {
					ConfigurationSource configurationSource = new ConfigurationSource(is);
					Configurator.initialize(null, configurationSource);
				}
			}
		}

		org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger();
		if (!"org.apache.logging.log4j.jul.LogManager".equals(LogManager.getLogManager().getClass().getName())) {
			logger.warn("Java LogManager instantiated as " + LogManager.getLogManager().getClass().getName()
					+ ", org.apache.logging.log4j.jul.LogManager not in place!");
		}
		if (configFile != null)
			logger.info("Log4J2 initialized with " + configFile.getAbsoluteFile());
		else {
			logger.warn("Log4J2 initialized with implicit configuration");
		}
	}
}
