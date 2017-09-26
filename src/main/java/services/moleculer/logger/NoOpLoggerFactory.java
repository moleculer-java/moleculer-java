package services.moleculer.logger;

public final class NoOpLoggerFactory extends LoggerFactory {

	// --- COMMON INSTANCE ---
	
	private static final NoOpLogger instance = new NoOpLogger();
	
	// --- GET LOGGER INSTANCE ---
	
	@Override
	public final Logger getLogger(String name) {
		return instance;
	}

	public static final Logger getInstance() {
		return instance;
	}
	
}
