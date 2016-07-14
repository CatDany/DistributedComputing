package catdany.bfdist.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import catdany.bfdist.log.BFLog;
import catdany.bfdist.server.report.Reporter;

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
				if (read.startsWith("init "))
				{
					String[] split = read.split(" ");
					BigInteger beginAt = new BigInteger(split[1]);
					server.clientBuffer = new BigInteger(split[2]);
					server.autoEmailReportTimer = Long.parseLong(split[3]);
					server.maxSteps = Integer.parseInt(split[4]);
					server.freeInterval = beginAt;
					BFLog.i("Free interval is set to [%s...inf]", server.freeInterval);
					server.scheduledEmailReporter = Reporter.startOnSchedule(1, server.autoEmailReportTimer, TimeUnit.MINUTES);
					BFLog.i("Scheduled auto e-mail report in %s %s", server.autoEmailReportTimer, TimeUnit.MINUTES);
					File currentDir = new File(".");
					for (File i : currentDir.listFiles())
					{
						String filename = i.getName();
						if (filename.startsWith("INTERVAL_") && filename.length() >= "INTERVAL_2bb20e1c-110c-39dc-9769-c5a151430a06.txt".length())
						{
							try
							{
								Files.delete(i.toPath());
								BFLog.d("Client interval file deleted: %s", filename);
							}
							catch (Exception t)
							{
								BFLog.w("Couldn't delete client interval file: %s", filename);
								BFLog.t(t);
							}
						}
					}
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
					BFLog.i("-- Initialize calculation: init [beginAt:BigInteger] [clientBuffer:BigInteger] [emailReportTimerMinutes:int] [maxSteps:int]");
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