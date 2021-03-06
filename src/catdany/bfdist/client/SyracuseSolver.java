package catdany.bfdist.client;

import java.math.BigInteger;

import catdany.bfdist.log.BFLog;

public class SyracuseSolver implements Runnable
{
	private static final BigInteger bigZero = new BigInteger("0");
	private static final BigInteger bigOne = new BigInteger("1");
	private static final BigInteger bigTwo = new BigInteger("2");
	private static final BigInteger bigThree = new BigInteger("3");
	
	public final ServerCom com;
	
	private BigInteger initial;
	private BigInteger max;
	private BigInteger current;
	
	/**
	 * Start time for {@link #initial} that's currently being processed
	 */
	private long startTime = System.currentTimeMillis();
	/**
	 * Last time reported (automatic reports every {@link #autoReportTimer} hours if the calculation is taking too long)
	 */
	private long lastReportedTime = System.currentTimeMillis();
	/**
	 * Steps this current number took to process
	 */
	private int steps = 0;
	
	/**
	 * Automatically report that one number is taking too long to process every <code>N</code> ms
	 */
	public final long autoReportTimer;
	/**
	 * If one number takes <code>N</code> steps to calculate, report and proceed to the next one 
	 */
	public final int maxSteps;
	
	public SyracuseSolver(long autoReportTimer, int maxSteps, BigInteger initial, BigInteger max, ServerCom com)
	{
		this.autoReportTimer = autoReportTimer;
		this.maxSteps = maxSteps;
		this.initial = current = initial;
		this.max = max;
		this.com = com;
	}
	
	@Override
	public void run()
	{
		while (initial.compareTo(max) == -1) // initial < max
		{
			startTime = lastReportedTime = System.currentTimeMillis();
			steps = 0;
			recursive();
			reportDone();
			initial = current = initial.add(bigOne);
		}
		sendToServer("SPCOMPLETE");
	}
	
	/**
	 * Report to server that a calculation on a particular number is complete and the end result has reached <code>1</code>
	 */
	private void reportDone()
	{
		long now = System.currentTimeMillis();
		sendToServer("SPDONE " + (now - startTime) + " " + initial);
	}
	
	/**
	 * Report to server time elapsed for the current number in process (happens automatically every {@link #autoReportTimer} ms
	 */
	private void reportTime(long now)
	{
		BFLog.d("last: %s | autoreporttimer: %s | now: %s", lastReportedTime, autoReportTimer, now);
		sendToServer("SPTIME " + (now - startTime) + " " + initial);
	}
	
	/**
	 * Report to server that one number took {@link #maxSteps} steps to calculate
	 */
	private void reportMaxStepsReached()
	{
		sendToServer("SPMSR " + initial);
	}
	
	/**
	 * Send message to the server
	 * @param s
	 */
	private void sendToServer(String s)
	{
		com.sendToServer(s);
	}
	
	/**
	 * Perform required calculations on a given number
	 */
	private void recursive()
	{
		if (!current.equals(bigOne))
		{
			long now = System.currentTimeMillis();
			if (now > lastReportedTime + autoReportTimer)
			{
				reportTime(now);
				lastReportedTime = now;
			}
			if (steps > maxSteps)
			{
				reportMaxStepsReached();
				return;
			}
			if (current.mod(bigTwo).equals(bigZero))
			{
				current = current.divide(bigTwo); // 0.5n
			}
			else
			{
				current = current.multiply(bigThree).add(bigOne); // 3n+1
			}
			steps++;
			recursive();
		}
	}
}