package services.moleculer.strategies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.services.Name;

@Name("Invocation Strategy Factory")
public abstract class InvocationStrategyFactory implements MoleculerComponent {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- START EVENT BUS ---

	/**
	 * Initializes Invocation Strategy Factory instance.
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
	
	// --- FACTORY METHOD ---

	public abstract InvocationStrategy create();

}