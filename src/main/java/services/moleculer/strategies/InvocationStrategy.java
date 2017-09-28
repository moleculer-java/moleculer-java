package services.moleculer.strategies;

import org.slf4j.Logger;

import services.moleculer.ServiceBroker;
import services.moleculer.logger.AsyncLoggerFactory;
import services.moleculer.services.Action;
import services.moleculer.utils.MoleculerComponent;

public abstract class InvocationStrategy implements MoleculerComponent {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	public String name() {
		return "Invocation Strategy";
	}

	// --- LOGGER ---

	protected final Logger logger;

	// --- CONSTUCTOR ---

	public InvocationStrategy() {
		logger = AsyncLoggerFactory.getLogger(name());
	}

	// --- START INVOCATION STRATEGY ---

	/**
	 * Initializes logger instance.
	 * 
	 * @param broker
	 */
	@Override
	public void init(ServiceBroker broker) throws Exception {
	}

	// --- STOP INVOCATION STRATEGY ---

	/**
	 * Closes logger.
	 */
	@Override
	public void close() {
	}
	
	// --- ADD ACCTION ---
	
	public abstract void add(Action action);

	// --- REMOVE ACTION ---
	
	public abstract void remove(Action action);

	// --- HAS ACTIONS ---
	
	public abstract boolean isEmpty();

	// --- GET ACTION AT REMOTE NODE ---
	
	public abstract Action get(String nodeID);
	
	// --- CALL LOCAL OR REMOTE INSTANCE ---
	
	public abstract Action get(boolean preferLocal);
	
}