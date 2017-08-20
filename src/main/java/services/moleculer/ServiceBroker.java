package services.moleculer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import io.datatree.Tree;
import services.moleculer.cachers.Cacher;
import services.moleculer.utils.EventBus;

public class ServiceBroker {

	// Local services
	private List<Service> services = new LinkedList<Service>();

	// Registered middlewares
	// private List<Object> middlewares = new LinkedList<Object>();

	public String namespace = "";
	public String nodeID;
	public Logger logger;
	public Cacher cacher;

	// --- INTERNAL EVENT BUS ---
	
	private final EventBus bus = new EventBus();
	
	// --- CONSTRUCTOR ---

	/**
	 * Creates an instance of ServiceBroker.
	 * 
	 * @param options
	 */
	public ServiceBroker() {
		this.logger = this.getLogger("broker");
		if (this.nodeID == null || this.nodeID.isEmpty()) {
			try {
				this.nodeID = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				this.logger.warn("Can't resolve hostname!");
			}
		}
	}

	/**
	 * Start broker. If has transporter, transporter.connect will be called.
	 */
	public void start() {
		// Call `started` of all services
		// Start transit.connect if transporter is defined

		this.logger.info("Broker started! NodeID: " + this.nodeID);
	}

	/**
	 * Stop broker. If has transporter, transporter.disconnect will be called.
	 */
	public void stop() {
		// Call `stopped` of all services
		// Start transit.disconnect if transporter is defined

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
	 */
	public <T extends Service> T createService(T service) {
		this.services.add(service);

		return service;
	}

	/**
	 * Destroy a local service
	 * 
	 * @param service
	 */
	public void destroyService(Service service) {
		service.stopped();
		this.services.remove(service);

		// TODO: Notify all other nodes
	}

	/**
	 * Get a local service by name
	 * 
	 * @param serviceName
	 * @return
	 */
	public Service getService(String serviceName) {
		return null;
	}

	/**
	 * Has a local service by name
	 * 
	 * @param serviceName
	 * @return
	 */
	public boolean hasService(String serviceName) {
		return false;
	}

	/**
	 * Has an action by name
	 * 
	 * @param actionName
	 * @return
	 */
	public boolean hasAction(String actionName) {
		return false;
	}

	/**
	 * Get an action by name
	 * 
	 * @param actionName
	 * @return
	 */
	public Action getAction(String actionName) {
		return null;
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
		return null;
	}

	// --- ADD EVENT LISTENER TO THE EVENT BUS ---
	
	/**
	 * Subscribe to an event
	 * 
	 * @param name
	 * @param handler
	 */
	public void on(String name, Listener handler) {
		this.bus.on(name, handler, false);
	}

	/**
	 * Subscribe to an event once
	 * 
	 * @param name
	 * @param listener
	 */
	public void once(String name, Listener handler) {
		this.bus.on(name, handler, true);
	}
	
	// --- REMOVE EVENT LISTENER FROM THE EVENT BUS ---
	
	/**
	 * Unsubscribe from an event
	 * 
	 * @param name
	 * @param listener
	 */
	public void off(String name, Listener handler) {
		this.bus.off(name, handler);
	}
	
	// --- EMIT EVENTS VIA EVENT BUS ---

	/**
	 * Emit an event (global & local)
	 * 
	 * @param name
	 * @param payload
	 */
	public void emit(String name, Object payload) {
		this.bus.emit(name, payload, nodeID);
		
		//if (this.transit)
		//	this.transit.emit(name, payload);
	}
	
	/**
	 * Emit an event (global & local)
	 * 
	 * @param name
	 * @param payload
	 */
	public void emitLocal(String name, Object payload, String sender) {
		this.bus.emit(name, payload, sender);
	}
	
}