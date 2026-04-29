package cz.bliksoft.javautils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

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
				String CAType = prop.getProperty("CAcrtType", "jks");

				System.setProperty("javax.net.ssl.trustStore", CAPath);
				System.setProperty("javax.net.ssl.trustStorePassword", CAPasswd);
				System.setProperty("javax.net.ssl.trustStoreType", CAType);
			}

			String CliPath = prop.getProperty("CLIcrtFile");
			if (CliPath != null) {
				String CliPasswd = CryptUtils.getPwdFromProperties(prop, "CLIcrtPwd", "changeit");
				saveProps |= CryptUtils.passwordRewritten();
				String CliType = prop.getProperty("CLIcrtType", "jks");
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

	/**
	 * set debugging mode for SSL handshake
	 */
	public static void traceHandshake() {
		System.setProperty("javax.net.debug", "ssl:handshake");
	}

	public static KeyStore loadKeystore(File propFile, String propPrefix) throws GeneralSecurityException, IOException {
		if (propFile.exists()) {
			Properties prop = PropertiesUtils.loadFromFile(propFile, EnvironmentUtils.getAllEnvironmentProperties());

			boolean saveProps = false;

			String CAPath = prop.getProperty(propPrefix + "crtFile");
			if (CAPath != null) {
				String CAPasswd = CryptUtils.getPwdFromProperties(prop, propPrefix + "crtPwd", "changeit");
				saveProps |= CryptUtils.passwordRewritten();
				String CAType = prop.getProperty(propPrefix + "crtType", "jks");

				try (FileInputStream myKeys = new FileInputStream(CAPath)) {
					KeyStore myTrustStore = KeyStore.getInstance(CAType);
					myTrustStore.load(myKeys, CAPasswd.toCharArray());
					log.info("Loaded " + CAPath + " as keystore");
					return myTrustStore;

				} finally {
					if (saveProps) {
						try {
							PropertiesUtils.saveProperties(prop, propFile, "PWD crypt", true);
						} catch (Exception e) {
							log.severe("Failed to save CRT properties file " + e.getMessage());
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * merge configured truststore into the default one
	 *
	 * @param propFile
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public static void mergeTrustStore(File propFile) throws IOException, GeneralSecurityException {
		X509TrustManager jreTrustManager = getJreTrustManager();
		X509TrustManager myTrustManager = getMyTrustManager(propFile);

		X509TrustManager mergedTrustManager = createMergedTrustManager(jreTrustManager, myTrustManager);
		setSystemTrustManager(mergedTrustManager);
	}

	private static X509TrustManager getJreTrustManager() throws NoSuchAlgorithmException, KeyStoreException {
		return findDefaultTrustManager(null);
	}

	private static X509TrustManager getMyTrustManager(File propFile)
			throws FileNotFoundException, IOException, GeneralSecurityException {
		if (propFile.exists()) {
			Properties prop = PropertiesUtils.loadFromFile(propFile, EnvironmentUtils.getAllEnvironmentProperties());

			boolean saveProps = false;

			String CAPath = prop.getProperty("CAcrtFile");
			if (CAPath != null) {
				String CAPasswd = CryptUtils.getPwdFromProperties(prop, "CAcrtPwd", "changeit");
				saveProps |= CryptUtils.passwordRewritten();
				String CAType = prop.getProperty("CAcrtType", "jks");

				try (FileInputStream myKeys = new FileInputStream(CAPath)) {
					KeyStore myTrustStore = KeyStore.getInstance(CAType);
					myTrustStore.load(myKeys, CAPasswd.toCharArray());
					log.info("Merging " + CAPath + " into Java truststore");
					return findDefaultTrustManager(myTrustStore);

				} finally {
					if (saveProps) {
						try {
							PropertiesUtils.saveProperties(prop, propFile, "PWD crypt", true);
						} catch (Exception e) {
							log.severe("Failed to save CRT properties file " + e.getMessage());
						}
					}
				}
			}
		}
		return null;
	}

	private static X509TrustManager findDefaultTrustManager(KeyStore keyStore)
			throws NoSuchAlgorithmException, KeyStoreException {
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(keyStore); // If keyStore is null, tmf will be initialized with the default trust store

		for (TrustManager tm : tmf.getTrustManagers()) {
			if (tm instanceof X509TrustManager) {
				return (X509TrustManager) tm;
			}
		}
		return null;
	}

	private static X509TrustManager createMergedTrustManager(X509TrustManager jreTrustManager,
			X509TrustManager customTrustManager) {
		return new X509TrustManager() {
			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				try {
					customTrustManager.checkServerTrusted(chain, authType);
				} catch (CertificateException e) {
					// This will throw another CertificateException if this fails too.
					jreTrustManager.checkServerTrusted(chain, authType);
				}
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				// If you're planning to use client-cert auth,
				// merge results from "defaultTm" and "myTm".
				return jreTrustManager.getAcceptedIssuers();
			}

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				// If you're planning to use client-cert auth,
				// do the same as checking the server.
				jreTrustManager.checkClientTrusted(chain, authType);
			}
		};
	}

	public static void setSystemTrustManager(X509TrustManager mergedTrustManager)
			throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, new TrustManager[] { mergedTrustManager }, null);

		// You don't have to set this as the default context,
		// it depends on the library you're using.
		SSLContext.setDefault(sslContext);
	}

}
