package services.moleculer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.cachers.Cacher;
import services.moleculer.config.ServiceBrokerBuilder;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.CallingOptions;
import services.moleculer.context.ContextFactory;
import services.moleculer.eventbus.EventBus;
import services.moleculer.services.ActionContainer;
import services.moleculer.services.Service;
import services.moleculer.services.ServiceRegistry;
import services.moleculer.transporters.Transporter;
import services.moleculer.utils.CommonUtils;
import services.moleculer.utils.MoleculerComponent;
import services.moleculer.utils.MoleculerComponents;

public final class ServiceBroker {

	// --- VERSION ---

	public static final String VERSION = "1.2";

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- UNIQUE NODE IDENTIFIER ---

	private final String nodeID;

	// --- COMPONENTS ---

	private final MoleculerComponents components;
	private final ServiceRegistry serviceRegistry;
	private final ContextFactory contextFactory;
	private final Transporter transporter;
	private final EventBus eventBus;

	// --- STATIC CONSTRUCTOR ---

	public static final ServiceBrokerBuilder builder() {
		return new ServiceBrokerBuilder(new ServiceBrokerConfig());
	}

	// --- CONSTRUCTORS ---

	public ServiceBroker() {
		this(new ServiceBrokerConfig());
	}

	public ServiceBroker(String nodeID, Transporter transporter, Cacher cacher) {
		this(new ServiceBrokerConfig(nodeID, transporter, cacher));
	}

	public ServiceBroker(ServiceBrokerConfig config) {

		// Set nodeID
		nodeID = config.getNodeID();

		// Set components
		components = new MoleculerComponents(config);
		config = null;

		// Set the pointers of frequently used components
		serviceRegistry = components.serviceRegistry();
		contextFactory = components.contextFactory();
		transporter = components.transporter();
		eventBus = components.eventBus();

		// Init base components
		try {

			// Starting Moleculer Service Broker
			logger.info("Starting Moleculer Service Broker (version " + VERSION + ")...");

			// Start service registry
			start(components.serviceRegistry());

		} catch (Exception cause) {
			throw new RuntimeException("Unable to init logger!", cause);
		}
	}

	// --- GET NODE ID ---

	public String nodeID() {
		return nodeID;
	}

	// --- GET COMPONENTS ---

	public final MoleculerComponents components() {
		return components;
	}

	// --- START BROKER INSTANCE ---

	/**
	 * Start broker. If has transporter, transporter.connect will be called.
	 */
	public final void start() throws Exception {

		// Starting thread-based components
		logger.info("Starting node \"" + nodeID + "\"...");

		// Start internal components
		start(contextFactory);
		start(components.uidGenerator());
		start(eventBus);
		start(components.cacher());
		start(transporter);

		// Ok, all components started successfully
		logger.info("Node \"" + nodeID + "\" started successfully.");
	}

	private final void start(MoleculerComponent component) throws Exception {
		if (component != null) {
			String name = CommonUtils.nameOf(component);
			try {
				component.init(this);
				logger.info(name + " started.");
			} catch (Exception cause) {
				logger.error("Unable to start " + name + "!", cause);
				throw cause;
			}
		}
	}

	/**
	 * Stop broker. If has transporter, transporter.disconnect will be called.
	 */
	public final void stop() {

		// Starting Moleculer Service Broker
		logger.info("Moleculer Service Broker stopping node \"" + nodeID + "\"...");

		// Stop internal components
		stop(serviceRegistry);
		stop(transporter);
		stop(components.cacher());
		stop(eventBus);
		stop(components.uidGenerator());
		stop(contextFactory);

		// Ok, broker stopped
		logger.info("Node \"" + nodeID + "\" stopped.");
	}

	private final void stop(MoleculerComponent component) {
		if (component != null) {
			String name = CommonUtils.nameOf(component);
			try {
				component.init(this);
				logger.info(name + " stopped.");
			} catch (Throwable cause) {
				logger.error("Unable to stop " + name + "!", cause);
			}
		}
	}

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
		serviceRegistry.addService(service);
		return service;
	}

	/**
	 * Destroy a local service
	 * 
	 * @param service
	 */
	public void destroyService(Service... service) {
		serviceRegistry.removeService(service);
	}

	/**
	 * Get a local service by name
	 * 
	 * @param serviceName
	 * @return
	 */
	public Service getLocalService(String serviceName) {
		return serviceRegistry.getService(serviceName);
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