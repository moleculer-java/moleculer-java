package services.moleculer.strategies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.services.Name;

/**
 * Base class of Invocation Strategies. An Invocation Strategy is a
 * high-performance endpoint selector (Round-Robin, Random, etc.) Performance of
 * implementations:<br>
 * <br>
 * Duration of 10 000 000 loops (lower value is the better):
 * <ul>
 * <li>RoundRobinStrategyFactory: 190 msec
 * <li>XORShiftRandomStrategyFactory: 210 msec
 * <li>NanoSecRandomStrategyFactory: 240 msec
 * <li>SecureRandomStrategyFactory: 1061 msec
 * </ul>
 *
 * @see RoundRobinStrategyFactory
 * @see NanoSecRandomStrategyFactory
 * @see SecureRandomStrategyFactory
 * @see XORShiftRandomStrategyFactory
 */
@Name("Strategy Factory")
public abstract class StrategyFactory implements MoleculerComponent {

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

	public abstract Strategy create();

}