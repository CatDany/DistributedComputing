package catdany.bfdist.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ServerPingCheck implements Runnable
{ 
	public final ClientHandler client;
	public final long timeout;
	public final ScheduledExecutorService executor;
	
	public ScheduledFuture<?> future;
	
	public ServerPingCheck(ClientHandler client, long timeout)
	{
		this.client = client;
		this.timeout = timeout;
		this.executor = Executors.newScheduledThreadPool(1);
	}
	
	/**
	 * Schedule ping check
	 * @param client
	 * @param initialDelay Delay before the first check in {@link TimeUnit#MILLISECONDS}
	 * @param timeout After this amount of {@link TimeUnit#MILLISECONDS} client will be dropped
	 * @return
	 */
	public static ServerPingCheck schedule(ClientHandler client, long initialDelay, long timeout)
	{
		ServerPingCheck check = new ServerPingCheck(client, timeout);
		check.future = check.executor.scheduleAtFixedRate(check, initialDelay, timeout, TimeUnit.MILLISECONDS);
		return check;
	}
	
	@Override
	public void run()
	{
		long now = System.currentTimeMillis();
		if (now > client.lastUpdateTime + timeout)
		{
			client.dropped = true;
			future.cancel(false);
		}
	}
}