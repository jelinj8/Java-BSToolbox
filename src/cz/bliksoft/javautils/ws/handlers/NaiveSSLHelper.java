package cz.bliksoft.javautils.ws.handlers;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;

public class NaiveSSLHelper {
	@SuppressWarnings("rawtypes")
	public static void makeWebServiceClientNotSANValidating(Object webServicePort) {
		if (webServicePort instanceof BindingProvider) {
			BindingProvider bp = (BindingProvider) webServicePort;
			Map<String, Object> requestContext = bp.getRequestContext();
			requestContext.put(JAXWS_HOSTNAME_VERIFIER, new NaiveHostnameVerifier());
		} else if (webServicePort instanceof Dispatch) {
			((Dispatch) webServicePort).getRequestContext().put(JAXWS_HOSTNAME_VERIFIER, new NaiveHostnameVerifier());
		} else {
			throw new IllegalArgumentException(
					"webServicePort " + webServicePort.getClass().getName() + " of unsupported type");
		}
	}

	@SuppressWarnings("rawtypes")
	public static void makeWebServiceClientTrustEveryone(Object webServicePort) {
		if (webServicePort instanceof BindingProvider) {
			BindingProvider bp = (BindingProvider) webServicePort;
			Map<String, Object> requestContext = bp.getRequestContext();
			requestContext.put(JAXWS_SSL_SOCKET_FACTORY, getTrustingSSLSocketFactory());
		} else if (webServicePort instanceof Dispatch) {
			((Dispatch) webServicePort).getRequestContext().put(JAXWS_SSL_SOCKET_FACTORY,
					getTrustingSSLSocketFactory());
		} else {
			throw new IllegalArgumentException(
					"webServicePort " + webServicePort.getClass().getName() + " of unsupported type");
		}
	}

	public static void disableHostVerification() {
		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}

	public static void trustAllSSLCertificates() throws Exception {
		HttpsURLConnection.setDefaultSSLSocketFactory(SSLSocketFactoryHolder.INSTANCE);
	}

	public static SSLSocketFactory getTrustingSSLSocketFactory() {
		return SSLSocketFactoryHolder.INSTANCE;
	}

	private static SSLSocketFactory createSSLSocketFactory() {
		TrustManager[] trustManagers = new TrustManager[] { new NaiveTrustManager() };
		SSLContext sslContext;
		try {
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(new KeyManager[0], trustManagers, new SecureRandom());
			return sslContext.getSocketFactory();
		} catch (GeneralSecurityException e) {
			return null;
		}
	}

	private interface SSLSocketFactoryHolder {
		SSLSocketFactory INSTANCE = createSSLSocketFactory();
	}

	private static class NaiveHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostName, SSLSession session) {
			return true;
		}
	}

	private static class NaiveTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
	};

//	private static final java.lang.String JAXWS_HOSTNAME_VERIFIER = "com.sun.xml.internal.ws.transport.https.client.hostname.verifier";
	private static final java.lang.String JAXWS_HOSTNAME_VERIFIER = "com.sun.xml.ws.transport.https.client.hostname.verifier";
//	private static final java.lang.String JAXWS_SSL_SOCKET_FACTORY = "com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory";
	private static final java.lang.String JAXWS_SSL_SOCKET_FACTORY = "com.sun.xml.ws.transport.https.client.SSLSocketFactory";
}