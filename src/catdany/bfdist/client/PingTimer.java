package catdany.bfdist.client;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PingTimer implements Runnable
{
	public final ServerCom com;
	public final ScheduledExecutorService executor;
	
	public ScheduledFuture<?> future;
	
	public PingTimer(long pingTime, ServerCom com)
	{
		this.com = com;
		this.executor = Executors.newSingleThreadScheduledExecutor();
		this.future = executor.scheduleAtFixedRate(this, pingTime, pingTime, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public void run()
	{
		if (BFClient.getClient().getServerCom() == com)
		{
			com.sendToServer("");
		}
		else
		{
			future.cancel(false);
		}
	}
}