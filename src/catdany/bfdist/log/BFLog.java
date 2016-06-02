package catdany.bfdist.log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import catdany.bfdist.BFHelper;

public class BFLog
{
	private static Level logLevel;
	private static File logFolder = new File("logs");
	private static File logFile;
	private static FileWriter logFileWriter;
	
	/**
	 * Initialize a logger by setting the logging level
	 * @param logLevel
	 */
	public static void init(Level logLevel)
	{
		BFLog.logLevel = logLevel;
		log(Level.ALL, "Logger initialized. Level threshold is %s.", logLevel);
		if (!logFolder.exists() || !logFolder.isDirectory())
		{
			logFolder.mkdir();
		}
		logFile = new File(logFolder, BFHelper.dateFormatFile.format(Calendar.getInstance().getTime()) + ".txt");
		try
		{
			logFileWriter = new FileWriter(logFile);
		}
		catch (IOException t)
		{
			log(Level.ALL, "Couldn't create a log file: %s", logFile.getAbsolutePath());
			t(t);
		}
	}
	
	/**
	 * Get current logging level
	 * @return
	 */
	public static Level getLoggingLevel()
	{
		return logLevel;
	}
	
	/**
	 * Info logging
	 * @param format
	 * @param args
	 */
	public static void i(String format, Object... args)
	{
		log(Level.INFO, format, args);
	}
	
	/**
	 * Error logging
	 * @param format
	 * @param args
	 */
	public static void e(String format, Object... args)
	{
		log(Level.ERROR, format, args);
	}
	
	/**
	 * Debug logging
	 * @param format
	 * @param args
	 */
	public static void d(String format, Object... args)
	{
		log(Level.DEBUG, format, args);
	}
	
	/**
	 * Warning logging
	 * @param format
	 * @param args
	 */
	public static void w(String format, Object... args)
	{
		log(Level.WARN, format, args);
	}
	
	/**
	 * Log stack trace
	 * @param t
	 * @param format
	 * @param args
	 */
	public static void t(Exception t)
	{
		log(Level.ERROR, t.getClass().getCanonicalName() + ": " + t.getMessage());
		for (StackTraceElement i : t.getStackTrace())
		{
			log(Level.ERROR, "  at " + i.toString());
		}
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
		if (logLevel == null)
		{
			System.out.println("Logger has not been initialized.");
		}
		else	
		{
			t(format, args);
		}
		int code = format.hashCode();
		System.err.println(String.format("[Log-EXIT] Exit code: %s", code));
		System.exit(code);
	}
	
	/**
	 * Logging
	 * @param level Logging level
	 * @param msg Message to log
	 * @see String#format(String, Object...)
	 */
	public static void log(Level level, String msg)
	{
		if (logLevel == null)
		{
			System.out.println("Logger has not been initialized.");
		}
		else if (level.value <= logLevel.value)
		{
			String s = String.format("[Log] [%s] [%s] [%s] %s",
					BFHelper.dateFormatLog.format(new Date()),
					Thread.currentThread().getName(),
					level,
					msg);
			System.out.println(s);
			logToFile(s);
		}
	}
	
	/**
	 * Logging
	 * @param level Logging level
	 * @param format Message format
	 * @param args {@link String#format(String, Object...)} arguments
	 */
	public static void log(Level level, String format, Object... args)
	{
		log(level, String.format(format, args));
	}
	
	/**
	 * Used internally to write a line to log file.<br>
	 * Use {@link BFLog#log(Level, String, Object...)} instead if you want to log properly.
	 */
	public static void logToFile(String s)
	{
		if (logFileWriter != null)
		{
			try
			{
				logFileWriter.write(s + "\n");
				logFileWriter.flush();
			}
			catch (IOException t)
			{
				logFileWriter = null;
				log(Level.ALL, "Couldn't write a line to log file: %s | %s", s, logFile.getAbsolutePath());
				t(t);
			}
		}
	}
	
	public enum Level
	{
		ERROR(0),
		WARN(1),
		INFO(2),
		DEBUG(3),
		ALL(-1);
		
		public final int value;
		
		private Level(int value)
		{
			this.value = value;
		}
	}
}
