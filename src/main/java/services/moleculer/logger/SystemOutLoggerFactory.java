package services.moleculer.logger;

import java.util.HashMap;

public final class SystemOutLoggerFactory implements LoggerFactory {

	// --- LEVELS ---
	
	public static final byte LEVEL_ALL = 0;
	public static final byte LEVEL_TRACE = 1;
	public static final byte LEVEL_DEBUG = 2;
	public static final byte LEVEL_INFO = 3;
	public static final byte LEVEL_WARN = 4;
	public static final byte LEVEL_ERROR = 5;
	public static final byte LEVEL_FATAL = 6;
	public static final byte LEVEL_OFF = 7;

	// --- PROPERTIES ---
	
	private final byte level;
	
	// --- CONSTRUCTORS ---
	
	public SystemOutLoggerFactory() {
		this(LEVEL_INFO);
	}
	
	public SystemOutLoggerFactory(byte level) {
		this.level = level;
	}

	// --- FACTORY METHOD ---
	
	private final HashMap<String, SystemOutLogger> loggers = new HashMap<>();
	
	@Override
	public final Logger getLogger(String name) {
		SystemOutLogger logger;
		synchronized (loggers) {
			logger = loggers.get(name);
			if (logger == null) {
				logger = new SystemOutLogger(name, level);
				loggers.put(name, logger);
			}
		}
		return logger;
	}

}
