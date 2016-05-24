package catdany.bfdist.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.UUID;

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
	
	public final UUID id;
	
	public BFClient(UUID id, InetAddress ip, int port)
	{
		this.id = id;
		connect(ip, port);
	}
	
	/**
	 * Creates a new instance of {@link BFClient} for given {@link InetAddress} and port.<br>
	 * Saves it in {@link BFClient#instance}, so you can refer to it later.
	 * @param ip
	 * @param port
	 * @see BFClient#getClient()
	 */
	public static void instantiate(UUID id, InetAddress ip, int port)
	{
		instance = new BFClient(id, ip, port);
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
			com.sendToServer("UUID " + id);
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