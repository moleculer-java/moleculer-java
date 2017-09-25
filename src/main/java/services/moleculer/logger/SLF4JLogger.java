package services.moleculer.logger;

public final class SLF4JLogger implements Logger {

	// --- VARIABLES ---
	
	private final org.slf4j.Logger logger;
	
	// --- CONSTRUCTOR ---
	
	SLF4JLogger(org.slf4j.Logger logger) {
		this.logger = logger;
	}
	
	// --- IMPLEMENTED LOGGING METHODS ---
	
	@Override
	public final void trace(Object msg) {
		logger.trace(String.valueOf(msg));
	}

	@Override
	public final boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}

	@Override
	public final void debug(Object msg) {
		logger.debug(String.valueOf(msg));
	}

	@Override
	public final boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	@Override
	public final void info(Object msg) {
		logger.info(String.valueOf(msg));
	}

	@Override
	public final void warn(Object msg) {
		logger.warn(String.valueOf(msg));
	}

	@Override
	public final void warn(Object msg, Throwable cause) {
		logger.warn(String.valueOf(msg), cause);
	}

	@Override
	public final boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	@Override
	public final void error(Object msg) {
		logger.error(String.valueOf(msg));
	}

	@Override
	public final void error(Object msg, Throwable cause) {
		logger.error(String.valueOf(msg), cause);		
	}

	@Override
	public final boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

	@Override
	public final void fatal(Object msg) {
		logger.error(String.valueOf(msg));
	}

	@Override
	public final void fatal(Object msg, Throwable cause) {
		logger.error(String.valueOf(msg), cause);
	}

	@Override
	public final boolean isFatalEnabled() {
		return logger.isErrorEnabled();
	}
	
}