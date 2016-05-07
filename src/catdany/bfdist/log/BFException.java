package catdany.bfdist.log;

public class BFException extends RuntimeException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2660389720755375616L;

	public BFException(String format, Object... args)
	{
		super(String.format(format, args));
	}
}
