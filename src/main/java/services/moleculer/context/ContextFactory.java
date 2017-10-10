package services.moleculer.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.services.Name;
import services.moleculer.utils.MoleculerComponent;

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

	// --- CREATE CONTEXT ---

	public abstract Context create(Tree params, CallingOptions opts);

}