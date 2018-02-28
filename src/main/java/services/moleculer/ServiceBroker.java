package services.moleculer;

import static services.moleculer.util.CommonUtils.nameOf;
import static services.moleculer.util.CommonUtils.parseParams;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.cacher.Cacher;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.CallingOptions;
import services.moleculer.context.Context;
import services.moleculer.context.ContextFactory;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.eventbus.Groups;
import services.moleculer.internal.NodeService;
import services.moleculer.repl.Repl;
import services.moleculer.service.Action;
import services.moleculer.service.Middleware;
import services.moleculer.service.Service;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.transporter.Transporter;
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
	protected final LinkedHashSet<Service> services = new LinkedHashSet<>();

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
	protected Cacher cacher;
	protected ServiceRegistry serviceRegistry;
	protected Transporter transporter;
	protected Repl repl;
	
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
			cacher = start(config.getCacher());
			serviceRegistry = start(config.getServiceRegistry());
			transporter = start(config.getTransporter());

			// Register enqued middlewares
			if (cacher != null) {
				middlewares.add(cacher);
			}
			serviceRegistry.use(middlewares);

			// Install internal services
			if (config.isInternalServices()) {
				services.add(new NodeService());
			}
			
			// Register and start enqued services and listeners
			for (Service service : services) {

				// Register actions
				serviceRegistry.addActions(service);

				// Register listeners
				eventbus.addListeners(service);
			}

			// Start transporter's connection loop
			if (transporter != null) {
				transporter.connect();
			}

			logger.info("Node \"" + getNodeID() + "\" started successfully.");

			// Start repl console
			repl = start(config.getRepl());
			
		} catch (Throwable cause) {
			logger.error("Moleculer Service Broker could not be started!", cause);
			stop();
		} finally {
			middlewares.clear();
			services.clear();
		}
	}

	protected <TYPE extends Service> TYPE start(TYPE component) throws Exception {
		if (component == null) {
			return null;
		}
		component.started(this);
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
		stop(repl);
		stop(serviceRegistry);
		stop(eventbus);
		stop(contextFactory);
		stop(strategyFactory);
		stop(uidGenerator);
	}

	protected void stop(Service component) {
		if (component == null) {
			return;
		}
		try {
			component.stopped();
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
		if (serviceRegistry == null) {

			// Start service later
			services.add(service);
		} else {

			// Start service now
			serviceRegistry.addActions(service);
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

	public void use(Collection<Middleware> middlewares) {
		if (serviceRegistry == null) {

			// Apply middlewares later
			this.middlewares.addAll(middlewares);
		} else {

			// Apply middlewares now
			serviceRegistry.use(middlewares);
		}
	}
	
	public void use(Middleware... middlewares) {
		use(Arrays.asList(middlewares));
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
		return new Promise(result -> {
			try {
				String targetID = opts == null ? null : opts.nodeID;
				Action action = serviceRegistry.getAction(name, targetID);
				Context ctx = contextFactory.create(name, params, opts, null);
				result.resolve(action.handler(ctx));
			} catch (Throwable cause) {
				result.reject(cause);
			}
		});
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