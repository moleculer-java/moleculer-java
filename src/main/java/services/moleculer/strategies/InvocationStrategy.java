package services.moleculer.strategies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.services.Action;
import services.moleculer.services.ActionContainer;
import services.moleculer.services.Name;

@Name("Invocation Strategy")
public abstract class InvocationStrategy implements MoleculerComponent {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

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

	// --- GET ACTION ---

	public abstract ActionContainer get(String nodeID);

}