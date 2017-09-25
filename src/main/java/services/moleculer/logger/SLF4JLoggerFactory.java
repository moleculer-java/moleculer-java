package services.moleculer.logger;

import java.util.HashMap;

public final class SLF4JLoggerFactory implements LoggerFactory {

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

}
