package catdany.bfdist.server.report;

public class ReporterException extends RuntimeException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4513556671131566758L;

	public ReporterException(String format, Object... args)
	{
		super(String.format(format, args));
	}
	
	public ReporterException(Throwable t, String format, Object... args)
	{
		super(String.format(format, args), t);
	}
}