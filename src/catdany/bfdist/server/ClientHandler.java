package catdany.bfdist.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import catdany.bfdist.log.BFLog;

public class ClientHandler implements Runnable
{
	public final Socket socket;
	public final int id;
	public final BFServer server;
	
	private Thread handlerThread;
	private BufferedReader in;
	private PrintWriter out;
	
	public ClientHandler(int id, Socket connectedClient, BFServer server)
	{
		this.id = id;
		this.socket = connectedClient;
		this.server = server;
		try
		{
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.out = new PrintWriter(socket.getOutputStream(), true);
			this.handlerThread = new Thread(this, "ClientHandler-" + id);
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
			while ((read = in.readLine()) != null)
			{
				BFLog.d("Received message from client: %s", read);
			}
		}
		catch (Exception t)
		{
			BFLog.t(t);
			BFLog.w("Error occurred during client handling. Client dropped.");
			server.kick(this);
		}
	}
	
	public void send(String msg)
	{
		out.println(msg);
		BFLog.d("Sent '%s' to client %s", msg, id);
	}
}