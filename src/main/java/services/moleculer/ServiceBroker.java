/**
 * This software is licensed under MIT license.<br>
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
import services.moleculer.service.ActionContainer;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.transporter.Transporter;

/**
 * Service Broker.
 */
@Name("Service Broker")
public final class ServiceBroker {

	// --- VERSIONS ---

	/**
	 * Version of ServiceBroker.
	 */
	public static final double IMPLEMENTATION_VERSION = 1.2;

	/**
	 * Version of the implemented Moleculer API.
	 */
	public static final int MOLECULER_VERSION = 2;

	// --- LOGGER ---

	/**
	 * SLF4J logger of this class.
	 */
	protected final static Logger logger = LoggerFactory.getLogger(ServiceBroker.class);

	// --- UNIQUE NODE IDENTIFIER ---

	/**
	 * Unique server ID (eg. "node1", "server-2", etc.)
	 */
	private final String nodeID;

	// --- CONFIGURATIONS ---

	/**
	 * Configuration (created by the {@link ServiceBrokerBuilder}).
	 */
	private final ServiceBrokerSettings settings;

	/**
	 * Optional configuration (loaded from JSON/YAML/TOML/XML file).
	 */
	private final Tree config;

	// --- INERNAL AND USER-DEFINED COMPONENTS ---

	/**
	 * Component registry of the Service Broker instance. ComponentRegistry has
	 * similar functionality to Spring's ApplicationContext; stores "beans"
	 * (MoleculerComponents), and by using the method "get(id)" you can retrieve
	 * instances of your component.
	 */
	private final ComponentRegistry components;

	/**
	 * Registry of local and remote Moleculer Services.
	 */
	private ServiceRegistry registry;

	// --- SERVICES AND CONFIGURATIONS ---

	/**
	 * Services which defined and added to the Broker before the boot process.
	 */
	private final LinkedHashMap<Service, Tree> services = new LinkedHashMap<>();

	// --- STATIC SERVICE BROKER BUILDER ---

	/**
	 * Creates a new {@link ServiceBrokerBuilder} instance.
	 * 
	 * @return builder instance
	 */
	public static final ServiceBrokerBuilder builder() {
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
	}

	// --- GET NODE ID ---

	public String nodeID() {
		return nodeID;
	}

	// --- GET COMPONENT REGISTRY ---

	public final ComponentRegistry components() {
		return components;
	}

	// --- START BROKER INSTANCE ---

	/**
	 * Start broker. If has transporter, transporter.connect will be called.
	 */
	public final void start() {

		// Check state
		if (registry != null) {
			throw new IllegalStateException("Moleculer Service Broker has already been started!");
		}
		try {

			// Start internal and custom components
			logger.info("Starting Moleculer Service Broker (version " + IMPLEMENTATION_VERSION + ")...");
			String name = nameOf(components, true);
			logger.info("Using " + name + " to load service classes.");
			components.start(this, settings, config);
			logger.info("Node \"" + nodeID + "\" started successfully.");

			// Set the pointers of frequently used components
			registry = components.registry();

			// Register and start pending services
			for (Map.Entry<Service, Tree> entry : services.entrySet()) {
				Service service = entry.getKey();
				Tree cfg = entry.getValue();
				if (cfg == null) {
					cfg = new Tree();
				}
				registry.addService(service, cfg);
			}

			// All components and services started successfully
			services.clear();

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
	public final void stop() {
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
			registry.addService(service, config);
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
	public ActionContainer getAction(String actionName) {
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
		Tree tree = null;
		Context parent = null;
		CallingOptions opts = null;
		if (params != null) {
			if (params.length == 1) {
				if (params[0] instanceof Tree) {
					tree = (Tree) params[0];
				} else {
					tree = new Tree().setObject(params[0]);
				}
			} else {
				LinkedHashMap<String, Object> map = new LinkedHashMap<>();
				String prev = null;
				Object value;
				for (int i = 0; i < params.length; i++) {
					value = params[i];
					if (prev == null) {
						if (!(value instanceof String)) {
							if (value instanceof CallingOptions) {
								opts = (CallingOptions) value;
								continue;
							}
							if (value instanceof Context) {
								parent = (Context) value;
								continue;
							}
							i++;
							throw new IllegalArgumentException("Parameter #" + i + " (\"" + value
									+ "\") must be String, Context, or CallingOptions!");
						}
						prev = (String) value;
						continue;
					}
					map.put(prev, value);
					prev = null;
				}
				tree = new Tree(map);
			}
		}
		String targetID = opts == null ? null : opts.nodeID();
		return registry.getAction(name, targetID).call(tree, opts, parent);
	}

	public Promise call(String name, Tree params) {
		return registry.getAction(name, null).call(params, (CallingOptions) null, (Context) null);
	}

	public Promise call(String name, Tree params, CallingOptions opts) {
		String targetID = opts == null ? null : opts.nodeID();
		return registry.getAction(name, targetID).call(params, opts, (Context) null);
	}

	// --- EMIT EVENTS VIA EVENT BUS ---

	/**
	 * Emits an event (grouped & balanced global event)
	 */
	public void emit(String name, Object payload, String... groups) {
	}

	/**
	 * Emits an event for all local & remote services
	 */
	public void broadcast(String name, Object payload) {
	}

	/**
	 * Emits an event for all local services
	 */
	public void broadcastLocal(String name, Object payload) {
	}

}