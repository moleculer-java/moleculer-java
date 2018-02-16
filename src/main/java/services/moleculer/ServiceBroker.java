/**
 * MOLECULER MICROSERVICES FRAMEWORK<br>
 * <br>
 * This project is based on the idea of Moleculer Microservices
 * Framework for NodeJS (https://moleculer.services). Special thanks to
 * the Moleculer's project owner (https://github.com/icebob) for the
 * consultations.<br>
 * <br>
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer;

import static services.moleculer.util.CommonUtils.nameOf;
import static services.moleculer.util.CommonUtils.parseParams;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.cacher.Cacher;
import services.moleculer.config.ComponentRegistry;
import services.moleculer.config.ServiceBrokerBuilder;
import services.moleculer.config.ServiceBrokerSettings;
import services.moleculer.context.CallingOptions;
import services.moleculer.context.Context;
import services.moleculer.eventbus.EventBus;
import services.moleculer.eventbus.Groups;
import services.moleculer.internal.NodeService;
import services.moleculer.repl.Repl;
import services.moleculer.service.ActionEndpoint;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.transporter.Transporter;
import services.moleculer.util.ParseResult;

/**
 * Service Broker.
 */
@Name("Service Broker")
public class ServiceBroker {

	// --- VERSIONS ---

	/**
	 * Version of the Java ServiceBroker API.
	 */
	public static final String SOFTWARE_VERSION = "1.3";

	/**
	 * Version of the implemented Moleculer Protocol.
	 */
	public static final String PROTOCOL_VERSION = "3";

	// --- LOGGER ---

	/**
	 * SLF4J logger of this class.
	 */
	protected final static Logger logger = LoggerFactory.getLogger(ServiceBroker.class);

	// --- UNIQUE NODE IDENTIFIER ---

	/**
	 * Unique server ID (eg. "node1", "server-2", etc.)
	 */
	protected final String nodeID;

	// --- CONFIGURATIONS ---

	/**
	 * Configuration (created by the {@link ServiceBrokerBuilder}).
	 */
	protected final ServiceBrokerSettings settings;

	/**
	 * Optional configuration (loaded from JSON/YAML/TOML/XML/JS file).
	 */
	protected final Tree config;

	// --- INERNAL AND USER-DEFINED COMPONENTS ---

	/**
	 * Component Registry of the Service Broker instance. ComponentRegistry has
	 * similar functionality to Spring's ApplicationContext; stores "beans"
	 * (MoleculerComponents), and by using the method "get(id)" you can retrieve
	 * instances of your component.
	 */
	protected final ComponentRegistry components;

	/**
	 * Registry of local and remote Moleculer Services.
	 */
	protected ServiceRegistry registry;

	/**
	 * Local EventBus.
	 */
	protected EventBus eventbus;

	// --- SERVICES AND CONFIGURATIONS ---

	/**
	 * Services which defined and added to the Broker before the boot process.
	 */
	protected final LinkedHashMap<Service, Tree> services = new LinkedHashMap<>();

	// --- STATIC SERVICE BROKER BUILDER ---

	/**
	 * Creates a new {@link ServiceBrokerBuilder} instance.
	 * 
	 * @return builder instance
	 */
	public static ServiceBrokerBuilder builder() {
		return new ServiceBrokerBuilder(new ServiceBrokerSettings());
	}

	// --- CONSTRUCTORS ---

	public ServiceBroker() {
		this(new ServiceBrokerSettings());
	}

	public ServiceBroker(String configPath) throws Exception {
		this(new ServiceBrokerSettings(configPath));
	}

	public ServiceBroker(String nodeID, Transporter transporter, Cacher cacher) {
		this(new ServiceBrokerSettings(nodeID, transporter, cacher));
	}

	public ServiceBroker(ServiceBrokerSettings settings) {

		// Configuration (created by builder)
		this.settings = settings;

		// Optional configuration (loaded from file)
		config = settings.getConfig();

		// Set nodeID
		nodeID = settings.getNodeID();

		// Set the component registry
		components = settings.getComponents();
		
		// Install internal services
		if (settings.isInternalServices()) {
			try {
				createService(new NodeService());				
			} catch (Exception cause) {
				logger.error("Unable to install \"$node\" service!", cause);
			}
		}
	}

	// --- GET NODE ID ---

	public String nodeID() {
		return nodeID;
	}

	// --- GET COMPONENT REGISTRY ---

	public ComponentRegistry components() {
		return components;
	}

	// --- START BROKER INSTANCE ---

