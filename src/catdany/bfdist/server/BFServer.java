package catdany.bfdist.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

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
	
	public long autoReportInterval;
	public BigInteger freeInterval;
	public BigInteger clientBuffer;
	
	public boolean shutdown = false;
	public boolean showContinueWarning = false;
	
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
		restoreServerIntervals();
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
				ClientHandler ch = new ClientHandler(connectedClient, this);
				clients.add(ch);
				BFLog.i("Client connected (from: %s)", connectedClient.getRemoteSocketAddress().toString());
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
	
	/**
	 * Allocate an interval for a client to calculate
	 * @param amount
	 * @param client
	 */
	public void allocate(BigInteger amount, ClientHandler client)
	{
		client.current = amount.toString();
		client.send("SPSTART " + autoReportInterval + " " + freeInterval.toString() + " " + client.current);
		client.max = freeInterval.add(amount).toString();
		BFLog.i("Allocated [%s...%s] to %s", freeInterval, client.max, client);
		freeInterval = freeInterval.add(amount);
		BFLog.i("Free interval is set to [%s...inf]", freeInterval);
	}
	
	/**
	 * Allocate the interval from save
	 * @param client
	 */
	public void allocateContinue(ClientHandler client)
	{
		BigInteger current = new BigInteger(client.current);
		BigInteger max = new BigInteger(client.max);
		client.send("SPSTART " + autoReportInterval + " " + client.current + " " + max.subtract(current));
		BFLog.i("Allocated [%s...%s] to %s", client.current, client.max, client);
		BFLog.i("Free interval is set to [%s...inf]", freeInterval);
	}
	
	void saveServerIntervals()
	{
		try (PrintWriter p = new PrintWriter(new File("INTERVAL_SERVER.txt")))
		{
			String freeInterval = this.freeInterval.toString();
			String clientBuffer = this.clientBuffer.toString();
			p.println(freeInterval);
			p.println(clientBuffer);
			BFLog.i("Saved free interval [%s...inf]", freeInterval);
			BFLog.i("Saved client buffer (%s)", clientBuffer);
		}
		catch (FileNotFoundException t)
		{
			BFLog.e("Couldn't save progress.");
			BFLog.t(t);
		}
	}
	
	void restoreServerIntervals()
	{
		try
		{
			List<String> serverIntervalLines = Files.readAllLines(new File("INTERVAL_SERVER.txt").toPath());
			if (serverIntervalLines.size() >= 2)
			{
				freeInterval = new BigInteger(serverIntervalLines.get(0));
				clientBuffer = new BigInteger(serverIntervalLines.get(1));
			}
		}
		catch (IOException t)
		{
			BFLog.e("Unable to restore free interval.");
			BFLog.t(t);
			showContinueWarning = true;
		}
	}
}