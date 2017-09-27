package services.moleculer.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class SystemOutLogger implements Logger {

	// --- VARIABLES ---

	private final String name;
	private final byte level;

	// --- CONSTRUCTOR ---

	SystemOutLogger(String name, byte level) {
		this.name = name;
		this.level = level;
	}

	// --- IMPLEMENTED LOGGING METHODS ---

	@Override
	public final void trace(Object msg) {
		log("TRACE", msg, null);
	}

	@Override
	public final boolean isTraceEnabled() {
		return level <= SystemOutLoggerFactory.LEVEL_TRACE;
	}

	@Override
	public final void debug(Object msg) {
		log("TRACE", msg, null);
	}

	@Override
	public final boolean isDebugEnabled() {
		return level <= SystemOutLoggerFactory.LEVEL_DEBUG;
	}

	@Override
	public final void info(Object msg) {
		log("INFO ", msg, null);
	}

	@Override
	public final boolean isInfoEnabled() {
		return level <= SystemOutLoggerFactory.LEVEL_INFO;
	}
	
	@Override
	public final void warn(Object msg) {
		log("WARN ", msg, null);
	}

	@Override
	public final void warn(Object msg, Throwable cause) {
		log("WARN ", msg, cause);
	}

	@Override
	public final boolean isWarnEnabled() {
		return level <= SystemOutLoggerFactory.LEVEL_WARN;
	}

	@Override
	public final void error(Object msg) {
		log("ERROR", msg, null);
	}

	@Override
	public final void error(Object msg, Throwable cause) {
		log("ERROR", msg, cause);
	}

	@Override
	public final boolean isErrorEnabled() {
		return level <= SystemOutLoggerFactory.LEVEL_ERROR;
	}

	@Override
	public final void fatal(Object msg) {
		log("FATAL", msg, null);
	}

	@Override
	public final void fatal(Object msg, Throwable cause) {
		log("FATAL", msg, cause);
	}

	@Override
	public final boolean isFatalEnabled() {
		return level <= SystemOutLoggerFactory.LEVEL_FATAL;
	}

	// --- COMMON LOGGER METHOD ---

	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
	private static final String SEPARATOR = System.getProperty("line.separator", "\r\n");
	private static final char[] SPACER = " - ".toCharArray();
	private static final char[] COLON = ": ".toCharArray();
	
	private final void log(String level, Object msg, Throwable cause) {
		StringBuilder line = new StringBuilder(128);
		synchronized (FORMAT) {
			line.append(FORMAT.format(new Date()));
		}
		line.append(SPACER);
		line.append(level);
		line.append(SPACER);
		line.append(name);
		line.append(COLON);
		line.append(msg);
		if (cause != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			cause.printStackTrace(pw);
			line.append(SEPARATOR);
			line.append(sw.toString());
		}
		System.out.println(line);
	}

}