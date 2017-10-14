package services.moleculer.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.services.Name;

@Name("Event Bus")
public abstract class EventBus implements MoleculerComponent {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- CONSTUCTOR ---

	public EventBus() {
	}

	// --- INIT EVENT BUS ---

	/**
	 * Initializes internal EventBus instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {
	}

	// --- STOP EVENT BUS ---

	@Override
	public void stop() {
	}

	// --- REGISTER LISTENER ----

	public abstract void on(String name, Listener listener, boolean once);

	// --- UNREGISTER LISTENER ---

	/**
	 * Unsubscribe from an event
	 * 
	 * @param name
	 * @param listener
	 */
	public abstract void off(String name, Listener listener);

	// --- EMIT EVENT TO LISTENERS ---

	public abstract void emit(String name, Object payload);

}