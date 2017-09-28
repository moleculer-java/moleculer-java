package services.moleculer;

import io.datatree.Tree;
import services.moleculer.cachers.Cacher;
import services.moleculer.config.ServiceBrokerBuilder;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.CallingOptions;
import services.moleculer.context.Context;
import services.moleculer.context.ContextPool;
import services.moleculer.eventbus.EventBus;
import services.moleculer.eventbus.Listener;
import services.moleculer.logger.Logger;
import services.moleculer.logger.LoggerFactory;
import services.moleculer.logger.NoOpLoggerFactory;
import services.moleculer.services.Action;
import services.moleculer.services.Service;
import services.moleculer.services.ServiceRegistry;
import services.moleculer.strategies.InvocationStrategyFactory;
import services.moleculer.transporters.Transporter;
import services.moleculer.uids.UIDGenerator;
import services.moleculer.utils.MoleculerComponent;

public final class ServiceBroker {

	// --- VERSION ---

	public static final String VERSION = "1.2";

	// --- UNIQUE NODE IDENTIFIER ---

	private final String nodeID;

	// --- INTERNAL COMPONENTS ---

	private final LoggerFactory loggerFactory;
	private final ContextPool contextPool;
	private final UIDGenerator uidGenerator;
	private final InvocationStrategyFactory invocationStrategyFactory;
	private final ServiceRegistry serviceRegistry;
	private final Cacher cacher;
	private final EventBus eventBus;
	private final Transporter transporter;

	// --- BROKER'S LOGGER ---

	private Logger logger = NoOpLoggerFactory.getInstance();

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

		// Set components
		nodeID = config.getNodeID();
		loggerFactory = config.getLoggerFactory();
		contextPool = config.getContextPool();
		uidGenerator = config.getUIDGenerator();
		invocationStrategyFactory = config.getInvocationStrategyFactory();
		serviceRegistry = config.getServiceRegistry();
		cacher = config.getCacher();
		eventBus = config.getEventBus();
		transporter = config.getTransporter();

		// Init base components
		try {

			// Init logger factory
			loggerFactory.init(this);

			// Create logger of broker instance
			logger = loggerFactory.getLogger(nodeID);

			// Start service registry
			start(serviceRegistry);

		} catch (Exception cause) {
			throw new RuntimeException("Unable to init logger!", cause);
		}

