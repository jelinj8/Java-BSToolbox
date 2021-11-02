package cz.bliksoft.javautils.ws;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class SecurityHandler implements SOAPHandler<SOAPMessageContext> {

	public enum AuthTypes {
		NONE, BASIC, BINDING, SOAP
	}

	private static TimeZone utc = TimeZone.getTimeZone("UTC");
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	static {
		sdf.setTimeZone(utc);
	}

	public AuthTypes authType = AuthTypes.NONE;

	public String getUser() {
		return user;
	}

	public void setCredentials(String user, String password) {
		this.user = user;
		this.pass = password;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	private String user;
	private String pass;

	@Override
	public boolean handleMessage(final SOAPMessageContext msgCtx) {
		if (authType == AuthTypes.NONE)
			return true;

		// Indicator telling us which direction this message is going in
		final Boolean outInd = (Boolean) msgCtx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

		// Handler must only add security headers to outbound messages
		if (outInd.booleanValue() && user != null && pass != null) {
			try {
				if (authType == AuthTypes.BASIC) {
					Map<String, List<String>> headers = new HashMap<String, List<String>>();
					headers.put("Username", Collections.singletonList(user));
					headers.put("Password", Collections.singletonList(pass));
					msgCtx.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
				}

				if (authType == AuthTypes.BINDING) {
					msgCtx.put(BindingProvider.USERNAME_PROPERTY, user);
					msgCtx.put(BindingProvider.PASSWORD_PROPERTY, pass);
				}

				if (authType == AuthTypes.SOAP) {
					// Create the timestamp
					Date curdate = new Date();

					String timestamp = sdf.format(curdate);

					// Generate a random nonce
					byte[] nonceBytes = new byte[16];
					for (int i = 0; i < 16; ++i)
						nonceBytes[i] = (byte) (Math.random() * 256 - 128);

					// Create the xml
					SOAPEnvelope envelope = msgCtx.getMessage().getSOAPPart().getEnvelope();
					SOAPHeader header = envelope.getHeader();
					if (header == null)
						header = envelope.addHeader();

					SOAPElement security = header.addChildElement("Security", "wsse",
							"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
					security.addNamespaceDeclaration("wsu",
							"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");

					// FIXME dočasně vynecháno
					SOAPElement soap_timestamp = security.addChildElement("Timestamp", "wsu");
					soap_timestamp.addChildElement("Created", "wsu").addTextNode(timestamp);
					soap_timestamp.addChildElement("Expires", "wsu")
							.addTextNode(sdf.format(new Date(curdate.getTime() + 1000 * 30)));
					;

					SOAPElement usernameToken = security.addChildElement("UsernameToken", "wsse");

					SOAPElement username = usernameToken.addChildElement("Username", "wsse");
					username.addTextNode(user);

					SOAPElement password = usernameToken.addChildElement("Password", "wsse");

					// Digest
					// String dig=calculatePasswordDigest(nonceBytes, timestamp, pass);
					// password.setAttribute("Type",
					// "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest");
					// password.addTextNode(dig);

					password.setAttribute("Type",
							"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
					password.addTextNode(pass);

					SOAPElement nonce = usernameToken.addChildElement("Nonce", "wsse");
					nonce.setAttribute("EncodingType",
							"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary");
					nonce.addTextNode(DatatypeConverter.printBase64Binary(nonceBytes));

					SOAPElement created = usernameToken.addChildElement("Created", "wsu");

					// timestamp = sdf.format(new Date());
					created.addTextNode(timestamp);
				}
			} catch (final Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	// Other required methods on interface need no guts

	@Override
	public boolean handleFault(SOAPMessageContext context) {
		return false;
	}

	@Override
	public void close(MessageContext context) {
	}

	// @Override
	// public Set<QName> getHeaders() {
	// return null;
	// }

	@Override
	public Set<QName> getHeaders() {
		QName securityHeader = new QName(
				"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "Security");
		HashSet<QName> headers = new HashSet<QName>();
		headers.add(securityHeader);
		return headers;
	}

}
