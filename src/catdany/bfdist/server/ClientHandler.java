package catdany.bfdist.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import catdany.bfdist.BFHelper;
import catdany.bfdist.log.BFLog;

public class ClientHandler implements Runnable
{
	public final Socket socket;
	public final BFServer server;
	public UUID id;
	
	private Thread handlerThread;
	private BufferedReader in;
	private PrintWriter out;
	
	public PrintWriter compLogger;

	public String current = null;
	public String max = null;
	
	public ClientHandler(Socket connectedClient, BFServer server)
	{
		this.socket = connectedClient;
		this.server = server;
		try
		{
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), BFHelper.charset));
			this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), BFHelper.charset), true);
			this.handlerThread = new Thread(this, "ClientHandler-" + connectedClient.getRemoteSocketAddress().toString());
			handlerThread.start();
		}
		catch (Exception t)
		{
			BFLog.t(t);
			BFLog.w("Couldn't get I/O stream from client.");
		}
	}
	
	@Override
	public void run()
	{
		try
		{
			String read;
			while (!server.shutdown && (read = in.readLine()) != null)
			{
				BFLog.d("Received message from client: %s", read);
				if (read.startsWith("UUID"))
				{
					id = UUID.fromString(read.substring("UUID ".length()));
					BFLog.i("Client %s identified. UUID: %s", socket.getRemoteSocketAddress().toString(), id);
					handlerThread.setName(handlerThread.getName() + "-" + id);
					restoreClientInterval();
					compLogger = new PrintWriter(new FileWriter("COMPLOG_" + id + ".txt", true), true);
					if (server.freeInterval != null && server.clientBuffer != null)
					{
						if (current != null && max != null)
						{
							server.allocateContinue(this);
						}
						else
						{
							server.allocate(server.clientBuffer, this);
						}
					}
				}
				else if (read.startsWith("SPCOMPLETE"))
				{
					BFLog.i("%s has completed its calculation.", this);
					server.allocate(server.clientBuffer, this);
				}
				else if (read.startsWith("SPDONE"))
				{
					String[] split = read.split(" ");
					current = split[2];
					compLog("Done after %s ms >> %s", split[1], split[2]);
				}
				else if (read.startsWith("SPTIME"))
				{
					String[] split = read.split(" ");
					compLog("Report after %s ms >> %s", split[1], split[2]);
				}
			}
			if (!server.shutdown)
			{
				throw new RuntimeException("readLine() = null");
			}
		}
		catch (Exception t)
		{
			BFLog.t(t);
			BFLog.w("Error occurred during client handling. Client dropped.");
			server.kick(this);
		}
		// Closing resources
		if (compLogger != null)
		{
			compLogger.close();
			BFLog.d("Closed compLogger stream.");
		}
		saveClientIntervals();
		server.kick(this);
		if (server.shutdown && server.getClients().size() == 0)
		{
			BFLog.exit("Console requested save-and-exit.");
		}
	}
	
	/**
	 * Send a message to this client
	 * @param msg
	 */
	public void send(String msg)
	{
		out.println(msg);
		BFLog.d("Sent '%s' to client %s", msg, id);
	}
	
	public void compLog(String format, Object... args)
	{
		String date = BFHelper.dateFormatLog.format(new Date());
		compLogger.println(String.format("[%s] %s", date, String.format(format, args)));
	}
	
	@Override
	public String toString()
	{
		return "Client(" + socket.getRemoteSocketAddress().toString() + ")(" + id + ")";
	}
	
	private void saveClientIntervals()
	{
		try (PrintWriter p = new PrintWriter(new File("INTERVAL_" + id + ".txt")))
		{
			p.println(current);
			p.println(max);
			BFLog.i("Saved allocated interval [%s...%s]", current, max);
		}
		catch (FileNotFoundException t)
		{
			BFLog.e("Couldn't save progress.");
			BFLog.t(t);
		}
	}
	
	private void restoreClientInterval()
	{
		File saveFile = new File("INTERVAL_" + id + ".txt");
		if (saveFile.exists())
		{
			try
			{
				List<String> saveLines = Files.readAllLines(saveFile.toPath());
				if (saveLines.size() >= 2)
				{
					current = saveLines.get(0);
					max = saveLines.get(1);
					BFLog.i("Restored allocated interval from save: [%s...%s]", current, max);
				}
			}
			catch (IOException t)
			{
				BFLog.t(t);
				BFLog.e("Unable to restore allocated interval.");
			}
		}
	}
}