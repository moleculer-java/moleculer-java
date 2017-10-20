package services.moleculer.strategies;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;

import static services.moleculer.utils.CommonUtils.getProperty;

/**
 * Abstract class for Round-Robin and Random invocation strategy factories.
 * 
 * @see RoundRobinStrategyFactory
 * @see NanoSecRandomStrategyFactory
 * @see SecureRandomStrategyFactory
 * @see XORShiftRandomStrategyFactory
 */
public abstract class ArrayBasedStrategyFactory extends StrategyFactory {

	// --- PROPERTIES ---
	
	/**
	 * Invoke local actions if possible
	 */
	protected boolean preferLocal;
	
	// --- CONSTRUCTORS ---
		
	public ArrayBasedStrategyFactory() {
		this(true);
	}
	
	public ArrayBasedStrategyFactory(boolean preferLocal) {
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
		preferLocal = getProperty(config, "preferLocal", preferLocal).asBoolean();
	}

	// --- GETTERS / SETTERS ---
	
	public final boolean isPreferLocal() {
		return preferLocal;
	}

	public final void setPreferLocal(boolean preferLocal) {
		this.preferLocal = preferLocal;
	}
	
}
