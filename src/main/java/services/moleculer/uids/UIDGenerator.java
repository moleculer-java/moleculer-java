package services.moleculer.uids;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import services.moleculer.ServiceBroker;
import services.moleculer.utils.MoleculerComponent;

public abstract class UIDGenerator implements MoleculerComponent {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	@Override
	public String name() {
		return "UID Generator";
	}
	
	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- CONSTUCTOR ---

	public UIDGenerator() {
	}
	
	// --- START GENERATOR ---

	/**
	 * Initializes UID generator instance.
	 * 
	 * @param broker
	 */
	@Override
	public void init(ServiceBroker broker) throws Exception {
	}

	// --- STOP GENERATOR ---

	/**
	 * Closes UID generator.
	 */
	@Override
	public void close() {
	}
	
	// --- GENERATE UID ---
	
	public abstract String nextUID();
	
}