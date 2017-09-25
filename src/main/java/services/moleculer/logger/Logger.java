package services.moleculer.logger;

public interface Logger {

	public void trace(Object msg);

	public boolean isTraceEnabled();
	
	public void debug(Object msg);

	public boolean isDebugEnabled();
	
	public void info(Object msg);

	public void warn(Object msg);

	public void warn(Object msg, Throwable cause);

	public boolean isWarnEnabled();

	public void error(Object msg);

	public void error(Object msg, Throwable cause);

	public boolean isErrorEnabled();

	public void fatal(Object msg);

	public void fatal(Object msg, Throwable cause);

	public boolean isFatalEnabled();

}