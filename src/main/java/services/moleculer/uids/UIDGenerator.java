package services.moleculer.uids;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.services.Name;

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
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {
	}

	// --- STOP GENERATOR ---

	/**
	 * Closes UID generator.
	 */
	@Override
	public void stop() {
	}

	// --- GENERATE UID ---

	public abstract String nextUID();

}