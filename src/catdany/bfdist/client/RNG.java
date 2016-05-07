package catdany.bfdist.client;

import java.security.SecureRandom;

public class RNG implements Runnable
{
	private final Thread thread;
	private long startTime = 0;
	private byte[] bytes;
	
	public RNG()
	{
		thread = new Thread(this, "RNG");
	}
	
	/**
	 * Start generating random bytes
	 * @param amount quantity
	 */
	public void start(int amount)
	{
		bytes = new byte[amount];
		thread.start();
	}
	
	@Override
	public void run()
	{
		SecureRandom rand = new SecureRandom();
		rand.nextBytes(bytes);
		BFClient.getClient().getServerCom().sendToServer("RNGDATA_START " + bytes.length);
		BFClient.getClient().getServerCom().sendToServer(bytes);
	}
}