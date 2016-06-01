package catdany.bfdist;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class BFHelper
{
	/**
	 * dd.MM.yyyy HH:mm
	 */
	public static final DateFormat dateFormatVersion = new SimpleDateFormat("dd.MM.yyyy HH:mm");
	/**
	 * yyyy-MM-dd HH:mm:ss
	 */
	public static final DateFormat dateFormatLog = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	/**
	 * yyyy-MM-dd-HH-mm-ss
	 */
	public static final DateFormat dateFormatFile = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	/**
	 * HH 'h' mm 'm' ss 's' S 'ms'
	 */
	public static final DateFormat dateFormatTime = new SimpleDateFormat("HH 'h' mm 'm' ss 's' S 'ms'");
	
	public static final Charset charset = Charset.forName("ISO-8859-15");
	
	/**
	 * Tries to {@link Integer#parseInt(String)parseInt} from given string.<br> If it throws {@link NumberFormatException}, catch it and return false.
	 * @param s
	 * @return true if string is parsable as integer (no {@link NumberFormatException} thrown)
	 */
	public static boolean isInteger(String s)
	{
		try
		{
			Integer.parseInt(s);
			return true;
		}
		catch (NumberFormatException t)
		{
			return false;
		}
	}
	
	/**
	 * Does <code>array</code> contain <code>obj</code>
	 * @param array
	 * @param obj
	 * @param compareObj
	 * <code>true</code> if you want to compare objects with "==" (same object)<br>
	 * <code>false</code> if you want to compare objects with {@link Object#equals(Object)}
	 * @return
	 */
	public static <T>boolean arrayContains(T[] array, T obj, boolean compareObj)
	{
		for (T i : array)
		{
			if (compareObj)
			{
				if (i == obj)
				{
					return true;
				}
			}
			else
			{
				if (i.equals(obj))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Write exception to string<br>
	 * Resulting contains full class name of the exception, its message and stack trace. 
	 * @param t
	 * @return
	 */
	public static String writeException(Exception t)
	{
		StringWriter s = new StringWriter();
		PrintWriter w = new PrintWriter(s);
		t.printStackTrace(w);
		return s.toString();
	}
}