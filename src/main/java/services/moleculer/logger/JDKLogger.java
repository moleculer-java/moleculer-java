package services.moleculer.logger;

import java.util.logging.Level;

public final class JDKLogger implements Logger {

	// --- VARIABLES ---
	
	private final java.util.logging.Logger logger;
	
	// --- CONSTRUCTOR ---
	
	JDKLogger(java.util.logging.Logger logger) {
		this.logger = logger;
	}
	
	// --- IMPLEMENTED LOGGING METHODS ---
	
	@Override
	public final void trace(Object msg) {
		log(Level.FINEST, msg, null);
	}

	@Override
	public final boolean isTraceEnabled() {
		return logger.isLoggable(Level.FINEST);
	}

	@Override
	public final void debug(Object msg) {
		log(Level.FINE, msg, null);
	}

	@Override
	public final boolean isDebugEnabled() {
		return logger.isLoggable(Level.FINE);
	}

	@Override
	public final void info(Object msg) {
		log(Level.INFO, msg, null);
	}

	@Override
	public final void warn(Object msg) {
		log(Level.WARNING, msg, null);
	}

	@Override
	public final void warn(Object msg, Throwable cause) {
		log(Level.WARNING, msg, cause);
	}

	@Override
	public final boolean isWarnEnabled() {
		return logger.isLoggable(Level.WARNING);
	}

	@Override
	public final void error(Object msg) {
		log(Level.SEVERE, msg, null);
	}

	@Override
	public final void error(Object msg, Throwable cause) {
		log(Level.SEVERE, msg, cause);		
	}

	@Override
	public final boolean isErrorEnabled() {
		return logger.isLoggable(Level.SEVERE);
	}

	@Override
	public final void fatal(Object msg) {
		log(Level.SEVERE, msg, null);
	}

	@Override
	public final void fatal(Object msg, Throwable cause) {
		log(Level.SEVERE, msg, cause);
	}

	@Override
	public final boolean isFatalEnabled() {
		return logger.isLoggable(Level.SEVERE);
	}
	
	// --- COMMON LOGGER METHOD ---
	
	private final void log(Level level, Object msg, Throwable cause) {
		logger.log(level, String.valueOf(msg), cause);
	}
	
}