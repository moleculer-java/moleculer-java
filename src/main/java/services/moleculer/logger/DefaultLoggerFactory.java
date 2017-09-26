package services.moleculer.logger;

public final class DefaultLoggerFactory extends LoggerFactory {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	@Override
	public final String name() {
		if (factory == null) {
			return "Default Logger Factory";
		}
		return factory.name();
	}
	
	// --- VARIABLES ---

	private final LoggerFactory factory;

	// --- STATIC CONSTRUCTOR ---

	public DefaultLoggerFactory() {
		LoggerFactory implementation = null;
		try {

			// 1.) Check "moleculer.logger" system property
			// 2.) Try to load SLF4J API
			// 3.) Try to load JDK Logger API
			// 4.) Use SystemOutLoggerFactory
			String className = System.getProperty("moleculer.logger", "services.moleculer.logger.SLF4JLoggerFactory");
			implementation = tryToLoad(className);
			if (implementation == null) {
				implementation = tryToLoad("services.moleculer.logger.JDKLoggerFactory");
			}
		} finally {
			if (implementation == null) {
				implementation = new SystemOutLoggerFactory();
			}
		}
		factory = implementation;
	}

	private static final LoggerFactory tryToLoad(String factoryClass) {
		try {
			return (LoggerFactory) Class.forName(factoryClass).newInstance();
		} catch (Throwable ignored) {
		}
		return null;
	}

	// --- FACTORY METHOD ---

	@Override
	public final Logger getLogger(String name) {
		return factory.getLogger(name);
	}

	// --- STOP LOGGER FACILITY ---

	/**
	 * Closes logger.
	 */
	@Override
	public final void close() {
		factory.close();
	}
	
}