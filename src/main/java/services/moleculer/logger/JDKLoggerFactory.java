package services.moleculer.logger;

import java.util.HashMap;

public final class JDKLoggerFactory extends LoggerFactory {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	@Override
	public final String name() {
		return "JDK Logger Factory";
	}
	
	// --- FACTORY METHOD ---
	
	private final HashMap<String, JDKLogger> loggers = new HashMap<>();
	
	@Override
	public final Logger getLogger(String name) {
		JDKLogger logger;
		synchronized (loggers) {
			logger = loggers.get(name);
			if (logger == null) {
				logger = new JDKLogger(java.util.logging.Logger.getLogger(name));
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