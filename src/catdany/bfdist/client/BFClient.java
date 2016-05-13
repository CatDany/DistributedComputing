package catdany.bfdist.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import catdany.bfdist.Main;
import catdany.bfdist.log.BFLog;

public class BFClient
{
	private static BFClient instance;
	
	/**
	 * Server communicator
	 */
	private ServerCom com;
	/**
	 * Server socket
	 */
	private Socket socket;
	
	public BFClient(InetAddress ip, int port)
	{
		connect(ip, port);
	}
	
	/**
	 * Creates a new instance of {@link BFClient} for given {@link InetAddress} and port.<br>
	 * Saves it in {@link BFClient#instance}, so you can refer to it later.
	 * @param ip
	 * @param port
	 * @see BFClient#getClient()
	 */
	public static void instantiate(InetAddress ip, int port)
	{
		instance = new BFClient(ip, port);
	}
	
	/**
	 * Connects to the server and creates a thread to keep this connection
	 * @param ip
	 * @param port
	 */
	public void connect(InetAddress ip, int port)
	{
		try
		{
			socket = new Socket(ip, port);
			this.com = new ServerCom(socket);
			BFLog.i("Connected to server: %s", socket.getRemoteSocketAddress().toString());
		}
		catch (IOException t)
		{
			BFLog.t(t);
			BFLog.exit("Unable to create client socket on client-side.");
		}
	}
	
	/**
	 * Get server communicator
	 * @return
	 */
	public ServerCom getServerCom()
	{
		return com;
	}
	
	/**
	 * Get {@link BFClient} instance.<br>
	 * Will throw an exception if called on server-side.
	 * @return
	 * {@link BFClient} instance if called on client-side<br>
	 * <code>null</code> if called on server-side
	 * @see BFClient#instantiate(InetAddress, int)
	 */
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