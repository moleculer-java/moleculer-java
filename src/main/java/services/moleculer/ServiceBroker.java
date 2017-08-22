package services.moleculer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import io.datatree.Tree;
import services.moleculer.actions.ActionRegistry;
import services.moleculer.cachers.Cacher;
import services.moleculer.transporters.Transporter;
import services.moleculer.utils.EventBus;

public class ServiceBroker {

	// Local services
	private HashMap<String, Service> services = new HashMap<>();

	private final Logger logger;

	// --- INTERNAL OBJECTS ---

	private final String nodeID;
	private final EventBus bus;
	private final Cacher cacher;
	private final Transporter transporter;
	private final InvocationStrategy invocationStrategy;
	private final ActionRegistry actionRegistry;

	// --- CONSTRUCTORS ---

	public ServiceBroker() {
		this(null, null, null, null);
	}

	public ServiceBroker(String nodeID, Cacher cacher, Transporter transporter, InvocationStrategy invocationStrategy) {

		// TODO
		this.logger = this.getLogger("broker");
		int initialCapacity = 2048;
		boolean fair = true;
		
		// Init internal objects
		String id = nodeID;
		if (id == null || id.isEmpty()) {
			try {
				id = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				this.logger.warn("Can't resolve hostname!");
			}
		}
		this.nodeID = id == null || id.isEmpty() ? "default" : id;
		this.bus = new EventBus(initialCapacity, fair);
		this.cacher = cacher;
		this.transporter = transporter;
		this.invocationStrategy = invocationStrategy;
		this.actionRegistry = new ActionRegistry(this, fair);
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

		// Log
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
		this.logger.info("Broker stopped! NodeID: " + this.nodeID);
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
		return new Logger();
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
		return new Logger();
	}

	/**
	 * Create a new service by schema
	 * 
	 * @param service
	 * @return
	 * @throws Exception 
	 */
	public <T extends Service> T createService(T service) throws Exception {

		services.put(service.name, service);

		Field[] fields = service.getClass().getFields();
		for (Field field : fields) {
			if (Action.class.isAssignableFrom(field.getType())) {

				// "list"
				String name = field.getName();


				Annotation[] annotations = field.getAnnotations();
				
				// Annotation values
				boolean cached = false;
				String version = null;

				for (Annotation annotation : annotations) {
					if (annotation instanceof Cache) {
						cached = ((Cache) annotation).value();
						continue;
					}
					if (annotation instanceof Version) {
						version = ((Version) annotation).value();
						continue;
					}
				}
				if (version != null && !version.isEmpty()) {
					name = version + '.' + service.name + '.' + name;
				} else {
					name = service.name + '.' + name;
				}
				
				// Action instance
				Action action = (Action) field.get(service);
				
				// Register
				actionRegistry.registerLocalAction(name, cached, action);
				
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
		try {
			service.stopped();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		services.remove(service.name);

		// TODO: Notify all other nodes
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
		return actionRegistry.getAction(null, actionName);
	}

	/**
	 * Get an action by name
	 * 
	 * @param actionName
	 * @return
	 */
	public Action getAction(String nodeID, String actionName) {
		return actionRegistry.getAction(nodeID, actionName);
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
	public Object call(String actionName, Tree params, CallingOptions opts) throws Exception {
		Action action = getAction(actionName);
		Context ctx = new Context(this, action, params, null);
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
		bus.emit(name, payload, nodeID);
		if (transporter != null) {
			transporter.publish(null);
		}
	}

	/**
	 * Emit an event (global & local)
	 * 
	 * @param name
	 * @param payload
	 */
	public void emitLocal(String name, Object payload, String sender) {
		bus.emit(name, payload, sender);
	}

	// --- GETTERS ---

	public Cacher getCacher() {
		return cacher;
	}

	public String getNodeID() {
		return nodeID;
	}

	public Transporter getTransporter() {
		return transporter;
	}

	public InvocationStrategy getInvocationStrategy() {
		return invocationStrategy;
	}

}