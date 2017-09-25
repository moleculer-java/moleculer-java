package services.moleculer;

import java.lang.reflect.Field;
import java.util.HashMap;

import io.datatree.Tree;
import services.moleculer.actions.Action;
import services.moleculer.actions.ActionRegistry;
import services.moleculer.cachers.Cache;
import services.moleculer.cachers.Cacher;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.logger.Logger;
import services.moleculer.transporters.Transporter;
import services.moleculer.utils.EventBus;
import services.moleculer.utils.UIDGenerator;

public class ServiceBroker {

	// --- CONFIGURATION ---

	private ServiceBrokerConfig config;

	// --- LOGGER ---

	private Logger logger;

	// --- SERVICE REGISTRY ---

	private final HashMap<String, Service> services = new HashMap<>();

	// --- INTERNAL ACTION REGISTRY ---

	private final ActionRegistry actionRegistry;

	// --- INTERNAL EVENT BUS ---

	private final EventBus bus = new EventBus();

	// --- INTERNAL UUID GENERATOR ---

	private UIDGenerator uidGenerator;

	// --- CONSTRUCTORS ---

	public ServiceBroker() {
		this(new ServiceBrokerConfig());
	}

	public ServiceBroker(String nodeID, Transporter transporter, Cacher cacher) {
		ServiceBrokerConfig config = new ServiceBrokerConfig();
		config.setNodeID(nodeID);
		config.setTransporter(transporter);
		config.setCacher(cacher);
		this.config = config;
	}

	public ServiceBroker(ServiceBrokerConfig config) {
		this.config = config;
	}

	// --- START BROKER INSTANCE ---

	/**
	 * Start broker. If has transporter, transporter.connect will be called.
	 */
	public void start() throws Exception {

		// Init services
		for (Service service : services.values()) {
			service.started();
		}

		// Init cacher
		if (cacher != null) {
			cacher.init(this);
		}

		// Init transporter
		if (transporter != null) {
			transporter.init(this);
		}

		// Ok, broker started
		logger.info("Broker started! NodeID: " + this.nodeID);
	}

	/**
	 * Stop broker. If has transporter, transporter.disconnect will be called.
	 */
	public void stop() {

		// Init services
		for (Service service : services.values()) {
			try {
				service.stopped();
			} catch (Throwable cause) {
				logger.warn("Unable to stop service!");
			}
		}

		// Stop cacher
		if (cacher != null) {
			try {
				cacher.close();
			} catch (Throwable cause) {
				logger.warn("Unable to stop cacher!");
			}
		}

		// Stop transporter
		if (transporter != null) {
			try {
				transporter.disconnect();
			} catch (Throwable cause) {
				logger.warn("Unable to stop transporter!");
			}
		}

		// Log
		this.logger.info("Broker stopped! NodeID: " + config.getNodeID());
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
	 * @param module
	 * @return
	 */
	public Logger getLogger(String module) {
		return config.getLoggerFactory().getLogger(module);
	}

	/**
	 * Get a custom logger for sub-modules (service, transporter, cacher,
	 * context...etc)
	 * 
	 * @param module
	 * @param service
	 * @param version
	 * @return
	 */
	public Logger getLogger(String module, String service, String version) {
		return config.getLoggerFactory().getLogger(module + '.' + service + '.' + version);
	}

	/**
	 * Create a new service by schema
	 * 
	 * @param service
	 * @return
	 * @throws Exception
	 */
	public <T extends Service> T createService(T service) throws Exception {

		// Init service
		service.init(this, service.name);

		// Register service
		services.put(service.name, service);

		// Get annotations of actions
		Class<? extends Service> clazz = service.getClass();
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			if (Action.class.isAssignableFrom(field.getType())) {

				// Name of the action (eg. "v2.service.add")
				String name = service.name + '.' + field.getName();

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

		service.created();
		return service;
	}

	/**
	 * Destroy a local service
	 * 
	 * @param service
	 */
	public void destroyService(Service service) {

		// Unregister service
		boolean found = services.remove(service.name) != null;
		if (!found) {
			throw new IllegalStateException("Service is not registered!");
		}

		// TODO Unregister actions

		// TODO Notify all other nodes

		// Invoke "stopped" method
		try {
			service.stopped();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get a local service by name
	 * 
	 * @param serviceName
	 * @return
	 */
	public Service getService(String serviceName) {
		return services.get(serviceName);
	}

	/**
	 * Has a local service by name
	 * 
	 * @param serviceName
	 * @return
	 */
	public boolean hasService(String serviceName) {
		return services.containsKey(serviceName);
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
		bus.on(name, handler, false);
	}

	/**
	 * Subscribe to an event once
	 * 
	 * @param name
	 * @param listener
	 */
	public void once(String name, Listener handler) {
		bus.on(name, handler, true);
	}

	// --- REMOVE EVENT LISTENER FROM THE EVENT BUS ---

	/**
	 * Unsubscribe from an event
	 * 
	 * @param name
	 * @param listener
	 */
	public void off(String name, Listener handler) {
		bus.off(name, handler);
	}

	// --- EMIT EVENTS VIA EVENT BUS ---

	/**
	 * Emit an event (global & local)
	 * 
	 * @param name
	 * @param payload
	 */
	public void emit(String name, Object payload) {
		bus.emit(name, payload);
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
		bus.emit(name, payload);
	}

	// --- UUID GENERATOR ---

	protected String nextUID() {
		return uidGenerator.next();
	}

}