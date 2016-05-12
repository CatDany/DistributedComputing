package catdany.bfdist;

import java.text.DateFormat;
import java.text.SimpleDateFormat;


public class BFHelper
{
	/**
	 * Date format used
	 */
	public static final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
	
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
}