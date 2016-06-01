package catdany.bfdist.server.report;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class MailPasswordAuth extends Authenticator
{
	public final String username;
	public final String password;
	
	public MailPasswordAuth(String username, String password)
	{
		this.username = username;
		this.password = password;
	}
	
	@Override
	protected PasswordAuthentication getPasswordAuthentication()
	{
		return new PasswordAuthentication(username, password);
	}
}