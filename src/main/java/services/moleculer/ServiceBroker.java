package services.moleculer;

import java.util.LinkedHashMap;
import java.util.Map;

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
import services.moleculer.services.ServiceRegistry;
import services.moleculer.transporters.Transporter;
import services.moleculer.utils.CommonUtils;

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

	// --- OTHER INTERNAL PROPERTIES AND VARIABLES ---

	private ServiceRegistry serviceRegistry;
	
	private final LinkedHashMap<Service, Tree> services = new LinkedHashMap<>();

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

		// Configuration (created by builder)
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
		
		// Check state
		if (serviceRegistry != null) {
			throw new IllegalStateException("Moleculer Service Broker has already been started!");
		}
		try {

			// Start internal and custom components
			logger.info("Starting Moleculer Service Broker (version " + VERSION + ")...");
			String name = CommonUtils.nameOf(components);
			if (name.indexOf(' ') == -1) {
				name = "\"" + name + "\"";
			}
			logger.info("Using " + name + " to load service classes.");
			components.start(this, brokerConfig, customConfig);
			logger.info("Node \"" + nodeID + "\" started successfully.");

			// Set the pointers of frequently used components
			serviceRegistry = components.serviceRegistry();
			// ...

			// Start pending services
			Tree config;
			Service service;
			for (Map.Entry<Service, Tree> entry: services.entrySet()) {
				service = entry.getKey();
				config = entry.getValue();
				
				// TODO modify nameOf
				name = CommonUtils.nameOf(service);
				if (name.indexOf(' ') == -1) {
					name = "\"" + name + "\"";
				}
				
				if (config == null) {
					config = new Tree();
				}
				serviceRegistry.addService(service, config);
			}
			
			// All components and services started successfully
			services.clear();
			return true;
		} catch (Throwable cause) {
			logger.error("Moleculer Service Broker could not be started!", cause);
			stop();
		}
		return false;
	}

	// --- STOP BROKER INSTANCE ---

	/**
	 * Stop broker. If has transporter, transporter.disconnect will be called.
	 */
	public final void stop() {
		if (serviceRegistry != null) {
			
			// Stop internal and custom components
			logger.info("Moleculer Service Broker stopping node \"" + nodeID + "\"...");
			components.stop();
			logger.info("Node \"" + nodeID + "\" stopped.");
			
			// Clear variables
			services.clear();
			serviceRegistry = null;
		}		
	}

	// --- PUBLIC BROKER FUNCTIONS ---

	/**
	 * Switch the console to REPL mode
	 */
	public void repl() {
	}

	/**
	 * Registers a new local service.
	 * 
	 * @param service
	 *            Service instance
	 * @return optional service configuration
	 * 
	 * @throws Exception
	 *             any exception
	 */
	public <T extends Service> T createService(T service, Tree config) throws Exception {
		if (serviceRegistry == null) {
			
			// Start service later
			services.put(service, config);
		} else {
			
			// Start service now
			serviceRegistry.addService(service, config);
		}
		return service;
	}

	/**
	 * Destroys a local service
	 * 
	 * @param service
	 */
	public void destroyService(Service service) {
		serviceRegistry.removeService(service);
	}

	/**
	 * Returns a local service by name
	 * 
	 * @param serviceName
	 * @return
	 */
	public Service getLocalService(String serviceName) {
		return serviceRegistry.getService(serviceName);
	}

	/**
	 * Returns an action by name
	 * 
	 * @param actionName
	 * @return
	 */
	public ActionContainer getAction(String actionName) {
		return null;
	}

	/**
	 * Adds a middleware to the broker
	 * 
	 * @param mws
	 */
	public void use(Object... mws) {
	}

	// --- INVOKE LOCAL OR REMOTE ACTION ---

	/**
	 * Calls an action (local or remote)
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
	 * Emits an event (grouped & balanced global event)
	 * 
	 * @param name
	 * @param payload
	 * @param groups
	 */
	public void emit(String name, Object payload, String... groups) {
	}

	/**
	 * Emits an event for all local & remote services
	 * 
	 * @param name
	 * @param payload
	 */
	public void broadcast(String name, Object payload) {
	}

	/**
	 * Emits an event for all local services
	 * 
	 * @param name
	 * @param payload
	 */
	public void broadcastLocal(String name, Object payload) {
	}

}