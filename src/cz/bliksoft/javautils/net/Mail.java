package cz.bliksoft.javautils.net;

import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Mail {
	private static Logger log = Logger.getLogger(Mail.class.getName());

	private static String host = "localhost";

	public static void setHost(String host) {
		Mail.host = host;
	}

	public static void sendMail(String from, String subject, String messageBody, String... recipients) {
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

		} catch (MessagingException mex) {
			log.severe("Sending message failed: " + mex.getMessage());
		}
	}
}
