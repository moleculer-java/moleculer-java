package services.moleculer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.cachers.Cacher;
import services.moleculer.config.ComponentRegistry;
import services.moleculer.config.ServiceBrokerBuilder;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.CallingOptions;
import services.moleculer.services.ActionContainer;
import services.moleculer.services.Service;
import services.moleculer.transporters.Transporter;

public final class ServiceBroker {

	// --- VERSION ---

	public static final String VERSION = "1.2";

	// --- LOGGER ---

	protected final static Logger logger = LoggerFactory.getLogger(ServiceBroker.class);

	// --- UNIQUE NODE IDENTIFIER ---

	private final String nodeID;

	// --- CONFIGURATIONS ---

	private final ServiceBrokerConfig brokerConfig;
	private final Tree customConfig;

	// --- INERNAL AND USER-DEFINED COMPONENTS ---

	private final ComponentRegistry components;

	// --- STATIC SERVICE BROKER BUILDER ---

	public static final ServiceBrokerBuilder builder() {
		return new ServiceBrokerBuilder(new ServiceBrokerConfig());
	}

	// --- CONSTRUCTORS ---

	public ServiceBroker() {
		this(new ServiceBrokerConfig());
	}

	public ServiceBroker(String configPath) throws Exception {
		this(new ServiceBrokerConfig(configPath));
	}

	public ServiceBroker(String nodeID, Transporter transporter, Cacher cacher) {
		this(new ServiceBrokerConfig(nodeID, transporter, cacher));
	}

	public ServiceBroker(ServiceBrokerConfig configuration) {

		// Set nodeID
		nodeID = configuration.getNodeID();

		// Configuration
		brokerConfig = configuration;

		// Optional configuration (loaded from file)
		customConfig = configuration.getConfig();

		// Create component registry
		components = configuration.getComponentRegistry();
	}

	// --- GET NODE ID ---

	public String nodeID() {
		return nodeID;
	}

	// --- GET COMPONENTS ---

	public final ComponentRegistry components() {
		return components;
	}

	// --- START BROKER INSTANCE ---

	/**
	 * Start broker. If has transporter, transporter.connect will be called.
	 */
	public final boolean start() {
		try {

			// Start internal and custom components
			logger.info("Starting Moleculer Service Broker (version " + VERSION + ")...");
			components.start(this, brokerConfig, customConfig);
			logger.info("Node \"" + nodeID + "\" started successfully.");

			// Set the pointers of frequently used components
			// serviceRegistry = components.serviceRegistry();
			// eventBus = components.eventBus();
			// ...

			return true;
		} catch (Throwable cause) {
			logger.error("Startup process aborted!", cause);
			stop();
		}
		return false;
	}

	// --- STOP BROKER INSTANCE ---

	/**
	 * Stop broker. If has transporter, transporter.disconnect will be called.
	 */
	public final void stop() {

		// Start internal and custom components
		logger.info("Moleculer Service Broker stopping node \"" + nodeID + "\"...");
		components.stop();
		logger.info("Node \"" + nodeID + "\" stopped.");
	}

	// --- PUBLIC BROKER FUNCTIONS ---

	/**
	 * Switch the console to REPL mode
	 */
	public void repl() {
	}

	/**
	 * Create a new service by schema
	 * 
	 * @param service
	 * @return
	 * @throws Exception
	 */
	public <T extends Service> T createService(T service) throws Exception {
		return null;
	}

	/**
	 * Destroy a local service
	 * 
	 * @param service
	 */
	public void destroyService(Service... service) {
	}

	/**
	 * Get a local service by name
	 * 
	 * @param serviceName
	 * @return
	 */
	public Service getLocalService(String serviceName) {
		return null;
	}

	/**
	 * Get an action by name
	 * 
	 * @param actionName
	 * @return
	 */
	public ActionContainer getAction(String actionName) {
		return null;
	}

	/**
	 * Add a middleware to the broker
	 * 
	 * @param mws
	 */
	public void use(Object... mws) {
	}

	// --- INVOKE LOCAL OR REMOTE ACTION ---

	/**
	 * Call an action (local or remote)
	 * 
	 * @param actionName
	 * @param params
	 * @param opts
	 * 
	 * @return
	 */
	public Promise call(String actionName, Tree params, CallingOptions opts) throws Exception {
		return null;
	}

	// --- EMIT EVENTS VIA EVENT BUS ---

	/**
	 * Emit an event (grouped & balanced global event)
	 * 
	 * @param name
	 * @param payload
	 * @param groups
	 */
	public void emit(String name, Object payload, String... groups) {
	}

	/**
	 * Emit an event for all local & remote services
	 * 
	 * @param name
	 * @param payload
	 */
	public void broadcast(String name, Object payload) {
	}

	/**
	 * Emit an event for all local services
	 * 
	 * @param name
	 * @param payload
	 */
	public void broadcastLocal(String name, Object payload) {
	}

}