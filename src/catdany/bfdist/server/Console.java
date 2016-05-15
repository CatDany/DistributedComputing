package catdany.bfdist.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
		}
	}
}