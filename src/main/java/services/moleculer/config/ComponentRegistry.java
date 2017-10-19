package services.moleculer.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.cachers.Cacher;
import services.moleculer.context.ContextFactory;
import services.moleculer.eventbus.EventBus;
import services.moleculer.services.ServiceRegistry;
import services.moleculer.strategies.InvocationStrategyFactory;
import services.moleculer.transporters.Transporter;
import services.moleculer.uids.UIDGenerator;

/**
 * ComponentRegistry is a registry for loaded Moleculer Components. A Moleculer
 * Component can be a required internal component (eg. EventBus), or a custom
 * user-defined object (eg. DAO object to access a database).
 *
 * @see StandaloneComponentRegistry
 * @see SpringComponentRegistry
 * @see GuiceComponentRegistry
 */
public abstract class ComponentRegistry {

	// --- BASE COMPONENT IDS ---

	public static final String COMPONENT_REGISTRY_ID = "componentRegistry";
	public static final String CONTEXT_FACTORY_ID = "contextFactory";
	public static final String UID_GENERATOR_ID = "uidGenerator";
	public static final String INVOCATION_STRATEGY_FACTORY_ID = "invocationStrategyFactory";
	public static final String EVENT_BUS_ID = "eventBus";
	public static final String CACHER_ID = "cacher";
	public static final String SERVICE_REGISTRY_ID = "serviceRegistry";
	public static final String TRANSPORTER_ID = "transporter";

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- START REGISTRY AND COMPONENTS ---

	/**
	 * Initializes registry instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param brokerConfig
	 *            configuration of the current component (created by the
	 *            ServiceBrokerBuilder)
	 * @param customConfig
	 *            optional configuration (loaded from file)
	 */
	public void start(ServiceBroker broker, ServiceBrokerConfig brokerConfig, Tree customConfig) throws Exception {
	}

	// --- STOP REGISTRY AND COMPONENTS ---

	/**
	 * Closes registry.
	 */
	public void stop() {
	}

	// --- GET THREAD POOLS ---

	public abstract ExecutorService executor();

	public abstract ScheduledExecutorService scheduler();

	// --- GET BASE COMPONENTS ---

	public abstract ContextFactory contextFactory();

	public abstract UIDGenerator uidGenerator();

	public abstract InvocationStrategyFactory invocationStrategyFactory();

	public abstract ServiceRegistry serviceRegistry();

	public abstract Cacher cacher();

	public abstract EventBus eventBus();

	public abstract Transporter transporter();
	
	// --- GET IDS OF ALL COMPONENTS ---

	public abstract String[] getComponentNames();

	// --- GET COMPONENT BY ID ---

	public abstract MoleculerComponent getComponent(String id);

	// --- CHECK COMPONENT ID ---

	public abstract boolean hasComponent(String id);

}