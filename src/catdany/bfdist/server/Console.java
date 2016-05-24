package catdany.bfdist.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;

import catdany.bfdist.log.BFLog;

public class Console implements Runnable
{
	public final Thread consoleThread;
	private BFServer server;
	
	public Console(BFServer server)
	{
		this.server = server;
		this.consoleThread = new Thread(this, "Server-Console");
		consoleThread.start();
	}
	
	@Override
	public void run()
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while (true)
		{
			try
			{
				String read = in.readLine();
				BFLog.logToFile("[SYSIN] " + read);
				if (read.startsWith("init"))
				{
					String[] split = read.split(" ");
					BigInteger beginAt = new BigInteger(split[1]);
					server.clientBuffer = new BigInteger(split[2]);
					long autoReportInterval = Long.parseLong(split[3]);
					server.autoReportTimer = autoReportInterval;
					BFLog.i("Set auto-report interval to %s ms", autoReportInterval);
					server.freeInterval = beginAt;
					BFLog.i("Free interval is set to [%s...inf]", server.freeInterval);
					for (ClientHandler i : server.getClients())
					{
						server.allocate(server.clientBuffer, i);
					}
				}
				else if (read.equals("x"))
				{
					BFLog.i("Console requested save-and-exit.");
					if (server.shutdown)
					{
						BFLog.w("Server is already in process of shutting down.");
					}
					else
					{
						server.saveServerIntervals();
						server.shutdown = true;
						server.sendToAll("SHUTDOWN");
						if (server.getClients().size() == 0)
						{
							BFLog.exit("Console requested save-and-exit.");
						}
					}
				}
				else
				{
					BFLog.w("Unknown command.");
					BFLog.i("Command list:");
					BFLog.i("-- Initialize calculation: init [beginAt:BigInteger] [clientBuffer:BigInteger] [autoReportInterval:long]");
					BFLog.i("-- Save progress and close server: x");
				}
			}
			catch (IOException t)
			{
				BFLog.t(t);
				BFLog.e("Unable to read from console.");
			}
			catch (Exception t)
			{
				BFLog.t(t);
				BFLog.e("Internal console error.");
			}
		}
	}
}