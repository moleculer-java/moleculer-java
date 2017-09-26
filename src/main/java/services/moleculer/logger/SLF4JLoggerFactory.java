package services.moleculer.logger;

import java.util.HashMap;

public final class SLF4JLoggerFactory extends LoggerFactory {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	@Override
	public final String name() {
		return "SLF4J Logger Factory";
	}
	
	// --- FACTORY METHOD ---
	
	private final HashMap<String, SLF4JLogger> loggers = new HashMap<>();
	
	@Override
	public final Logger getLogger(String name) {
		SLF4JLogger logger;
		synchronized (loggers) {
			logger = loggers.get(name);
			if (logger == null) {
				logger = new SLF4JLogger(org.slf4j.LoggerFactory.getLogger(name));
				loggers.put(name, logger);
			}
		}
		return logger;
	}

	// --- STOP LOGGER FACILITY ---

	/**
	 * Closes logger.
	 */
	@Override
	public final void close() {
		loggers.clear();
	}
	
}
