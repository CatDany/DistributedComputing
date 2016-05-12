package catdany.bfdist.log;

public class BFLog
{
	/*
	 * TODO: Logging system
	 */
	public static final BFLog instance = new BFLog();
	
	/**
	 * Info logging
	 * @param format
	 * @param args
	 */
	public static void i(String format, Object... args)
	{
		log(format, args);
	}
	
	/**
	 * Error logging
	 * @param format
	 * @param args
	 */
	public static void e(String format, Object... args)
	{
		log(format, args);
	}
	
	/**
	 * Debug logging
	 * @param format
	 * @param args
	 */
	public static void d(String format, Object... args)
	{
		log(format, args);
	}
	
	/**
	 * Warning logging
	 * @param format
	 * @param args
	 */
	public static void w(String format, Object... args)
	{
		log(format, args);
	}
	
	/**
	 * Print stack trace
	 * @param t
	 * @param format
	 * @param args
	 */
	public static void t(Exception t)
	{
		t.printStackTrace();
	}
	
	/**
	 * Make a {@link BFException} and {@link #t(Exception) print stacktrace}
	 * @param format
	 * @param args
	 */
	public static void t(String format, Object... args)
	{
		BFException t = new BFException(format, args);
		t(t);
	}
	
	/**
	 * {@link #t(Exception)} -> exit<br>
	 * Exit code is a hash code of the unformatted error message
	 * @param format
	 * @param args
	 */
	public static void exit(String format, Object... args)
	{
		t(format, args);
		int code = format.hashCode();
		System.err.println(String.format("[Log-EXIT] Exit code: %s", code));
		System.exit(code);
	}
	
	/**
	 * NYFI Temporarily just dumps stuff to sysout
	 * @param format
	 * @param args
	 */
	private static void log(String format, Object... args)
	{
		System.out.println("[Log] " + String.format(format, args));
	}
}
