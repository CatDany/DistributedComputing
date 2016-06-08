package catdany.bfdist.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;

import javax.xml.bind.DatatypeConverter;

import catdany.bfdist.BFHelper;
import catdany.bfdist.Main;
import catdany.bfdist.log.BFException;
import catdany.bfdist.log.BFLog;

public class ServerCom implements Runnable, Sender
{
	public final Socket socket;
	
	private Thread comThread;
	private BufferedReader in;
	private PrintWriter out;
	
	private Thread solverThread;
	private CustomSolver solver;
	
	@SuppressWarnings("unused")
	private PingTimer ping;
	
	public ServerCom(Socket socket)
	{
		this.socket = socket;
		try
		{
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), BFHelper.charset));
			this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), BFHelper.charset), true);
			this.comThread = new Thread(this, "Client-ServerCom");
			comThread.start();
			ping = new PingTimer(Main.CLIENT_PING_TIME, this);
		}
		catch (IOException t)
		{
			BFLog.t(t);
			BFLog.exit("Couldn't get I/O from server.");
		}
	}
	
	@Override
	public void run()
	{
		try
		{
			String read;
			while ((read = in.readLine()) != null)
			{
				BFLog.d("Received message from server: %s", read);
				if (read.startsWith("CSPSTART"))
				{
					if (solverThread != null)
					{
						solverThread.interrupt();
						solver = null;
					}
					String[] split = read.split(" ");
					BigInteger start = new BigInteger(split[4]);
					BigInteger end = new BigInteger(split[4]).add(new BigInteger(split[5]));
					int coefFirst = Integer.parseInt(split[1]);
					int coefSecond = Integer.parseInt(split[2]);
					solver = new CustomSolver(Integer.parseInt(split[3]), start, end, coefFirst, coefSecond, this);
					solverThread = new Thread(solver, "Custom-Solver");
					solverThread.setPriority(Thread.MAX_PRIORITY);//XXX: Solver Thread Priority
					solverThread.start();
					BFLog.i("Started Solver on an interval [%s...%s] for %s", split[4], end, Main.anplusb(coefFirst, coefSecond));
				}
				else if (read.equals("SHUTDOWN"))
				{
					BFLog.i("Server has been stopped.");
					BFLog.exit("Server has been manually stopped.");
					return;
				}
			}
			throw new BFException("readLine() = null");
		}
		catch (Exception t)
		{
			BFLog.t(t);
			BFLog.e("Error occurred while communicating with server.");
		}
		if (solverThread != null)
			solverThread.interrupt();
		solver = null;
		reconnect();
	}
	
	private void reconnect()
	{
		BFLog.i("Reconnecting to server...");
		BFClient client = BFClient.getClient();
		BFClient.instantiate(client.id, client.ip, client.port);
	}
	
	/**
	 * Send a message to server
	 * @param msg
	 */
	public void sendToServer(String msg)
	{
		out.println(msg);
		BFLog.d("Sent '%s' to server", msg);
	}
	
	/**
	 * Send a byte array to server (temporarily sends hexadecimal encoded byte-array)
	 * @param bytes
	 */
	@Deprecated
	public void sendToServer(byte[] bytes)
	{
		out.println(new String(bytes, BFHelper.charset));
		BFLog.d("Sent %s bytes to server:", bytes.length);
		BFLog.d(DatatypeConverter.printHexBinary(bytes));
	}
}