		// Starting Moleculer Service Broker
		logger.info("Moleculer Service Broker (version " + VERSION + ") initalized.");
	}

	// --- GET NODE ID ---

	public String nodeID() {
		return nodeID;
	}

	// --- GET COMPONENTS ---

	// TODO remove unecessary getters

	public final LoggerFactory loggerFactory() {
		return loggerFactory;
	}

	public final ContextPool contextPool() {
		return contextPool;
	}

	public final UIDGenerator uidGenerator() {
		return uidGenerator;
	}

	public final InvocationStrategyFactory invocationStrategyFactory() {
		return invocationStrategyFactory;
	}

	public final ServiceRegistry serviceRegistry() {
		return serviceRegistry;
	}

	public final Cacher cacher() {
		return cacher;
	}

	public final EventBus eventBus() {
		return eventBus;
	}

	public final Transporter transporter() {
		return transporter;
	}

	// --- START BROKER INSTANCE ---

	/**
	 * Start broker. If has transporter, transporter.connect will be called.
	 */
	public final void start() throws Exception {

		// Starting thread-based components
		logger.info("Starting node \"" + nodeID + "\"...");

		// Start internal components
		start(contextPool);
		start(uidGenerator);
		start(eventBus);
		start(cacher);
		start(transporter);

		// Ok, all components started successfully
		int services = serviceRegistry.countServices();
		String info = services + " service";
		if (services > 1) {
			info += "s";
		}
		logger.info("Node \"" + nodeID + "\" started successfully with " + info + '.');
	}

	private final void start(MoleculerComponent component) throws Exception {
		if (component != null) {
			String info = component.name();
			if (info == null || info.isEmpty()) {
				info = component.getClass().toString();
			}
			try {
				component.init(this);
				logger.info(info + " started.");
			} catch (Exception cause) {
				logger.fatal("Unable to start " + info + "!", cause);
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
		stop(cacher);
		stop(eventBus);
		stop(uidGenerator);
		stop(contextPool);

		// Ok, broker stopped
		logger.info("Node \"" + nodeID + "\" stopped.");

		// Stop logger factory
		loggerFactory.close();
	}

	private final void stop(MoleculerComponent component) {
		if (component != null) {
			String info = component.name();
			if (info == null || info.isEmpty()) {
				info = component.getClass().toString();
			}
			try {
				component.init(this);
				logger.info(info + " stopped.");
			} catch (Throwable cause) {
				logger.fatal("Unable to stop " + info + "!", cause);
			}
		}
	}

	/**
	 * Switch the console to REPL mode
	 */
	public void repl() {

	}

	/**
	 * Get a custom logger for sub-modules (service, transporter, cacher,
	 * context...etc)
	 * 
	 * @param name
	 * @return
	 */
	public Logger getLogger(String name) {
		return loggerFactory.getLogger(name);
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
	public boolean destroyService(Service service) {
		return serviceRegistry.removeService(service.name());
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
	public Action getAction(String actionName) {
		// TODO: it returns an Endpoint instance

		return serviceRegistry.getAction(null, actionName);
	}

	/**
	 * Find the next available endpoint for action
	 * 
	 * @param actionName
	 * @param nodeID
	 * @return
	 */
	public Action findNextActionEndpoint(String actionName, String nodeID) {
		// TODO: it returns an Endpoint instance
		return serviceRegistry.getAction(nodeID, actionName);
	}

	/**
	 * Add a middleware to the broker
	 * 
	 * @param mws
	 */
	public void use(Object... mws) {

	}

	/**
	 * Call an action (local or remote)
	 * 
	 * @param actionName
	 * @param params
	 * @param opts
	 * 
	 * @return
	 */
	public Object call(String actionName, Tree params, CallingOptions opts) throws Exception {
		Action action = getAction(actionName);
		Context ctx = null;
		try {
			ctx = contextPool.borrow();
			return action.handler(ctx);
		} finally {
			if (ctx != null) {
				contextPool.release(ctx);
			}
		}
	}

	// --- ADD EVENT LISTENER TO THE EVENT BUS ---

	// /**
	// * Subscribe to an event
	// *
	// * @param name
	// * @param handler
	// */
	// public void on(String name, Listener handler) {
	// eventBus.on(name, handler, false);
	// }
	//
	// /**
	// * Subscribe to an event once
	// *
	// * @param name
	// * @param listener
	// */
	// public void once(String name, Listener handler) {
	// eventBus.on(name, handler, true);
	// }
	//
	// // --- REMOVE EVENT LISTENER FROM THE EVENT BUS ---
	//
	// /**
	// * Unsubscribe from an event
	// *
	// * @param name
	// * @param listener
	// */
	// public void off(String name, Listener handler) {
	// eventBus.off(name, handler);
	// }

	// --- EMIT EVENTS VIA EVENT BUS ---

	/**
	 * Emit an event (grouped & balanced global event)
	 * 
	 * @param name
	 * @param payload
	 * @param groups
	 */
	public void emit(String name, Object payload, String[] groups) {
		eventBus.emit(name, payload);
		if (transporter != null) {
			transporter.publish(name, null, payload);
		}
	}

	/**
	 * Emit an event for all local & remote services
	 * 
	 * @param name
	 * @param payload
	 */
	public void broadcast(String name, Object payload) {
		eventBus.emit(name, payload);
	}

	/**
	 * Emit an event for all local services
	 * 
	 * @param name
	 * @param payload
	 */
	public void broadcastLocal(String name, Object payload) {
		eventBus.emit(name, payload);
	}

}