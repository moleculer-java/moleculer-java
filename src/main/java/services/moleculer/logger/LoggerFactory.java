package services.moleculer.logger;

import services.moleculer.ServiceBroker;
import services.moleculer.utils.MoleculerComponent;

public abstract class LoggerFactory implements MoleculerComponent {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	@Override
	public String name() {
		return "Logger Factory";
	}
	
	// --- START LOGGER FACILITY ---

	/**
	 * Initializes logger instance.
	 * 
	 * @param broker
	 */
	@Override
	public void init(ServiceBroker broker) throws Exception {
	}

	// --- STOP LOGGER FACILITY ---

	/**
	 * Closes logger.
	 */
	@Override
	public void close() {
	}
	
	// --- GET / CREATE LOGGER INSTANCE ---
	
	public abstract Logger getLogger(String name);

	public Logger getLogger(Class<?> clazz) {
		return getLogger(clazz.getName());
	}

}