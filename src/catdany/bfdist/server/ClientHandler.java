package catdany.bfdist.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;

import javax.xml.bind.DatatypeConverter;

import catdany.bfdist.BFHelper;
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
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), BFHelper.charset));
			this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), BFHelper.charset), true);
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
				if (read.startsWith("RNGDATA_START") && BFHelper.isInteger(read.substring(14)) && server.rngData != null)
				{
					ByteBuffer bb = ByteBuffer.allocate(Integer.parseInt(read.substring(14)));
					while (bb.hasRemaining())
					{
						String read0;
						if ((read0 = in.readLine()) != null)
						{
							bb.put(read0.getBytes(BFHelper.charset));
							if (bb.hasRemaining())
							{
								bb.put("\n".getBytes(BFHelper.charset));
							}
						}
					}
					bb.position(0);
					server.rngData.put(bb);
					if (!server.rngData.hasRemaining())
					{
						BFLog.i("Random data (%s bytes):", server.rngData.position());
						BFLog.i(DatatypeConverter.printHexBinary(server.rngData.array()));
						server.rngData = null;
					}
				}
			}
		}
		catch (Exception t)
		{
			BFLog.t(t);
			BFLog.w("Error occurred during client handling. Client dropped.");
			server.kick(this);
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
}