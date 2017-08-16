package services.moleculer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import io.datatree.Tree;
import services.moleculer.cachers.Cacher;

public class ServiceBroker {
	
	// Local services
	private List<Service> services = new LinkedList<Service>();
	
	// Registered middlewares
	//private List<Object> middlewares = new LinkedList<Object>();
	
	public String namespace = "";
	
	public String nodeID;
	
	public Logger logger;
	
	public Cacher cacher;
	
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
	 * Get a custom logger for sub-modules (service, transporter, cacher, context...etc)
	 * 
	 * @param module
	 * @return
	 */
	public Logger getLogger(String module) {
		return new Logger();
	}

	/**
	 * Get a custom logger for sub-modules (service, transporter, cacher, context...etc)
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
	 * Subscribe to an event
	 * 
	 * @param name
	 * @param handler
	 */
	public void on(String name, Object handler) {
		
	}
	
	/**
	 * Unsubscribe from an event
	 * 
	 * @param name
	 * @param handler
	 */
	public void off(String name, Object handler) {
		
	}
	
	/**
	 * Subscribe to an event once
	 * 
	 * @param name
	 * @param handler
	 */
	public void once(String name, Object handler) {
		
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
	 * Create a new Context instance
	 * 
	 * @param action
	 * @param nodeID
	 * @param params
	 * @param opts
	 * @return
	 */
	public Context createNewContext(Action action, String nodeID, Tree params, CallingOptions opts) {
		return null;
	}
	
	/**
	 * Call an action (local or remote)
	 * 
	 * @param actionName
	 * @param params
	 * @param opts
	 * @return
	 */
	public Object call(String actionName, Tree params, CallingOptions opts) {
		return null;
	}
	
	/**
	 * Emit an event (global & local)
	 * 
	 * @param eventName
	 * @param payload
	 */
	public void emit(String eventName, Object payload) {
		
	}
	
	/**
	 * Emit an event only local
	 * 
	 * @param eventName
	 * @param payload
	 * @param sender
	 */
	public void emitLocal(String eventName, Object payload, String sender) {
		
	}
}
