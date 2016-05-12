package catdany.bfdist.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;

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
				if (read.startsWith("rng") && BFHelper.isInteger(read.substring(4)))
				{
					ArrayList<ClientHandler> clients = server.getClients();
					int comps = clients.size();
					int amount = Integer.parseInt(read.substring(4));
					int extra = amount % comps;
					int amountPerClient = amount / comps;
					server.rngData = ByteBuffer.allocate(amount);
					for (int i = 0; i < comps; i++)
					{
						ClientHandler c = clients.get(i);
						int a = amountPerClient + (i < extra ? 1 : 0);
						c.send("RANDOM " + a);
					}
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