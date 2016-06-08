package catdany.bfdist.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import catdany.bfdist.Main;
import catdany.bfdist.log.BFLog;
import catdany.bfdist.server.report.Reporter;

public class BFServer implements Runnable
{
	private static BFServer instance;
	
	public final int port;
	private ServerSocket socket;
	private Console console = new Console(this);
	private ArrayList<ClientHandler> clients = new ArrayList<ClientHandler>();
	private Thread serverThread;
	public ScheduledFuture<?> scheduledEmailReporter;

	/**
	 * Write to compLog every N ms
	 */
	@Deprecated
	long autoCompLogTimer = 0;
	int maxSteps = 0;
	@Deprecated
	public long autoReportTimer;
	public long autoEmailReportTimer;
	public BigInteger freeInterval;
	public BigInteger clientBuffer;
	
	public int coefFirst;
	public int coefSecond;
	
	public boolean shutdown = false;
	
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
		this.serverThread = new Thread(this, "SocketAcceptor");
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
	public synchronized void kick(ClientHandler client)
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
	public ArrayList<ClientHandler> getClients()
	{
		return clients;
	}
	
	/**
	 * Allocate an interval for a client to calculate
	 * @param amount
	 * @param client
	 */
	public synchronized void allocate(BigInteger amount, ClientHandler client)
	{
		client.current = amount.toString();
		client.lastCompLogNumber = freeInterval.toString();
		client.lastCompLogTime = System.currentTimeMillis();
		try
		{
			client.setCompLog(Main.anplusb(coefFirst, coefSecond) + "_" + client.id.toString());
		} catch (IOException e) {
			BFLog.e("Couldn't create a complog");
			BFLog.t(e);
		}
		client.send("CSPSTART " + coefFirst + " " + coefSecond + " " + maxSteps + " " + freeInterval.toString() + " " + client.current);
		client.max = freeInterval.add(amount).toString();
		BFLog.i("Allocated [%s...%s] to %s", freeInterval, client.max, client);
		freeInterval = freeInterval.add(amount);
		BFLog.i("Free interval is set to [%s...inf]", freeInterval);
	}
	
	/**
	 * Allocate the interval from save
	 * @param client
	 */
	public synchronized void allocateContinue(ClientHandler client)
	{
		BigInteger current = new BigInteger(client.current);
		BigInteger max = new BigInteger(client.max);
		client.lastCompLogNumber = client.current;
		client.lastCompLogTime = System.currentTimeMillis();
		try
		{
			client.setCompLog(Main.anplusb(coefFirst, coefSecond) + "_" + client.id.toString());
		} catch (IOException e) {
			BFLog.e("Couldn't create a complog");
			BFLog.t(e);
		}
		client.send("CSPSTART " + client.coefFirst + " " + client.coefSecond + " " + maxSteps + " " + client.current + " " + max.subtract(current));
		BFLog.i("Allocated [%s...%s] to %s", client.current, client.max, client);
	}
	
	void saveServerIntervals()
	{
		try (PrintWriter p = new PrintWriter(new File("INTERVAL_SERVER.txt")))
		{
			String freeInterval = this.freeInterval.toString();
			String clientBuffer = this.clientBuffer.toString();
			p.println(freeInterval);
			p.println(clientBuffer);
			p.println(autoEmailReportTimer);
			p.println(maxSteps);
			p.println(coefFirst);
			p.println(coefSecond);
			BFLog.i("Saved free interval [%s...inf]", freeInterval);
			BFLog.i("Saved client buffer (%s)", clientBuffer);
			BFLog.i("Saved auto e-mail report time (%s)", autoEmailReportTimer);
			BFLog.i("Saved max steps for 1 calculation (%s)", maxSteps);
			BFLog.i("Saved coefficients (%s)", Main.anplusb(coefFirst, coefSecond));
		}
		catch (FileNotFoundException t)
		{
			BFLog.e("Couldn't save settings and progress.");
			BFLog.t(t);
		}
	}
	
	void restoreServerIntervals()
	{
		try
		{
			List<String> serverIntervalLines = Files.readAllLines(new File("INTERVAL_SERVER.txt").toPath());
			if (serverIntervalLines.size() >= 6)
			{
				String freeIntervalStr = serverIntervalLines.get(0);
				String clientBufferStr = serverIntervalLines.get(1);
				String autoEmailReportTimerStr = serverIntervalLines.get(2);
				String maxStepsStr = serverIntervalLines.get(3);
				String coefFirstStr = serverIntervalLines.get(4);
				String coefSecondStr = serverIntervalLines.get(5);
				freeInterval = new BigInteger(freeIntervalStr);
				clientBuffer = new BigInteger(clientBufferStr);
				autoEmailReportTimer = Long.parseLong(autoEmailReportTimerStr);
				maxSteps = Integer.parseInt(maxStepsStr);
				coefFirst = Integer.parseInt(coefFirstStr);
				coefSecond = Integer.parseInt(coefSecondStr);
				BFLog.i("Restored free interval [%s...inf]", freeIntervalStr);
				BFLog.i("Restored client buffer (%s)", clientBufferStr);
				BFLog.i("Restored auto e-mail report time (%s)", autoEmailReportTimer);
				BFLog.i("Restored max steps for 1 calculation (%s)", maxStepsStr);
				BFLog.i("Restored coefficients (%s)", Main.anplusb(coefFirst, coefSecond));
				scheduledEmailReporter = Reporter.startOnSchedule(1, autoEmailReportTimer, TimeUnit.MINUTES);
			}
		}
		catch (IOException t)
		{
			BFLog.e("Unable to restore settings and progress.");
			BFLog.t(t);
		}
	}
}