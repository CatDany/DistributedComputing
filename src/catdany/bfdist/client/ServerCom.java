package catdany.bfdist.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import javax.xml.bind.DatatypeConverter;

import catdany.bfdist.BFHelper;
import catdany.bfdist.log.BFLog;

public class ServerCom implements Runnable
{
	public final Socket socket;
	
	private Thread comThread;
	private BufferedReader in;
	private OutputStream outStream;
	private PrintWriter out;
	
	public ServerCom(Socket socket)
	{
		this.socket = socket;
		try
		{
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.outStream = socket.getOutputStream();
			this.out = new PrintWriter(outStream, true);
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
				if (read.startsWith("RANDOM") && BFHelper.isInteger(read.substring(7)))
				{
					RNG rng = new RNG();
					rng.start(Integer.parseInt(read.substring(7)));
				}
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
	
	public void sendToServer(byte[] bytes)
	{
		sendToServer(DatatypeConverter.printHexBinary(bytes));
		/* XXX: Make it twice more efficient
		try
		{
			outStream.write(bytes);
			BFLog.d("Sent %s bytes to server:", bytes.length);
			BFLog.d(BFHelper.toHex(bytes));
			outStream.flush();
			//TODO: Bytes to hex and log.d them
		}
		catch (IOException t)
		{
			BFLog.t(t);
			BFLog.e("Couldn't send bytes to server.");
		}
		*/
	}
}
