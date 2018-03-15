/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.cacher.Cacher;
import services.moleculer.config.ServiceBrokerBuilder;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.CallOptions;
import services.moleculer.context.Context;
import services.moleculer.context.ContextFactory;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.eventbus.Groups;
import services.moleculer.internal.NodeService;
import services.moleculer.service.Action;
import services.moleculer.service.Middleware;
import services.moleculer.service.MoleculerComponent;
import services.moleculer.service.Service;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.transporter.Transporter;
import services.moleculer.uid.UIDGenerator;
import services.moleculer.util.ParseResult;

/**
 * The ServiceBroker is the main component of Moleculer. It handles services &
 * events, calls actions and communicates with remote nodes. You need to create
 * an instance of ServiceBroker for every node. Sample of usage:<br>
 *
 * <pre>
 * ServiceBroker broker = new ServiceBroker("node-1");
 *
 * broker.createService(new Service("math") {
 * 	public Action add = ctx -> {
 * 		return ctx.params.get("a").asInteger() + ctx.params.get("b").asInteger();
 * 	};
 * });
 *
 * broker.start();
 *
 * broker.call("math.add", "a", 5, "b", 3).then(rsp -> {
 * 	broker.getLogger().info("Response: " + rsp.asInteger());
 * });
 * </pre>
 *
 * This project is based on the idea of Moleculer Microservices Framework for
 * NodeJS (https://moleculer.services). Special thanks to the Moleculer's
 * project owner (https://github.com/icebob) for the consultations.
 */
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
	protected Cacher cacher;
	protected ServiceRegistry serviceRegistry;
	protected Transporter transporter;

	// --- STATIC SERVICE BROKER BUILDER ---

	/**
	 * Creates a new {@link ServiceBrokerBuilder} instance. Sample of usage:<br>
	 * <br>
	 * ServiceBroker broker = ServiceBroker.builder().cacher(cacher).build();
	 *
	 * @return builder instance
	 */
	public static ServiceBrokerBuilder builder() {
		return new ServiceBrokerBuilder();
	}

	// --- CONSTRUCTORS ---

	public ServiceBroker(ServiceBrokerConfig config) {
		this.config = config;
	}

	public ServiceBroker() {
		this(null, null, null);
	}

	public ServiceBroker(String nodeID) {
		this(nodeID, null, null);
	}

	public ServiceBroker(String nodeID, Cacher cacher, Transporter transporter) {
		this(new ServiceBrokerConfig(nodeID, cacher, transporter));
	}

	// --- GET CONFIGURATION ---

	public ServiceBrokerConfig getConfig() {
		return config;
	}

	// --- PROPERTY GETTERS ---

	public String getNodeID() {
		return config.getNodeID();
	}

	// --- START BROKER INSTANCE ---

	/**
	 * Start broker. If has a Transporter, transporter.connect() will be called.
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
				services.put("$node", new NodeService());
			}

			// Register and start enqued services and listeners
			for (Map.Entry<String, Service> entry : services.entrySet()) {
				Service service = entry.getValue();

				// Register actions
				serviceRegistry.addActions(entry.getKey(), service);

				// Register listeners
				eventbus.addListeners(service);
			}

			// Start transporter's connection loop
			if (transporter != null) {
				transporter.connect();
			}

			// Ok, services, transporter and gateway started
			logger.info("Node \"" + config.getNodeID() + "\" started successfully.");

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
		createService(service.getName(), service);
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
	 * ...or with CallOptions:<br>
	 * <br>
	 * broker.call("math.add", "a", 1, "b", 2, CallOptions.nodeID("node2"));
	 */
	public Promise call(String name, Object... params) {
		ParseResult res = parseParams(params);
		return call(name, res.data, res.opts);
	}

	public Promise call(String name, Tree params) {
		return call(name, params, null);
	}

	public Promise call(String name, Tree params, CallOptions.Options opts) {
		String targetID;
		int retryCount;
		if (opts == null) {
			targetID = null;
			retryCount = 0;
		} else {
			targetID = opts.nodeID;
			retryCount = opts.retryCount;
		}
		return call(name, params, opts, targetID, retryCount);
	}

	protected Promise call(String name, Tree params, CallOptions.Options opts, String targetID, int retryCount) {
		try {
			Action action = serviceRegistry.getAction(name, targetID);
			Context ctx = contextFactory.create(name, params, opts, null);
			return Promise.resolve(action.handler(ctx)).catchError(cause -> {
				return retry(cause, name, params, opts, targetID, retryCount);
			});
		} catch (Throwable cause) {
			return retry(cause, name, params, opts, targetID, retryCount);
		}
	}

	protected Promise retry(Throwable cause, String name, Tree params, CallOptions.Options opts, String targetID,
			int retryCount) {
		if (retryCount > 0) {
			int remaining = retryCount - 1;
			logger.warn("Retrying request (" + remaining + " attempts left)...", cause);
			return call(name, params, opts, targetID, remaining);
		}
		return Promise.reject(cause);
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

	// --- WAIT FOR SERVICE(S) ---

	public Promise waitForServices(String... services) {
		return waitForServices(0, services);
	}

	public Promise waitForServices(int timeout, String... services) {
		return waitForServices(timeout, Arrays.asList(services));
	}

	public Promise waitForServices(int timeout, Collection<String> services) {
		return serviceRegistry.waitForServices(timeout, services);
	}

	// --- START DEVELOPER CONSOLE ---

	public boolean repl() {
		return repl(true);
	}

	public boolean repl(boolean local) {
		try {
			String className = local ? "Local" : "Remote";
			String serviceName = className.toLowerCase() + "-repl";
			try {
				serviceRegistry.getService(serviceName);
				return false;
			} catch (Exception ignored) {
			}
			createService(serviceName,
					(Service) Class.forName("services.moleculer.repl." + className + "Repl").newInstance());
			return true;
		} catch (Exception cause) {
			logger.error("REPL package not installed!");
		}
		return false;
	}

}