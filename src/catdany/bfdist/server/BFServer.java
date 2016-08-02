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
	
	public int coefIndex = 0;
	public int[] coefFirstArray;
	public int[] coefSecondArray;
	
	public boolean shutdown = false;
	
	/**
	 * If <code>true</code> then {@link #allocate(BigInteger, ClientHandler)} will no longer allocate intervals.<br>
	 * Set to <code>true</code> when <code>{@link #coefIndex} >= {@link #coefFirstArray}.length</code>
	 */
	public boolean stopAllocation = false;
	
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
		loadCoefficientsFromFile();
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
		if (!stopAllocation)
		{
			client.current = amount.toString();
			client.lastCompLogNumber = freeInterval.toString();
			client.lastCompLogTime = System.currentTimeMillis();
			try
			{
				client.setCompLog(Main.anplusb(coefFirstArray[coefIndex], coefSecondArray[coefIndex]) + "_" + client.id.toString());
			} catch (IOException e) {
				BFLog.e("Couldn't create a complog");
				BFLog.t(e);
			}
			client.coefFirst = coefFirstArray[coefIndex];
			client.coefSecond = coefSecondArray[coefIndex];
			client.send("CSPSTART " + client.coefFirst + " " + client.coefSecond + " " + maxSteps + " " + freeInterval.toString() + " " + client.current);
			client.max = freeInterval.add(amount).toString();
			BFLog.i("Allocated [%s...%s] to %s", freeInterval, client.max, client);
			//freeInterval = freeInterval.add(amount); XXX:freeInterval++
			BFLog.i("Free interval is set to [%s...inf]", freeInterval);
			
			coefIndex++;
			if (coefIndex >= coefFirstArray.length)
			{
				stopAllocation = true;
			}
		}
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
			client.setCompLog(Main.anplusb(client.coefFirst, client.coefSecond) + "_" + client.id.toString());
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
			p.println(coefIndex);
			BFLog.i("Saved free interval [%s...inf]", freeInterval);
			BFLog.i("Saved client buffer (%s)", clientBuffer);
			BFLog.i("Saved auto e-mail report time (%s)", autoEmailReportTimer);
			BFLog.i("Saved max steps for 1 calculation (%s)", maxSteps);
			BFLog.i("Saved coef index (%s)", coefIndex);
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
			if (serverIntervalLines.size() >= 5)
			{
				String freeIntervalStr = serverIntervalLines.get(0);
				String clientBufferStr = serverIntervalLines.get(1);
				String autoEmailReportTimerStr = serverIntervalLines.get(2);
				String maxStepsStr = serverIntervalLines.get(3);
				String coefIndexStr = serverIntervalLines.get(4);
				freeInterval = new BigInteger(freeIntervalStr);
				clientBuffer = new BigInteger(clientBufferStr);
				autoEmailReportTimer = Long.parseLong(autoEmailReportTimerStr);
				maxSteps = Integer.parseInt(maxStepsStr);
				coefIndex = Integer.parseInt(coefIndexStr);
				BFLog.i("Restored free interval [%s...inf]", freeIntervalStr);
				BFLog.i("Restored client buffer (%s)", clientBufferStr);
				BFLog.i("Restored auto e-mail report time (%s)", autoEmailReportTimerStr);
				BFLog.i("Restored max steps for 1 calculation (%s)", maxStepsStr);
				BFLog.i("Restored coef index (%s)", coefIndexStr);
				scheduledEmailReporter = Reporter.startOnSchedule(1, autoEmailReportTimer, TimeUnit.MINUTES);
			}
		}
		catch (IOException t)
		{
			BFLog.e("Unable to restore settings and progress.");
			BFLog.t(t);
		}
	}
	
	private void loadCoefficientsFromFile()
	{
		try
		{
			List<String> coefLines = Files.readAllLines(new File("COEF.txt").toPath());
			coefFirstArray = new int[coefLines.size()];
			coefSecondArray = new int[coefLines.size()];
			for (int i = 0; i < coefLines.size(); i++)
			{
				String[] splitLine = coefLines.get(i).split(" ", 2);
				coefFirstArray[i] = Integer.parseInt(splitLine[0]);
				coefSecondArray[i] = Integer.parseInt(splitLine[1]);
			}
			BFLog.i("Restored %s coefficients.", coefLines.size());
		}
		catch (IOException | NumberFormatException | ArrayIndexOutOfBoundsException t)
		{
			BFLog.e("Unable to load coefficients from file.");
			BFLog.t(t);
		}
	}
}