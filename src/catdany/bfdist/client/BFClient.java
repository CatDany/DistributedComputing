package catdany.bfdist.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import catdany.bfdist.Main;
import catdany.bfdist.log.BFLog;

public class BFClient
{
	private static BFClient instance;
	
	private ServerCom com;
	private Socket socket;
	
	public BFClient(InetAddress ip, int port)
	{
		connect(ip, port);
	}
	
	public static void instantiate(InetAddress ip, int port)
	{
		instance = new BFClient(ip, port);
	}
	
	public void connect(InetAddress ip, int port)
	{
		try
		{
			socket = new Socket(ip, port);
			this.com = new ServerCom(socket);
			BFLog.d("Connected to server.");
		}
		catch (IOException t)
		{
			BFLog.t(t);
			BFLog.exit("Unable to create client socket on client-side.");
		}
	}
	
	public static BFClient getClient()
	{
		if (Main.getSide().isClient())
		{
			return instance;
		}
		else
		{
			BFLog.t("getClient() called on server-side");
			return null;
		}
	}
}