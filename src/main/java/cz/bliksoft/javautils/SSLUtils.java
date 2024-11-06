package cz.bliksoft.javautils;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;
import java.util.logging.Logger;

public class SSLUtils {
	private static Logger log = Logger.getLogger(SSLUtils.class.getName());

	public static void setupKeystores(File propFile) throws IOException, GeneralSecurityException {
		if (propFile.exists()) {
			Properties prop = PropertiesUtils.loadFromFile(propFile, EnvironmentUtils.getAllEnvironmentProperties());

			boolean saveProps = false;

			String CAPath = prop.getProperty("CAcrtFile");
			if (CAPath != null) {
				String CAPasswd = CryptUtils.getPwdFromProperties(prop, "CAcrtPwd", "changeit");
				saveProps |= CryptUtils.passwordRewritten();
				String CAType = prop.getProperty("CAcrtType");

				System.setProperty("javax.net.ssl.trustStore", CAPath);
				System.setProperty("javax.net.ssl.trustStorePassword", CAPasswd);
				System.setProperty("javax.net.ssl.trustStoreType", CAType);
			}

			String CliPath = prop.getProperty("CLIcrtFile");
			if (CliPath != null) {
				String CliPasswd = CryptUtils.getPwdFromProperties(prop, "CLIcrtPwd", "changeit");
				saveProps |= CryptUtils.passwordRewritten();
				String CliType = prop.getProperty("CLIcrtType");
				System.setProperty("javax.net.ssl.keyStore", CliPath);
				System.setProperty("javax.net.ssl.keyStorePassword", CliPasswd);
				System.setProperty("javax.net.ssl.keyStoreType", CliType);
			}

			String CLIProvider = prop.getProperty("CLIProvider");
			if (CLIProvider != null)
				System.setProperty("javax.net.ssl.keyStoreProvider", CLIProvider);

			if (saveProps) {
				try {
					PropertiesUtils.saveProperties(prop, propFile, "PWD crypt", true);
				} catch (Exception e) {
					log.severe("Failed to save CRT properties file " + e.getMessage());
				}
			}
		} else {
			log.warning("Keystore properties file " + propFile + " not found!");
		}
	}

	/**
	 * to enable communication with old servers
	 */
	public static void enableTLSv1() {
		System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
		System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.1,TLSv1.2");
	}

	/**
	 * to enable communication with old servers
	 */
	public static void enableTLSv1_1() {
		System.setProperty("https.protocols", "TLSv1.1,TLSv1.2");
		System.setProperty("jdk.tls.client.protocols", "TLSv1.1,TLSv1.2");
	}

	/**
	 * e.g. for Java 1.6
	 */
	public static void requireTLSv1_2() {
		System.setProperty("https.protocols", "TLSv1.2");
		System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
	}

	/**
	 * e.g. for loadbalancer connections
	 */
	public static void allowUnasafeRenegotiation() {
		System.setProperty("jdk.tls.allowUnsafeServerCertChange", "true");
		System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
	}

}
