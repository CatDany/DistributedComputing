package catdany.bfdist.server;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.xml.bind.DatatypeConverter;

import catdany.bfdist.BFHelper;
import catdany.bfdist.log.BFLog;

public class ClientHandler implements Runnable
{
	public final Socket socket;
	public final int id;
	public final BFServer server;
	
	private Thread handlerThread;
	private InputStream inStream;
	private BufferedReader in;
	private PrintWriter out;
	
	public ClientHandler(int id, Socket connectedClient, BFServer server)
	{
		this.id = id;
		this.socket = connectedClient;
		this.server = server;
		try
		{
			this.inStream = socket.getInputStream();
			this.in = new BufferedReader(new InputStreamReader(inStream));
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
				if (read.startsWith("RNGDATA_START") && BFHelper.isInteger(read.substring(14)))
				{
					byte[] bytes = DatatypeConverter.parseHexBinary(in.readLine());
					server.rngData.put(bytes);
					if (server.rngData.remaining() == 0)
					{
						BFLog.i("Random data (%s bytes):", server.rngData.limit());
						BFLog.i(DatatypeConverter.printHexBinary(server.rngData.array()));
					}
					/* XXX: Make it twice more efficient
					byte[] bytes = new byte[Integer.parseInt(read.substring(14))];
					int off = 0;
					while (off < bytes.length)
					{
						int available = inStream.available();
						if (available > 0)
						{
							inStream.read(bytes, off, (off + available > bytes.length ? bytes.length - off : available));
							off += available;
							BFLog.d("ClientHandler line 64 > %s", BFHelper.toHex(bytes));
						}
					}
					server.rngData.put(bytes);
					*/
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