package cz.bliksoft.javautils.net;

import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * To uise please add
 * 
 * <pre>
 * <dependency>
 *  <groupId>com.sun.mail</groupId>
 *  <artifactId>javax.mail</artifactId>
 *  <version>1.6.2</version>
 * </dependency>
 * </pre>
 * 
 * to MVN dependencies.
 * 
 * @author jakub
 *
 */
public class Mail {
	
	private Mail() {
		
	}
	
	private static Logger log = Logger.getLogger(Mail.class.getName());

	private static String host = "localhost";

	public static void setHost(String host) {
		Mail.host = host;
	}

	public static boolean sendMail(String from, String subject, String messageBody, String... recipients) {
		//		String host = "localhost";//or IP address  

		//Get the session object  
		Properties properties = System.getProperties();
		properties.setProperty("mail.smtp.host", host);
		Session session = Session.getDefaultInstance(properties);

		//compose the message  
		try {
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			for (String addr : recipients) {
				message.addRecipient(Message.RecipientType.TO, new InternetAddress(addr));
			}
			message.setSubject(subject);
			message.setText(messageBody);

			// Send message  
			Transport.send(message);
			log.info("Message sent.");
			return true;
		} catch (MessagingException mex) {
			log.severe("Sending message failed: " + mex.getMessage());
			return false;
		}
	}
}
