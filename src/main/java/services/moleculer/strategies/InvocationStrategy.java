package services.moleculer.strategies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.services.Action;
import services.moleculer.services.ActionContainer;

public abstract class InvocationStrategy implements MoleculerComponent {

	// --- NAME OF THE MOLECULER COMPONENT ---

	public String name() {
		return "Invocation Strategy";
	}

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- CONSTUCTOR ---

	public InvocationStrategy() {
	}

	// --- START INVOCATION STRATEGY ---

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
	}

	// --- STOP INVOCATION STRATEGY ---

	/**
	 * Closes instance.
	 */
	@Override
	public void stop() {
	}

	// --- ADD ACCTION ---

	public abstract void add(Action action, Tree parameters);

	// --- REMOVE ACTION ---

	public abstract void remove(Action action);

	// --- HAS ACTIONS ---

	public abstract boolean isEmpty();

	// --- GET ACTION AT REMOTE NODE ---

	public abstract ActionContainer get(String nodeID);

	// --- CALL LOCAL OR REMOTE INSTANCE ---

	public abstract ActionContainer get(boolean preferLocal);

}