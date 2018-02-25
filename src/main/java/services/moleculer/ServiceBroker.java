package services.moleculer;

import static services.moleculer.util.CommonUtils.nameOf;
import static services.moleculer.util.CommonUtils.parseParams;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.CallingOptions;
import services.moleculer.context.Context;
import services.moleculer.context.ContextFactory;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.service.Action;
import services.moleculer.service.Middleware;
import services.moleculer.service.Service;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.uid.UIDGenerator;
import services.moleculer.util.ParseResult;

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
	protected static final Logger logger = LoggerFactory.getLogger(ServiceBroker.class);

	// --- CONFIGURATION ---

	protected final ServiceBrokerConfig config;

	// --- ENQUED SERVICES ---

	/**
	 * Services which defined and added to the Broker before the boot process.
	 */
	protected final LinkedHashMap<String, Service> services = new LinkedHashMap<>();

	// --- ENQUED MIDDLEWARES ---

	/**
	 * Middlewares which defined and added to the Broker before the boot
	 * process.
	 */
	protected final LinkedHashSet<Middleware> middlewares = new LinkedHashSet<>();

	// --- INTERNAL COMPONENTS ---

	protected UIDGenerator uidGenerator;
	protected StrategyFactory strategyFactory;
	protected ContextFactory contextFactory;
	protected Eventbus eventbus;
	protected ServiceRegistry serviceRegistry;

	// --- CONSTRUCTORS ---

	public ServiceBroker(ServiceBrokerConfig config) {
		this.config = config;
	}

	// --- GET CONFIGURATION ---

	public ServiceBrokerConfig getConfig() {
		return config;
	}

	// --- PROPERTY GETTERS ---

	public String getNodeID() {
		return getConfig().getNodeID();
	}

	// --- START BROKER INSTANCE ---

	/**
	 * Start broker. If has transporter, transporter.connect will be called.
	 */
	public void start() throws Exception {

		// Check state
		if (serviceRegistry != null) {
			throw new IllegalStateException("Moleculer Service Broker has already been started!");
		}
		try {

			// Start internal components, services, middlewares...
			logger.info("Starting Moleculer Service Broker (version " + SOFTWARE_VERSION + ")...");

			// Set internal components
			uidGenerator = start(config.getUidGenerator());
			strategyFactory = start(config.getStrategyFactory());
			contextFactory = start(config.getContextFactory());
			eventbus = start(config.getEventbus());
			serviceRegistry = start(config.getServiceRegistry());

			// Register enqued middlewares
			serviceRegistry.use(middlewares);

			// Register and start enqued services and listeners
			for (Map.Entry<String, Service> entry : services.entrySet()) {
				String name = entry.getKey();
				Service service = entry.getValue();

				// Register actions
				serviceRegistry.addActions(name, service);

				// Register listeners
				// eventbus.addListeners(name, service);
			}

			// Start transporter's connection loop
			// Transporter transporter = components.transporter();
			// if (transporter != null) {
			// transporter.connect();
			// }

			logger.info("Node \"" + getNodeID() + "\" started successfully.");

		} catch (Throwable cause) {
			logger.error("Moleculer Service Broker could not be started!", cause);
			stop();
		} finally {
			middlewares.clear();
			services.clear();
		}
	}

	protected <TYPE extends MoleculerComponent> TYPE start(TYPE component) throws Exception {
		if (component == null) {
			return null;
		}
		component.start(this);
		logger.info(nameOf(component, true) + " started.");
		return component;
	}

	// --- STOP BROKER INSTANCE ---

	/**
	 * Stop broker. If the Broker has a Transporter, transporter.disconnect will
	 * be called.
	 */
	public void stop() {

		// Stop internal components
		stop(serviceRegistry);
		stop(eventbus);
		stop(contextFactory);
		stop(strategyFactory);
		stop(uidGenerator);
	}

	protected void stop(MoleculerComponent component) {
		if (component == null) {
			return;
		}
		try {
			component.stop();
			logger.info(nameOf(component, true) + " stopped.");
		} catch (Exception cause) {
			logger.warn("Unable to stop component!", cause);
		}
	}

	// --- LOGGING ---
	
	public Logger getLogger() {
		return logger;
	}
	
	public Logger getLogger(Class<?> clazz) {
		return LoggerFactory.getLogger(clazz);
	}

	public Logger getLogger(String name) {
		return LoggerFactory.getLogger(name);
	}
	
	// --- ADD LOCAL SERVICE ---

	public void createService(Service service) {
		createService(nameOf(service, false), service);
	}

	public void createService(String name, Service service) {
		if (serviceRegistry == null) {

			// Start service later
			services.put(name, service);
		} else {

			// Start service now
			serviceRegistry.addActions(name, service);
		}
	}

	// --- GET LOCAL SERVICE ---

	/**
	 * Returns a local service by name
	 * 
	 * @param serviceName
	 * @return
	 */
	public Service getLocalService(String serviceName) {
		return serviceRegistry.getService(serviceName);
	}

	// --- ADD MIDDLEWARE ---

	public void use(Middleware... middlewares) {
		if (serviceRegistry == null) {

			// Apply middlewares later
			for (Middleware middleware: middlewares) {
				this.middlewares.add(middleware);
			}
		} else {

			// Apply middlewares now
			serviceRegistry.use(Arrays.asList(middlewares));
		}
	}

	// --- GET LOCAL OR REMOTE ACTION ---

	/**
	 * Returns an action by name
	 * 
	 * @param actionName
	 * @return
	 */
	public Action getAction(String actionName) {
		return serviceRegistry.getAction(actionName, null);
	}

	/**
	 * Returns an action by name
	 * 
	 * @param actionName
	 * @param nodeID
	 * @return
	 */
	public Action getAction(String actionName, String nodeID) {
		return serviceRegistry.getAction(actionName, nodeID);
	}

	// --- INVOKE LOCAL OR REMOTE ACTION ---

	/**
	 * Calls an action (local or remote). Sample code:<br>
	 * <br>
	 * Promise promise = broker.call("math.add", "a", 1, "b", 2);<br>
	 * <br>
	 * ...or with CallingOptions:<br>
	 * <br>
	 * broker.call("math.add", "a", 1, "b", 2, CallingOptions.nodeID("node2"));
	 */
	public Promise call(String name, Object... params) {
		ParseResult res = parseParams(params);
		return call(name, res.data, res.opts);
	}

	public Promise call(String name, Tree params) {
		return call(name, params, null);
	}

	public Promise call(String name, Tree params, CallingOptions.Options opts) {
		return new Promise(init -> {
			try {
				String targetID = opts == null ? null : opts.nodeID;
				Action action = serviceRegistry.getAction(name, targetID);
				Context ctx = contextFactory.create(name, params, opts, null);				
				init.resolve(action.handler(ctx));
			} catch (Throwable cause) {
				init.reject(cause);
			}			
		});
	}

}