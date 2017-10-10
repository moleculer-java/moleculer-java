package services.moleculer.uids;

import services.moleculer.ServiceBroker;
import services.moleculer.services.Name;
import services.moleculer.utils.MoleculerComponent;

@Name("UID Generator")
public abstract class UIDGenerator implements MoleculerComponent {

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