	/**
	 * Start broker. If has transporter, transporter.connect will be called.
	 */
	public void start() {

		// Check state
		if (registry != null) {
			throw new IllegalStateException("Moleculer Service Broker has already been started!");
		}
		try {

			// Start internal and custom components
			logger.info("Starting Moleculer Service Broker (version " + SOFTWARE_VERSION + ")...");
			String name = nameOf(components, true);
			logger.info("Using " + name + " to load service classes.");
			components.start(this, settings, config);
			logger.info("Node \"" + nodeID + "\" started successfully.");

			// Set the pointers of frequently used components
			registry = components.registry();
			eventbus = components.eventbus();
			
			// Register and start pending services
			for (Map.Entry<Service, Tree> entry : services.entrySet()) {
				Service service = entry.getKey();
				Tree cfg = entry.getValue();
				if (cfg == null) {
					cfg = new Tree();
				}

				// Register actions
				registry.addActions(service, cfg);

				// Register listeners
				eventbus.addListeners(service, cfg);
			}

			// All components and services started successfully
			services.clear();
			
			// Start transporter's connection loop
			Transporter transporter = components.transporter();
			if (transporter != null) {
				transporter.connect();
			}

		} catch (Throwable cause) {
			logger.error("Moleculer Service Broker could not be started!", cause);
			stop();
		}
	}

	// --- STOP BROKER INSTANCE ---

	/**
	 * Stop broker. If the Broker has a Transporter, transporter.disconnect will
	 * be called.
	 */
	public void stop() {
		if (registry != null) {

			// Stop internal and custom components
			logger.info("Moleculer Service Broker stopping node \"" + nodeID + "\"...");
			components.stop();
			logger.info("Node \"" + nodeID + "\" stopped.");

			// Clear variables
			services.clear();
			registry = null;
		}
	}

	// --- PUBLIC BROKER FUNCTIONS ---

	/**
	 * Switch the console to REPL mode
	 */
	public void repl() {
		Repl repl = components.repl();
		if (repl != null) {
			repl.setEnabled(true);
		}
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
	public <T extends Service> T createService(T service) throws Exception {
		return createService(service, new Tree());
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
		if (registry == null) {

			// Start service later
			services.put(service, config);
		} else {

			// Start service now
			registry.addActions(service, config);
		}
		return service;
	}

	/**
	 * Destroys a local service
	 * 
	 * @param service
	 */
	public void destroyService(Service service) {
	}

	/**
	 * Returns a local service by name
	 * 
	 * @param serviceName
	 * @return
	 */
	public Service getLocalService(String serviceName) {
		return registry.getService(serviceName);
	}

	/**
	 * Returns an action by name
	 * 
	 * @param actionName
	 * @return
	 */
	public ActionEndpoint getAction(String actionName) {
		return registry.getAction(actionName, null);
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
	 * Calls an action (local or remote). Sample code:<br>
	 * <br>
	 * Promise promise = broker.call("math.add", "a", 1, "b", 2);<br>
	 * <br>
	 * ...or with CallingOptions:<br>
	 * <br>
	 * broker.call("math.add", "a", 1, "b", 2, new CallingOptions("node2"));
	 */
	public Promise call(String name, Object... params) {
		ParseResult res = parseParams(params);
		String targetID = res.opts == null ? null : res.opts.nodeID;
		return registry.getAction(name, targetID).call(res.data, res.opts, (Context) null);
	}

	public Promise call(String name, Tree params) {
		return registry.getAction(name, null).call(params, (CallingOptions.Options) null, (Context) null);
	}

	public Promise call(String name, Tree params, CallingOptions.Options opts) {
		String targetID = opts == null ? null : opts.nodeID;
		return registry.getAction(name, targetID).call(params, opts, (Context) null);
	}

	// --- EMIT EVENT TO EVENT GROUP ---

	/**
	 * Emits an event (grouped & balanced global event)
	 */
	public void emit(String name, Object... params) {
		ParseResult res = parseParams(params);
		eventbus.emit(name, res.data, res.groups, false);
	}
	
	/**
	 * Emits an event (grouped & balanced global event)
	 */
	public void emit(String name, Tree payload, Groups groups) {
		eventbus.emit(name, payload, groups, false);
	}

	/**
	 * Emits an event (grouped & balanced global event)
	 */
	public void emit(String name, Tree payload) {
		eventbus.emit(name, payload, null, false);
	}
	
	// --- BROADCAST EVENT TO ALL LISTENERS ---
	
	/**
	 * Emits an event for all local & remote services
	 */
	public void broadcast(String name, Object... params) {
		ParseResult res = parseParams(params);
		eventbus.broadcast(name, res.data, res.groups, false);
	}
	
	/**
	 * Emits an event for all local & remote services
	 */
	public void broadcast(String name, Tree payload, Groups groups) {
		eventbus.broadcast(name, payload, groups, false);
	}

	/**
	 * Emits an event for all local & remote services
	 */
	public void broadcast(String name, Tree payload) {
		eventbus.broadcast(name, payload, null, false);
	}
	
	// --- BROADCAST EVENT TO LOCAL LISTENERS ---
	
	/**
	 * Emits an event for all local services.
	 */
	public void broadcastLocal(String name, Object... params) {
		ParseResult res = parseParams(params);
		eventbus.broadcast(name, res.data, res.groups, true);
	}
	
	/**
	 * Emits an event for all local services.
	 */
	public void broadcastLocal(String name, Tree payload, Groups groups) {
		eventbus.broadcast(name, payload, groups, true);
	}

	/**
	 * Emits an event for all local services.
	 */
	public void broadcastLocal(String name, Tree payload) {
		eventbus.broadcast(name, payload, null, true);
	}
	
}