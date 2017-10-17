package services.moleculer.strategies;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;

public abstract class ArrayBasedInvocationStrategyFactory extends InvocationStrategyFactory {

	// --- PROPERTIES ---
	
	protected boolean preferLocal;
	
	// --- CONSTRUCTORS ---
		
	public ArrayBasedInvocationStrategyFactory() {
		this(true);
	}
	
	public ArrayBasedInvocationStrategyFactory(boolean preferLocal) {
		this.preferLocal = preferLocal;
	}
	
	// --- START INVOCATION FACTORY ---

	/**
	 * Initializes strategy instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {
		
		// Process config
		preferLocal = config.get("preferLocal", preferLocal);
	}

	// --- GETTERS / SETTERS ---
	
	public final boolean isPreferLocal() {
		return preferLocal;
	}

	public final void setPreferLocal(boolean preferLocal) {
		this.preferLocal = preferLocal;
	}
	
}
