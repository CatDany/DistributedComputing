package catdany.bfdist.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
				server.sendToAll(read);
			}
			catch (IOException t)
			{
				BFLog.t(t);
				BFLog.e("Unable to read from console.");
			}
		}
	}
}