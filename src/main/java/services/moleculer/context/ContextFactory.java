package services.moleculer.context;

import services.moleculer.ServiceBroker;
import services.moleculer.utils.MoleculerComponent;

public abstract class ContextFactory implements MoleculerComponent {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	@Override
	public String name() {
		return "Context Factory";
	}
	
	// --- START CONTEXT FACTORY ---

	/**
	 * Initializes Context Factory instance.
	 * 
	 * @param broker
	 */
	@Override
	public void init(ServiceBroker broker) throws Exception {
	}

	// --- STOP CONTEXT FACTORY ---

	/**
	 * Closes Context Factory.
	 */
	@Override
	public void close() {
	}
	
	// --- GET / CREATE CONTEXT ---
	
	public abstract Context create();
	
}