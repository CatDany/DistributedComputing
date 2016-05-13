package catdany.bfdist.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import catdany.bfdist.Main;
import catdany.bfdist.log.BFLog;

public class BFServer implements Runnable
{
	private static BFServer instance;
	
	public final int port;
	private ServerSocket socket;
	private Console console = new Console(this);
	private ArrayList<ClientHandler> clients = new ArrayList<ClientHandler>();
	private Thread serverThread;
	public ByteBuffer rngData;
	
	private BFServer(int port)
	{
		this.port = port;
		try
		{
			socket = new ServerSocket(port);
			BFLog.i("Bound to port %s.", port);
		}
		catch (IOException t)
		{
			BFLog.t(t);
			BFLog.exit("Unable to bind to port %s.", port);
		}
		catch (IllegalArgumentException t)
		{
			BFLog.t(t);
			BFLog.exit("Port %s is out of range (0-65535).", port);
		}
		this.serverThread = new Thread(this, "Server");
		serverThread.start();
	}
	
	/**
	 * Creates a new instance of {@link BFServer} for given port.<br>
	 * Saves it in {@link BFServer#instance}, so you can refer to it later.
	 * @param port
	 * @return
	 * @see BFServer#getServer()
	 */
	public static BFServer instantiate(int port)
	{
		instance = new BFServer(port);
		return instance;
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				Socket connectedClient = socket.accept();
				int id = clients.size();
				ClientHandler ch = new ClientHandler(id, connectedClient, this);
				clients.add(ch);
				BFLog.i("Client connected (id: %s, from: %s)", id, socket.getInetAddress().getCanonicalHostName());
			}
			catch (IOException t)
			{
				BFLog.t(t);
				BFLog.w("Client couldn't connect.");
			}
		}
	}
	
	/**
	 * Get {@link BFServer} instance.<br>
	 * Will throw an exception if called on client-side.
	 * @return
	 * {@link BFServer} instance if called on server-side<br>
	 * <code>null</code> if called on client-side
	 * @see BFServer#instantiate(InetAddress, int)
	 * @return
	 */
	public static BFServer getServer()
	{
		if (Main.getSide().isServer())
		{
			return instance;
		}
		else
		{
			BFLog.t("getServer() called on client-side");
			return null;
		}
	}
	
	/**
	 * Send a message to all connected clients
	 * @param msg
	 */
	public void sendToAll(String msg)
	{
		for (ClientHandler i : clients)
		{
			i.send(msg);
		}
	}
	
	/**
	 * Kick connected client
	 * @param client
	 */
	public void kick(ClientHandler client)
	{
		if (clients.contains(client))
		{
			clients.remove(client);
			BFLog.d("Client kicked (%s)", client.id);
		}
		else
		{
			BFLog.d("Attempted to remove a client that is not connected.");
		}
	}
	
	/**
	 * Get {@link Console} object
	 * @return
	 */
	protected Console getConsole()
	{
		return console;
	}
	
	/**
	 * Get all connected clients<br>
	 * <b>Warning:</b> use {@link ArrayList#clone()} you intend to modify it
	 * @return
	 */
	protected ArrayList<ClientHandler> getClients()
	{
		return clients;
	}
}