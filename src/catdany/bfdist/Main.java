package catdany.bfdist;

import java.net.InetAddress;
import java.net.UnknownHostException;

import catdany.bfdist.client.BFClient;
import catdany.bfdist.log.BFLog;
import catdany.bfdist.server.BFServer;

public class Main
{
	private static final String ARGS_USAGE = "(client|server) (port) [server-ip]";
	
	private static Side side;
	private static int port;
	
	public static void main(String[] args)
	{
		BFLog.i("Started.");
		if (args.length < 2)
		{
			BFLog.exit("No runtime arguments. Try: %s", ARGS_USAGE);
		}
		if (args[0].equals("server"))
		{
			side = Side.SERVER;
			port = Integer.parseInt(args[1]);
			BFServer.instantiate(port);
		}
		else if (args[0].equals("client"))
		{
			side = Side.CLIENT;
			port = Integer.parseInt(args[1]);
			try
			{
				BFClient.instantiate(InetAddress.getByName(args[2]), port);
			}
			catch (UnknownHostException t)
			{
				BFLog.t(t);
				BFLog.exit("Invalid server address.");
			}
			catch (IndexOutOfBoundsException t)
			{
				BFLog.t(t);
				BFLog.exit("Invalid runtime arguments. Server address is not specified. Try: %s", ARGS_USAGE);
			}
		}
		else
		{
			BFLog.exit("Invalid runtime arguments. Side is neither client nor server. Try: %s", ARGS_USAGE);
		}
	}
	
	public static Side getSide()
	{
		return side;
	}
	
	public static int getPort()
	{
		return port;
	}
}