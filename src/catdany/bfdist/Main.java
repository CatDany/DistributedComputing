package catdany.bfdist;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import catdany.bfdist.client.BFClient;
import catdany.bfdist.log.BFLog;
import catdany.bfdist.server.BFServer;

public class Main
{
	/**
	 * Arguments usage (used for error messages)
	 */
	private static final String ARGS_USAGE = "(client|server) (port) [server-ip]";
	/**
	 * Version in convention (major.minor-maintenance/build)
	 */
	public static final String VERSION_NAME = "1.0-a6";
	/**
	 * Datetime in <u>seconds</u> representing when this version was built
	 */
	public static final long VERSION_DATE = 1463233508L;
	
	/**
	 * Side that the program is running on (client/server)<br>
	 * Used for references
	 */
	private static Side side;
	/**
	 * Port that server is hosted on, set through runtime arguments.<br>
	 * If {@link Main#side side} is {@link Side#CLIENT client}, client will try to connect to this port<br>
	 * If {@link Side#SERVER server} the server will try to bind to this port
	 */
	private static int port;
	
	/**
	 * main method
	 * @param args See {@link #ARGS_USAGE}
	 */
	public static void main(String[] args)
	{
		BFLog.init(BFHelper.arrayContains(args, "--enableDebugLogging", false) ? BFLog.Level.DEBUG : BFLog.Level.INFO);
		BFLog.i("You're running DistComp by CatDany. Current version is %s (%s)", VERSION_NAME, getBuildDate());
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
	
	/**
	 * Get side.<br>
	 * Depends on runtime arguments.
	 * @return {@link Side#CLIENT} or {@link Side#SERVER}
	 */
	public static Side getSide()
	{
		return side;
	}
	
	/**
	 * Get port that the server is hosted on.<br>
	 * Depends on runtime arguments.
	 * @return
	 */
	public static int getPort()
	{
		return port;
	}
	
	/**
	 * Get formatted date representing when the current version was built<br>
	 * Format: {@link BFHelper#dateFormatVersion}<br>
	 * Datetime used: {@link Main#VERSION_DATE};
	 * @return
	 */
	public static String getBuildDate()
	{
		return BFHelper.dateFormatVersion.format(new Date(VERSION_DATE * 1000));
	}
}