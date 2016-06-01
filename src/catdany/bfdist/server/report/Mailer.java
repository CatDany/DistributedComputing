package catdany.bfdist.server.report;

import java.io.File;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;

import catdany.bfdist.log.BFLog;

public class Mailer
{
	private final Properties props;
	
	public Mailer()
	{
		props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");
	}
	
	public void send(Session session, String sendTo, String sendFrom, String subject, String body, File attachment) throws MessagingException
	{
		// Headers
		MimeMessage m = new MimeMessage(session);
		m.setFrom(new InternetAddress(sendFrom));
		m.setRecipient(RecipientType.TO, new InternetAddress(sendTo));
		m.setSubject(subject);
		// Text Body
		MimeBodyPart partBodyText = new MimeBodyPart();
		partBodyText.setText(body);
		MimeBodyPart partBodyFile = new MimeBodyPart();
		if (attachment != null)
		{
			FileDataSource fileDataSource = new FileDataSource(attachment);
			partBodyFile.setDataHandler(new DataHandler(fileDataSource));
			partBodyFile.setFileName(attachment.getName());
		}
		// Combining
		MimeMultipart multi = new MimeMultipart();
		multi.addBodyPart(partBodyText);
		multi.addBodyPart(partBodyFile);
		m.setContent(multi);
		// Send
		Transport.send(m);
	}
	
	public void sendTry(Session session, String sendTo, String sendFrom, String subject, String body, File attachment, int attempts)
	{
		for (int i = 1; i <= attempts; i++)
		{
			try
			{
				send(session, sendTo, sendFrom, subject, body, attachment);
				break;
			}
			catch (MessagingException t)
			{
				BFLog.w("Unable to send an e-mail. Doing %s more attempts...", attempts - i);
				BFLog.t(t);
				if (i == attempts)
				{
					BFLog.e("Unable to send an e-mail after %s attempts", i);
				}
			}
		}
	}
	
	public Session getSession(String username, String password)
	{
		return Session.getInstance(props, new MailPasswordAuth(username, password));
	}
}