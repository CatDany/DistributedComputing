package catdany.bfdist.client;

import java.math.BigInteger;
import java.util.HashSet;

public class CustomSolver implements Runnable
{
	private static final BigInteger bigZero = new BigInteger("0");
	private static final BigInteger bigOne = new BigInteger("1");
	private static final BigInteger bigTwo = new BigInteger("2");
	
	public final Sender com;
	public final BigInteger coefFirst;
	public final BigInteger coefSecond;
	public int maxSteps;
	
	private HashSet<BigInteger> pastCycles = new HashSet<BigInteger>();
	
	private BigInteger initial;
	private BigInteger max;
	private BigInteger current;
	private BigInteger calcEnd;
	
	private int steps;
	
	/**
	 * Starts a solver for recursive function defined as follows:<br>
	 * <code>
	 * f(x) = {<br>
	 * x equals 1 => 1<br>
	 * x mod 2 equals 0 => f(x/2)<br>
	 * x mod 2 equals 1 => f(ax+b)<br>
	 * }
	 * </code><br>
	 * <code>a</code> and <code>b</code> are coefficients specified in the arguments
	 * @param maxSteps Maximal recursive steps one number will take before the thorough recalculation will be started  
	 * @param initial Initial number of the interval (calculate from, inclusively)
	 * @param max End number of the interval (calculate to, exclusively)
	 * @param coefFirst First coefficient, <code>a</code>
	 * @param coefSecond Second coefficient, <code>b</code>
	 * @param com {@link ServerCom} object
	 */
	public CustomSolver(int maxSteps, BigInteger initial, BigInteger max, int coefFirst, int coefSecond, Sender com)
	{
		this.com = com;
		this.maxSteps = maxSteps;
		this.initial = initial;
		this.max = max;
		this.coefFirst = BigInteger.valueOf(coefFirst);
		this.coefSecond = BigInteger.valueOf(coefSecond);
	}
	
	@Override
	public void run()
	{
		while (initial.compareTo(max) == -1) // initial < max
		{
			calculate();
			initial = current = initial.add(bigOne);
		}
		sendToServer("CSPCOMPLETE");
	}
	
	private void reportDone()
	{
		sendToServer("CSPDONE " + initial);
	}
	
	private void reportMaxStepsReached()
	{
		sendToServer("CSPMSR " + initial);
	}
	
	private void reportCycle(BigInteger cycle)
	{
		sendToServer("CSPCYCLE " + initial + " " + cycle);
	}
	
	/**
	 * Send message to server.<br>
	 * This method is used within this class, instead of {@link ServerCom#sendToServer(String) directly sending a message} 
	 * @param s
	 */
	private void sendToServer(String s)
	{
		com.sendToServer(s);
	}
	
	/**
	 * Calculate {@link #initial} with the recursive algorithm.
	 * @return <code>true</code> if any of the following conditions are met:
	 * <ul>
	 * <li>the calculation ended with <code>1</code> before {@link #maxSteps} was reached
	 * <li>{@link #maxSteps} was reached before the calculation ended with <code>1</code> but {@link #recalc() recalculation} returned a positive number
	 * </ul>
	 * <code>false</code> if any of the following conditions are met:
	 * <ul>
	 * <li>{@link #maxSteps} was reached before the calculation ended with <code>1</code> and {@link #recalc() recalculation} returned <code>0</code>
	 * </ul>
	 */
	private void calculate()
	{
		steps = 0;
		current = initial;
		while (!current.equals(bigOne))
		{
			steps++;
			if (steps > maxSteps)
			{
				this.calcEnd = current;
				int recalc = recalc();
				if (recalc > -1)
					reportCycle(initial);
				else
					reportMaxStepsReached();
				return;
			}
			if (pastCycles.contains(current))
			{
				reportCycle(current);
				return;
			}
			if (current.mod(bigTwo).equals(bigZero))
			{
				current = current.divide(bigTwo); // 0.5n
			}
			else
			{
				current = current.multiply(coefFirst).add(coefSecond); // an+b
			}
		}
		reportDone();
	}
	
	/**
	 * Recalculate {@link #initial} with the same recursive algorithm that's used in {@link #calculate()}<br>
	 * If {@link #calcEnd} is found, break.<br> 
	 * @return
	 * <ul>
	 * <li>Amount of steps it took before {@link #calcEnd} was found<br>
	 * <li><code>-1</code> if {@link #calcEnd} was not found after {@link #maxSteps} steps
	 * </ul>
	 */
	private int recalc()
	{
		steps = 0;
		current = initial;
		while (!current.equals(calcEnd))
		{
			steps++;
			if (steps > maxSteps)
			{
				return -1;
			}
			if (current.mod(bigTwo).equals(bigZero))
			{
				current = current.divide(bigTwo); // 0.5n
			}
			else
			{
				current = current.multiply(coefFirst).add(coefSecond); // an+b
			}
		}
		pastCycles.add(initial);
		return steps;
	}
}