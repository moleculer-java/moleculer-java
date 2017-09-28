package services.moleculer.context;

import org.slf4j.Logger;

import services.moleculer.ServiceBroker;
import services.moleculer.logger.AsyncLoggerFactory;
import services.moleculer.utils.MoleculerComponent;

public abstract class ContextPool implements MoleculerComponent {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	@Override
	public String name() {
		return "Context Pool";
	}

	// --- LOGGER ---

	protected final Logger logger;

	// --- CONSTUCTOR ---

	public ContextPool() {
		logger = AsyncLoggerFactory.getLogger(name());
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
	
	// --- GET CONTEXT FROM POOL ---
	
	// TODO add parameters to "borrow"
	
	public abstract Context borrow();

	// --- PUSHBACK CONTEXT INTO THE POOL ---
	
	public abstract void release(Context ctx);
	
}