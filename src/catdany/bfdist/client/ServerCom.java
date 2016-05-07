package catdany.bfdist.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import catdany.bfdist.log.BFLog;

public class ServerCom implements Runnable
{
	public final Socket socket;
	
	private Thread comThread;
	private BufferedReader in;
	private PrintWriter out;
	
	public ServerCom(Socket socket)
	{
		this.socket = socket;
		try
		{
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.out = new PrintWriter(socket.getOutputStream(), true);
			this.comThread = new Thread(this, "Client-ServerCom");
			comThread.start();
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
			}
		}
		catch (Exception t)
		{
			BFLog.t(t);
			BFLog.exit("Error occurred while communicating with server.");
		}
	}
	
	public void sendToServer(String msg)
	{
		out.println(msg);
		BFLog.d("Sent '%s' to server", msg);
	}
}
