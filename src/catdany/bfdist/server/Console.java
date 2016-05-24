package catdany.bfdist.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.xml.bind.DatatypeConverter;

import catdany.bfdist.BFHelper;
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
				int maxBytes = 8192;
				if (read.startsWith("start"))
				{
					String[] split = read.split(" ");
					BigInteger beginAt = new BigInteger(split[1]);
					server.clientBuffer = new BigInteger(split[2]);
					long autoReportInterval = Long.parseLong(split[3]);
					server.autoReportInterval = autoReportInterval;
					BFLog.i("Set auto-report interval to %s ms", autoReportInterval);
					server.freeInterval = beginAt;
					BFLog.i("Unallocated interval is set to [%s...inf]", server.freeInterval);
					for (ClientHandler i : server.getClients())
					{
						server.allocate(server.clientBuffer, i);
					}
					server.showContinueWarning = false;
				}
				else if (read.startsWith("continue"))
				{
					if (server.showContinueWarning && !read.startsWith("continuex"))
					{
						BFLog.w("Attempted to continue allocated intervals, but one or more of the intervals was not restored successfully. If you wish to forcefully proceed, use 'continuex [autoReportInterval:long]'");
					}
					else
					{
						String[] split = read.split(" ");
						server.autoReportInterval = Long.parseLong(split[1]);
						for (ClientHandler i : server.getClients())
						{
							server.allocateContinue(i);
						}
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
					}
				}
				else
				{
					BFLog.w("Unknown command.");
					BFLog.w("Calculate new: start [beginAt:BigInteger] [clientBuffer:BigInteger] [autoReportInterval:long]");
					BFLog.w("Continue calculation: continue [autoReportInterval:long]");
					BFLog.w("Save progress and close server: x");
				}
				//FIXME:Remove old code
				if (read.startsWith("rng"))
				{
					if (read.length() > 4 && BFHelper.isInteger(read.substring(4)) && Integer.parseInt(read.substring(4)) > 0 && Integer.parseInt(read.substring(4)) <= maxBytes)
					{
						if (server.rngData == null)
						{
							ArrayList<ClientHandler> clients = server.getClients();
							if (!clients.isEmpty())
							{
								int comps = clients.size();
								int amount = Integer.parseInt(read.substring(4));
								int extra = amount % comps;
								int amountPerClient = amount / comps;
								server.rngData = ByteBuffer.allocate(amount);
								for (int i = 0; i < comps; i++)
								{
									ClientHandler c = clients.get(i);
									int a = amountPerClient + (i < extra ? 1 : 0);
									if (a > 0)
									{
										c.send("RANDOM " + a);
									}
								}
							}
							else
							{
								BFLog.w("No clients are available.");
							}
						}
						else
						{
							BFLog.w("%s bytes were requested earlier. Unable to queue another request until the current one is complete. To cancel this, use 'cancelrng' command.", server.rngData.capacity());
						}
					}
					else
					{
						BFLog.w("Console attempted to execute command 'rng x' but arguments didn't meet the requirement. x must be an integer within range [1;%s]", maxBytes);
					}
				}
				if (read.startsWith("cancelrng"))
				{
					server.rngData = null;
					BFLog.d("Random data request cancelled.");
				}
				if (read.startsWith("hex>iso "))
				{
					BFLog.d("hex>iso: %s", new String(DatatypeConverter.parseHexBinary(read.substring(8)), BFHelper.charset));
				}
				if (read.startsWith("iso>hex "))
				{
					BFLog.d("iso>hex: %s", DatatypeConverter.printHexBinary(read.substring(8).getBytes(BFHelper.charset)));
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