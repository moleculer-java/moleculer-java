package services.moleculer;

import java.lang.reflect.Field;

import io.datatree.Tree;
import services.moleculer.actions.Action;
import services.moleculer.actions.ActionRegistry;
import services.moleculer.cachers.Cache;
import services.moleculer.cachers.Cacher;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.CallingOptions;
import services.moleculer.context.Context;
import services.moleculer.eventbus.EventBus;
import services.moleculer.eventbus.Listener;
import services.moleculer.logger.Logger;
import services.moleculer.logger.LoggerFactory;
import services.moleculer.logger.NoOpLoggerFactory;
import services.moleculer.services.Service;
import services.moleculer.services.ServiceRegistry;
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
	private final UIDGenerator uidGenerator;
	private final ServiceRegistry serviceRegistry;
	private final ActionRegistry actionRegistry;
	private final Cacher cacher;
	private final EventBus eventBus;
	private final Transporter transporter;

	// --- BROKER'S LOGGER ---

	private Logger logger = NoOpLoggerFactory.getInstance();

	// --- CONSTRUCTORS ---

	public ServiceBroker() {
		this(new ServiceBrokerConfig());
	}

	public ServiceBroker(String nodeID, Transporter transporter, Cacher cacher) {
		this(new ServiceBrokerConfig(nodeID, transporter, cacher));
	}

	public ServiceBroker(ServiceBrokerConfig config) {
		nodeID = config.getNodeID();
		loggerFactory = config.getLoggerFactory();
		uidGenerator = config.getUIDGenerator();
		serviceRegistry = config.getServiceRegistry();
		actionRegistry = config.getActionRegistry();
		cacher = config.getCacher();
		eventBus = config.getEventBus();
		transporter = config.getTransporter();
	}

	// --- GET NODE ID ---

	public String nodeID() {
		return nodeID;
	}

	// --- START BROKER INSTANCE ---

	/**
	 * Start broker. If has transporter, transporter.connect will be called.
	 */
	public final void start() throws Exception {

		// Start logger factory
		loggerFactory.init(this);

		// Create logger of broker instance
		logger = loggerFactory.getLogger(ServiceBroker.class);

		// Starting Moleculer Service Broker
		logger.info("Moleculer Service Broker (version " + VERSION + ") starting node \"" + nodeID + "\"...");

		// Start internal components
		start(uidGenerator);
		start(eventBus);
		start(cacher);
		start(transporter);
		start(serviceRegistry);
		start(actionRegistry);

		// Ok, all components started successfully
		logger.info("Node \" + nodeID + \" started successfully.");
	}

	private final void start(MoleculerComponent component) throws Exception {
		if (component != null) {
			String info = component.name();
			if (info == null || info.isEmpty()) {
				info = component.getClass().toString();
			}
			try {
				logger.info("Starting " + info + "...");
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
		stop(actionRegistry);
		stop(serviceRegistry);
		stop(transporter);
		stop(cacher);
		stop(eventBus);
		stop(uidGenerator);

		// Ok, broker stopped
		logger.info("Node \" + nodeID + \" stopped successfully.");

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
				logger.info("Stopping " + info + "...");
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

	public Logger getLogger(Class<?> clazz) {
		return loggerFactory.getLogger(clazz);
	}

	/**
	 * Create a new service by schema
	 * 
	 * @param service
	 * @return
	 * @throws Exception
	 */
	public <T extends Service> T createService(T service) throws Exception {

		// Get annotations of actions
		Class<? extends Service> clazz = service.getClass();
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			if (Action.class.isAssignableFrom(field.getType())) {

				// Name of the action (eg. "v2.service.add")
				String name = service.name() + '.' + field.getName();

				// Process "Cache" annotation
				Cache cache = field.getAnnotation(Cache.class);
				boolean cached = false;
				String[] keys = null;
				if (cache != null) {
					cached = true;
					if (cached) {
						keys = cache.value();
						if (keys != null && keys.length == 0) {
							keys = null;
						}
					}
				}

				// Action instance
				Action action = (Action) field.get(service);

				// Register action
				actionRegistry.add(name, cached, keys, action);

			}
		}

		// Register service
		// TODO remove actions on error
		serviceRegistry.add(service);
		
		return service;
	}

	/**
	 * Destroy a local service
	 * 
	 * @param service
	 */
	public boolean destroyService(Service service) {
		return serviceRegistry.remove(service.name());
	}

	/**
	 * Get a local service by name
	 * 
	 * @param serviceName
	 * @return
	 */
	public Service getService(String serviceName) {
		return serviceRegistry.get(serviceName);
	}

	/**
	 * Has a local service by name
	 * 
	 * @param serviceName
	 * @return
	 */
	public boolean hasService(String serviceName) {
		return serviceRegistry.contains(serviceName);
	}

	/**
	 * Has an action by name
	 * 
	 * @param actionName
	 * @return
	 */
	public boolean hasAction(String nodeID, String actionName) {
		return getAction(nodeID, actionName) != null;
	}

	/**
	 * Has an action by name
	 * 
	 * @param actionName
	 * @return
	 */
	public boolean hasAction(String actionName) {
		return getAction(actionName) != null;
	}

	/**
	 * Get an action by name
	 * 
	 * @param actionName
	 * @return
	 */
	public Action getAction(String actionName) {
		return actionRegistry.get(null, actionName);
	}

	/**
	 * Get an action by name
	 * 
	 * @param actionName
	 * @return
	 */
	public Action getAction(String nodeID, String actionName) {
		return actionRegistry.get(nodeID, actionName);
	}

	/**
	 * Check has callable action handler
	 * 
	 * @param actionName
	 * @return
	 */
	public boolean isActionAvailable(String actionName) {
		return false;
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
	 * @return
	 */
	public Object call(String actionName, Tree params, CallingOptions opts, String requestID) throws Exception {
		Action action = getAction(actionName);
		Context ctx = new Context(this, action, params, requestID);
		return action.handler(ctx);
	}

	// --- ADD EVENT LISTENER TO THE EVENT BUS ---

	/**
	 * Subscribe to an event
	 * 
	 * @param name
	 * @param handler
	 */
	public void on(String name, Listener handler) {
		eventBus.on(name, handler, false);
	}

	/**
	 * Subscribe to an event once
	 * 
	 * @param name
	 * @param listener
	 */
	public void once(String name, Listener handler) {
		eventBus.on(name, handler, true);
	}

	// --- REMOVE EVENT LISTENER FROM THE EVENT BUS ---

	/**
	 * Unsubscribe from an event
	 * 
	 * @param name
	 * @param listener
	 */
	public void off(String name, Listener handler) {
		eventBus.off(name, handler);
	}

	// --- EMIT EVENTS VIA EVENT BUS ---

	/**
	 * Emit an event (global & local)
	 * 
	 * @param name
	 * @param payload
	 */
	public void emit(String name, Object payload) {
		eventBus.emit(name, payload);
		if (transporter != null) {
			transporter.publish(name, null, payload);
		}
	}

	/**
	 * Emit a local event
	 * 
	 * @param name
	 * @param payload
	 */
	public void emitLocal(String name, Object payload) {
		eventBus.emit(name, payload);
	}

}