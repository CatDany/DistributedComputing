package catdany.bfdist.client;


public class PingTimer implements Runnable
{
	public final Thread threadPing;
	private long pingTime;
	private ServerCom com;
	
	private long lastPingTime = System.currentTimeMillis();
	
	public PingTimer(long pingTime, ServerCom com)
	{
		this.pingTime = pingTime;
		this.com = com;
		this.threadPing = new Thread(this, "PingTimer");
		threadPing.start();
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			long now = System.currentTimeMillis();
			if (now > lastPingTime + pingTime)
			{
				com.sendToServer("");
				lastPingTime = now;
			}
		}
	}
	
	public void setPingTime(long pingTime)
	{
		this.pingTime = pingTime;
	}
}