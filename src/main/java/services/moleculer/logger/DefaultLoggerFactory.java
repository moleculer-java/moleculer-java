package services.moleculer.logger;

public final class DefaultLoggerFactory implements LoggerFactory {

	// --- VARIABLES ---

	private LoggerFactory factory;

	// --- STATIC CONSTRUCTOR ---

	public DefaultLoggerFactory() {
		try {

			// -Dmoleculer.logger = your.logger.FactoryClass
			String className = System.getProperty("moleculer.logger", "services.moleculer.logger.SLF4JLoggerFactory");
			factory = tryToLoad(className);
			if (factory == null) {
				factory = tryToLoad("services.moleculer.logger.JDKLoggerFactory");
			}
		} finally {
			if (factory == null) {
				factory = new SystemOutLoggerFactory();
			}
		}
	}

	private static final LoggerFactory tryToLoad(String factoryClass) {
		try {
			return (LoggerFactory) Class.forName(factoryClass).newInstance();
		} catch (Throwable ignored) {
		}
		return null;
	}

	// --- STATIC FACTORY METHOD ---

	@Override
	public final Logger getLogger(String name) {
		return factory.getLogger(name);
	}

}