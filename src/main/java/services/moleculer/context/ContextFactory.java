package services.moleculer.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.services.Name;

@Name("Context Factory")
public abstract class ContextFactory implements MoleculerComponent {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- CONSTUCTOR ---

	public ContextFactory() {
	}

	// --- START CONTEXT FACTORY ---

	/**
	 * Initializes Context Factory instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {
	}

	// --- STOP CONTEXT FACTORY ---

	/**
	 * Closes Context Factory.
	 */
	@Override
	public void stop() {
	}

	// --- CREATE CONTEXT ---

	public abstract Context create(Tree params, CallingOptions opts);

}