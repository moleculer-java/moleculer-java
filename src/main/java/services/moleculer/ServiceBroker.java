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
import static services.moleculer.util.CommonUtils.suggestDependency;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Promise;
import io.datatree.Tree;
import io.datatree.dom.TreeReader;
import io.datatree.dom.TreeReaderRegistry;
import io.datatree.dom.TreeWriter;
import io.datatree.dom.TreeWriterRegistry;
import services.moleculer.breaker.CircuitBreaker;
import services.moleculer.cacher.Cacher;
import services.moleculer.config.ServiceBrokerBuilder;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.CallOptions;
import services.moleculer.context.ContextFactory;
import services.moleculer.context.DefaultContextFactory;
import services.moleculer.error.MoleculerServerError;
import services.moleculer.eventbus.DefaultEventbus;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.eventbus.Groups;
import services.moleculer.internal.NodeService;
import services.moleculer.service.Action;
import services.moleculer.service.DefaultServiceInvoker;
import services.moleculer.service.DefaultServiceRegistry;
import services.moleculer.service.Middleware;
import services.moleculer.service.MoleculerComponent;
import services.moleculer.service.Service;
import services.moleculer.service.ServiceInvoker;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.strategy.CpuUsageStrategyFactory;
import services.moleculer.strategy.NanoSecRandomStrategyFactory;
import services.moleculer.strategy.NetworkLatencyStrategyFactory;
import services.moleculer.strategy.RoundRobinStrategyFactory;
import services.moleculer.strategy.SecureRandomStrategyFactory;
import services.moleculer.strategy.Strategy;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.strategy.XorShiftRandomStrategyFactory;
import services.moleculer.stream.PacketStream;
import services.moleculer.transporter.AmqpTransporter;
import services.moleculer.transporter.GoogleTransporter;
import services.moleculer.transporter.JmsTransporter;
import services.moleculer.transporter.KafkaTransporter;
import services.moleculer.transporter.MqttTransporter;
import services.moleculer.transporter.NatsTransporter;
import services.moleculer.transporter.RedisTransporter;
import services.moleculer.transporter.TcpTransporter;
import services.moleculer.transporter.Transporter;
import services.moleculer.uid.IncrementalUidGenerator;
import services.moleculer.uid.StandardUidGenerator;
import services.moleculer.uid.UidGenerator;
import services.moleculer.util.ParseResult;

/**
 * The ServiceBroker is the main component of Moleculer. It handles services
 * &amp; events, calls actions and communicates with remote nodes. You need to
 * create an instance of ServiceBroker for every node. Features of Moleculer:
 * <ul>
 * <li>Fast - High-performance and non-blocking
 * <li>Polyglot - Moleculer is implemented under Node.js and Java
 * <li>Extensible - All built-in modules (caching, serializer, transporter) are
 * pluggable
 * <li>Open source - Moleculer is 100% open source and free of charge
 * <li>Fault tolerant - With built-in load balancer &amp; circuit breaker
 * </ul>
 * Sample of usage:<br>
 *
 * <pre>
 * ServiceBroker broker = new ServiceBroker("node-1");
 *
 * broker.createService(new Service("math") {
 * 	public Action add = ctx -&gt; {
 * 		return ctx.params.get("a").asInteger() + ctx.params.get("b").asInteger();
 * 	};
 * });
 *
 * broker.start();
 *
 * broker.call("math.add", "a", 5, "b", 3).then(rsp -&gt; {
 * 	broker.getLogger().info("Response: " + rsp.asInteger());
 * });
 * </pre>
 *
 * This project is based on the idea of Moleculer Microservices Framework for
 * Node.js (https://moleculer.services). Special thanks to the Moleculer's
 * project owner (https://github.com/icebob) for the consultations.
 */
public class ServiceBroker {

	// --- VERSIONS ---

	/**
	 * Version of the Java ServiceBroker API.
	 */
	public static final String SOFTWARE_VERSION = "1.0.5";

	/**
	 * Version of the implemented Moleculer Protocol.
	 */
	public static final String PROTOCOL_VERSION = "3";

	// --- LOGGER ---

	/**
	 * SLF4J logger of this class.
	 */
	protected static final Logger logger = LoggerFactory.getLogger(ServiceBroker.class);

	// --- CURRENT NODE ID ---

	/**
	 * Unique ID of this node (~= ServiceBroker instance).
	 */
	protected final String nodeID;

	// --- CONFIGURATION ---

	/**
	 * Configuration settings and internal components (event bus, cacher,
	 * service registry, etc.) of this node / broker. Use the
	 * {@link #getConfig() getConfig} method to access this object.
	 */
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

	/**
	 * UID generator (each {@link services.moleculer.context.Context Context}
	 * instance has its own identifier). Use
	 * <code>getConfig().getUidGenerator()</code> to access this instance.
	 * 
	 * @see IncrementalUidGenerator
	 * @see StandardUidGenerator
	 */
	protected UidGenerator uidGenerator;

	/**
	 * Default (round-robin) service invocation factory. Use
	 * <code>getConfig().getStrategyFactory()</code> to access this instance.
	 * 
	 * @see RoundRobinStrategyFactory
	 * @see NanoSecRandomStrategyFactory
	 * @see SecureRandomStrategyFactory
	 * @see XorShiftRandomStrategyFactory
	 * @see CpuUsageStrategyFactory
	 * @see NetworkLatencyStrategyFactory
	 */
	protected StrategyFactory strategyFactory;

	/**
	 * Context generator / factory. Use
	 * <code>getConfig().getContextFactory()</code> to access this instance.
	 * 
	 * @see DefaultContextFactory
	 */
	protected ContextFactory contextFactory;

	/**
	 * Service invoker. Use <code>getConfig().getServiceInvoker()</code> to
	 * access this instance.
	 * 
	 * @see DefaultServiceInvoker
	 * @see CircuitBreaker
	 */
	protected ServiceInvoker serviceInvoker;

	/**
	 * Implementation of the event bus of the current node. Use
	 * <code>getConfig().getEventbus()</code> to access this instance.
	 * 
	 * @see DefaultEventbus
	 * @see #broadcast(String, Object...)
	 * @see #emit(String, Object...)
	 */
	protected Eventbus eventbus;

	/**
	 * Implementation of the service registry of the current node. Use
	 * <code>getConfig().getServiceRegistry()</code> to access this instance.
	 * 
	 * @see DefaultServiceRegistry
	 * @see #call(String, Object...)
	 */
	protected ServiceRegistry serviceRegistry;

	/**
	 * Implementation of the Transporter. Use
	 * <code>getConfig().getTransporter()</code> to access this instance. Can be
	 * <code>null</code>.
	 * 
	 * @see TcpTransporter
	 * @see RedisTransporter
	 * @see NatsTransporter
	 * @see MqttTransporter
	 * @see JmsTransporter
	 * @see GoogleTransporter
	 * @see KafkaTransporter
	 * @see AmqpTransporter
	 */
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

	/**
	 * Creates a new ServiceBroker by the specified {@link ServiceBrokerConfig
	 * configuration}.
	 * 
	 * @param config
	 *            configuration of the Broker
	 */
	public ServiceBroker(ServiceBrokerConfig config) {
		this.config = config;
		this.nodeID = config.getNodeID();
	}

	/**
	 * Creates a new ServiceBroker without {@link Transporter}. The
	 * {@link #nodeID} is generated from the host name and process ID.
	 */
	public ServiceBroker() {
		this(null, null, null);
	}

	/**
	 * Creates a new ServiceBroker without {@link Transporter} and with the
	 * specified {@link #nodeID}.
	 * 
	 * @param nodeID
	 *            the unique {@link #nodeID} of this node
	 */
	public ServiceBroker(String nodeID) {
		this(nodeID, null, null);
	}

	/**
	 * Creates a new ServiceBroker with the specified {@link #nodeID},
	 * {@link Cacher}, and {@link Transporter}.
	 * 
	 * @param nodeID
	 *            the unique {@link #nodeID} of this node
	 * @param cacher
	 *            {@link Cacher} of this broker instance
	 * @param transporter
	 *            {@link Transporter} of this broker instance
	 */
	public ServiceBroker(String nodeID, Cacher cacher, Transporter transporter) {
		this(new ServiceBrokerConfig(nodeID, cacher, transporter));
	}

	// --- GET CONFIGURATION ---

	/**
	 * Returns the configuration settings and internal components (event bus,
	 * cacher, service registry, etc.) of this node / broker.
	 *
	 * @return configuration container
	 */
	public ServiceBrokerConfig getConfig() {
		return config;
	}

	// --- PROPERTY GETTERS ---

	/**
	 * Returns the unique {@link #nodeID} of this node (~= ServiceBroker
	 * instance).
	 * 
	 * @return unique {@link #nodeID}
	 */
	public String getNodeID() {
		return nodeID;
	}

	// --- START BROKER INSTANCE ---

	/**
	 * Start broker. If has a Transporter, transporter.connect() will be called.
	 * 
	 * @throws Exception
	 *             fatal error (missing classes or JARs, used port, etc.)
	 */
	public void start() throws Exception {

		// Check state
		if (serviceRegistry != null) {
			throw new MoleculerServerError("Moleculer Service Broker has already been started!", nodeID,
					"ALREADY_STARTED");
		}
		try {

			// Start internal components, services, middlewares...
			logger.info("Starting Moleculer Service Broker (version " + SOFTWARE_VERSION + ")...");

			// Set global JSON reader API (Jackson, Gson, Boon, FastJson, etc.)
			String readerList = config.getJsonReaders();
			if (readerList != null) {
				String[] readers = readerList.split(",");
				Set<String> supportedReaders = TreeReaderRegistry.getReadersByFormat("json");
				TreeReader selectedReader = null;
				for (String reader : readers) {
					reader = reader.trim().toLowerCase();
					if (!reader.isEmpty()) {
						for (String supportedReader : supportedReaders) {
							int i = supportedReader.lastIndexOf('.');
							if (i > -1) {
								supportedReader = supportedReader.substring(i + 1);
							}
							if (supportedReader.toLowerCase().contains(reader)) {
								selectedReader = TreeReaderRegistry.getReader(supportedReader);
								logger.info(
										"Default JSON deserializer/reader is \"" + selectedReader.getClass() + "\".");
								TreeReaderRegistry.setReader("json", selectedReader);
								break;
							}
						}
						if (selectedReader != null) {
							break;
						}
					}
				}
			}

			// Set global JSON writer API (Jackson, Gson, Boon, FastJson, etc.)
			String writerList = config.getJsonWriters();
			if (writerList != null) {
				String[] writers = writerList.split(",");
				Set<String> supportedWriters = TreeWriterRegistry.getWritersByFormat("json");
				TreeWriter selectedWriter = null;
				for (String writer : writers) {
					writer = writer.trim().toLowerCase();
					if (!writer.isEmpty()) {
						for (String supportedWriter : supportedWriters) {
							int i = supportedWriter.lastIndexOf('.');
							if (i > -1) {
								supportedWriter = supportedWriter.substring(i + 1);
							}
							if (supportedWriter.toLowerCase().contains(writer)) {
								selectedWriter = TreeWriterRegistry.getWriter(supportedWriter);
								logger.info("Default JSON serializer/writer is \"" + selectedWriter.getClass() + "\".");
								TreeWriterRegistry.setWriter("json", selectedWriter);
								break;
							}
						}
						if (selectedWriter != null) {
							break;
						}
					}
				}
			}

			// Set internal components
			uidGenerator = start(config.getUidGenerator());
			strategyFactory = start(config.getStrategyFactory());
			contextFactory = start(config.getContextFactory());
			serviceInvoker = start(config.getServiceInvoker());
			eventbus = start(config.getEventbus());
			serviceRegistry = start(config.getServiceRegistry());
			transporter = start(config.getTransporter());

			// Register enqued middlewares
			Cacher cacher = config.getCacher();
			if (cacher != null) {
				logger.info(nameOf(cacher, true) + " started.");
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
				String serviceName = entry.getKey();
				eventbus.addListeners(serviceName, service);
				serviceRegistry.addActions(serviceName, service);
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

	/**
	 * Starts the specified {@link MoleculerComponent}.
	 * 
	 * @param component
	 *            component to start
	 * @param <TYPE>
	 *            Moleculer component (service registry, transporter, etc.)
	 * 
	 * @return the started component
	 * 
	 * @throws Exception
	 *             any configuration or I/O exceptions
	 */
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
	 * Stop broker and all internal components (event bus, context factory,
	 * etc.). If the Broker has a Transporter, transporter.disconnect() will be
	 * called.
	 */
	public void stop() {

		// Stop internal components
		stop(transporter);
		stop(serviceRegistry);
		stop(eventbus);
		stop(serviceInvoker);
		stop(contextFactory);
		stop(strategyFactory);
		stop(uidGenerator);

		// Shutdown thread pools
		if (config.isShutDownThreadPools()) {
			ExecutorService executor = config.getExecutor();
			if (executor != null && !executor.isShutdown() && !executor.isTerminated()) {
				try {
					executor.shutdownNow();
				} catch (Exception ignored) {
				}
			}
			logger.info("Task Executor Service stopped.");
			ScheduledExecutorService scheduler = config.getScheduler();
			if (scheduler != null && !scheduler.isShutdown() && !scheduler.isTerminated()) {
				try {
					scheduler.shutdownNow();
				} catch (Exception ignored) {
				}
			}
			logger.info("Task Scheduler Service stopped.");
		}
	}

	/**
	 * Stops the specified {@link MoleculerComponent}.
	 * 
	 * @param component
	 *            component to stop
	 */
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

	/**
	 * Returns the SLF4J logger of this broker instance.
	 * 
	 * @return logger instance
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Returns a logger named corresponding to the class passed as parameter.
	 * 
	 * @param clazz
	 *            the returned logger will be named after clazz
	 * 
	 * @return logger instance
	 */
	public Logger getLogger(Class<?> clazz) {
		return LoggerFactory.getLogger(clazz);
	}

	/**
	 * Return a logger named according to the name parameter.
	 * 
	 * @param name
	 *            the name of the logger
	 * 
	 * @return logger instance
	 */
	public Logger getLogger(String name) {
		return LoggerFactory.getLogger(name);
	}

	// --- ADD LOCAL SERVICE ---

	/**
	 * Installs a new service instance and notifies other nodes about the
	 * actions/listeners of the new service.
	 * 
	 * @param service
	 *            the new service instance
	 */
	public void createService(Service service) {
		createService(service.getName(), service);
	}

	/**
	 * Installs a new service with the specified name (eg. "user" service) and
	 * notifies other nodes about the actions/listeners of this new service.
	 * 
	 * @param name
	 *            custom service name (eg. "user", "logger", "configurator",
	 *            etc.)
	 * @param service
	 *            the new service instance
	 */
	public void createService(String name, Service service) {
		if (serviceRegistry == null) {

			// Start service later
			services.put(name, service);
		} else {

			// Register and start service now
			eventbus.addListeners(name, service);
			serviceRegistry.addActions(name, service);
		}
	}

	// --- GET LOCAL SERVICE ---

	/**
	 * Returns a local service by name (eg. "user" service).
	 *
	 * @param serviceName
	 *            service name (eg. "user", "logger", "configurator", etc.)
	 * 
	 * @return local service instance
	 * 
	 * @throws NoSuchElementException
	 *             if the service name is not valid
	 */
	public Service getLocalService(String serviceName) {
		return serviceRegistry.getService(serviceName);
	}

	// --- ADD MIDDLEWARE ---

	/**
	 * Installs a collection of middlewares.
	 * 
	 * @param middlewares
	 *            collection of middlewares
	 */
	public void use(Collection<Middleware> middlewares) {
		if (serviceRegistry == null) {

			// Apply middlewares later
			this.middlewares.addAll(middlewares);
		} else {

			// Apply middlewares now
			serviceRegistry.use(middlewares);
		}
	}

	/**
	 * Installs one or an array of middleware(s).
	 * 
	 * @param middlewares
	 *            array of middlewares
	 */
	public void use(Middleware... middlewares) {
		use(Arrays.asList(middlewares));
	}

	// --- GET LOCAL OR REMOTE ACTION ---

	/**
	 * Returns an action by name.
	 *
	 * @param actionName
	 *            name of the action (in "service.action" syntax, eg.
	 *            "math.add")
	 * 
	 * @return local or remote action container
	 */
	public Action getAction(String actionName) {
		return serviceRegistry.getAction(actionName, null);
	}

	/**
	 * Returns an action by name and nodeID.
	 *
	 * @param actionName
	 *            name of the action (in "service.action" syntax, eg.
	 *            "math.add")
	 * @param nodeID
	 *            node identifier where the service is located
	 * 
	 * @return local or remote action container
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
	 * 
	 * @param name
	 *            action name (eg. "math.add" in "service.action" syntax)
	 * @param params
	 *            list of parameter name-value pairs and an optional CallOptions
	 * 
	 * @return response Promise
	 */
	public Promise call(String name, Object... params) {
		ParseResult res = parseParams(params);
		return serviceInvoker.call(name, res.data, res.opts, res.stream, null);
	}

	/**
	 * Calls an action (local or remote). Sample code:<br>
	 * <br>
	 * Tree params = new Tree();<br>
	 * params.put("a", true);<br>
	 * params.putList("b").add(1).add(2).add(3);<br>
	 * Promise promise = broker.call("math.add", params);
	 * 
	 * @param name
	 *            action name (eg. "math.add" in "service.action" syntax)
	 * @param params
	 *            {@link Tree} structure (input parameters of the method call)
	 * 
	 * @return response Promise
	 */
	public Promise call(String name, Tree params) {
		return serviceInvoker.call(name, params, null, null, null);
	}

	/**
	 * Calls an action (local or remote). Sample code:<br>
	 * <br>
	 * Tree params = new Tree();<br>
	 * params.put("a", true);<br>
	 * params.putList("b").add(1).add(2).add(3);<br>
	 * Promise promise = broker.call("math.add", params,
	 * CallOptions.nodeID("node2"));
	 * 
	 * @param name
	 *            action name (eg. "math.add" in "service.action" syntax)
	 * @param params
	 *            {@link Tree} structure (input parameters of the method call)
	 * @param opts
	 *            calling options (target nodeID, call timeout, number of
	 *            retries)
	 * 
	 * @return response Promise
	 */
	public Promise call(String name, Tree params, CallOptions.Options opts) {
		return serviceInvoker.call(name, params, opts, null, null);
	}

	// --- EMIT EVENT TO EVENT GROUP ---

	/**
	 * Emits an event to <b>ONE</b> listener from ALL (or the specified) event
	 * group(s), who are listening this event. The service broker uses the
	 * default {@link Strategy strategy} of the broker for event redirection and
	 * node selection. Sample code:<br>
	 * <br>
	 * broker.emit("user.deleted", "a", 1, "b", 2);<br>
	 * <br>
	 * ...or send event to (one or more) listener group(s):<br>
	 * <br>
	 * broker.emit("user.deleted", "a", 1, "b", 2, Groups.of("logger"));
	 * 
	 * @param name
	 *            name of event (eg. "user.deleted")
	 * @param params
	 *            list of parameter name-value pairs and an optional
	 *            {@link Groups event group} container
	 */
	public void emit(String name, Object... params) {
		ParseResult res = parseParams(params);
		eventbus.emit(name, res.data, res.groups, false);
	}

	/**
	 * Emits an event to <b>ONE</b> listener from the specified event group(s),
	 * who are listening this event. The service broker uses the default
	 * {@link Strategy strategy} of the broker for event redirection and node
	 * selection. Sample code:<br>
	 * <br>
	 * Tree params = new Tree();<br>
	 * params.put("a", true);<br>
	 * params.putList("b").add(1).add(2).add(3);<br>
	 * broker.emit("user.created", params, Groups.of("group1", "group2"));
	 * 
	 * @param name
	 *            name of event (eg. "user.modified")
	 * @param payload
	 *            {@link Tree} structure (payload of the event)
	 * @param groups
	 *            {@link Groups event group} container
	 */
	public void emit(String name, Tree payload, Groups groups) {
		eventbus.emit(name, payload, groups, false);
	}

	/**
	 * Emits an event to <b>ONE</b> listener from ALL event groups, who are
	 * listening this event. The service broker uses the default {@link Strategy
	 * strategy} of the broker for event redirection and node selection. Sample
	 * code:<br>
	 * <br>
	 * Tree params = new Tree();<br>
	 * params.put("a", true);<br>
	 * params.putList("b").add(1).add(2).add(3);<br>
	 * broker.emit("user.modified", params);
	 * 
	 * @param name
	 *            name of event (eg. "user.created")
	 * @param payload
	 *            {@link Tree} structure (payload of the event)
	 */
	public void emit(String name, Tree payload) {
		eventbus.emit(name, payload, null, false);
	}

	// --- BROADCAST EVENT TO ALL LISTENERS ---

	/**
	 * Emits an event to <b>ALL</b> listeners from ALL (or the specified) event
	 * group(s), who are listening this event. Sample code:<br>
	 * <br>
	 * broker.broadcast("user.deleted", "a", 1, "b", 2);<br>
	 * <br>
	 * ...or send event to (one or more) listener group(s):<br>
	 * <br>
	 * broker.broadcast("user.deleted", "a", 1, "b", 2, Groups.of("logger"));
	 * 
	 * @param name
	 *            name of event (eg. "user.deleted")
	 * @param params
	 *            list of parameter name-value pairs and an optional
	 *            {@link Groups event group} container
	 */
	public void broadcast(String name, Object... params) {
		ParseResult res = parseParams(params);
		eventbus.broadcast(name, res.data, res.groups, false);
	}

	/**
	 * Emits an event to <b>ALL</b> listeners from the specified event group(s),
	 * who are listening this event. Sample code:<br>
	 * <br>
	 * Tree params = new Tree();<br>
	 * params.put("a", true);<br>
	 * params.putList("b").add(1).add(2).add(3);<br>
	 * broker.broadcast("user.created", params, Groups.of("group1", "group2"));
	 * 
	 * @param name
	 *            name of event (eg. "user.modified")
	 * @param payload
	 *            {@link Tree} structure (payload of the event)
	 * @param groups
	 *            {@link Groups event group} container
	 */
	public void broadcast(String name, Tree payload, Groups groups) {
		eventbus.broadcast(name, payload, groups, false);
	}

	/**
	 * Emits an event to <b>ALL</b> listeners from ALL event groups, who are
	 * listening this event. Sample code:<br>
	 * <br>
	 * Tree params = new Tree();<br>
	 * params.put("a", true);<br>
	 * params.putList("b").add(1).add(2).add(3);<br>
	 * broker.broadcast("user.modified", params);
	 * 
	 * @param name
	 *            name of event (eg. "user.created")
	 * @param payload
	 *            {@link Tree} structure (payload of the event)
	 */
	public void broadcast(String name, Tree payload) {
		eventbus.broadcast(name, payload, null, false);
	}

	// --- BROADCAST EVENT TO LOCAL LISTENERS ---

	/**
	 * Emits a <b>LOCAL</b> event to <b>ALL</b> listeners from ALL (or the
	 * specified) event group(s), who are listening this event. Sample code:<br>
	 * <br>
	 * broker.broadcastLocal("user.deleted", "a", 1, "b", 2);<br>
	 * <br>
	 * ...or send event to (one or more) local listener group(s):<br>
	 * <br>
	 * broker.broadcastLocal("user.deleted", "a", 1, "b", 2,
	 * Groups.of("logger"));
	 * 
	 * @param name
	 *            name of event (eg. "user.deleted")
	 * @param params
	 *            list of parameter name-value pairs and an optional
	 *            {@link Groups event group} container
	 */
	public void broadcastLocal(String name, Object... params) {
		ParseResult res = parseParams(params);
		eventbus.broadcast(name, res.data, res.groups, true);
	}

	/**
	 * Emits a <b>LOCAL</b> event to <b>ALL</b> listeners from the specified
	 * event group(s), who are listening this event. Sample code:<br>
	 * <br>
	 * Tree params = new Tree();<br>
	 * params.put("a", true);<br>
	 * params.putList("b").add(1).add(2).add(3);<br>
	 * broker.broadcastLocal("user.created", params, Groups.of("group1",
	 * "group2"));
	 * 
	 * @param name
	 *            name of event (eg. "user.modified")
	 * @param payload
	 *            {@link Tree} structure (payload of the event)
	 * @param groups
	 *            {@link Groups event group} container
	 */
	public void broadcastLocal(String name, Tree payload, Groups groups) {
		eventbus.broadcast(name, payload, groups, true);
	}

	/**
	 * Emits a <b>LOCAL</b> event to <b>ALL</b> listeners from ALL event groups,
	 * who are listening this event. Sample code:<br>
	 * <br>
	 * Tree params = new Tree();<br>
	 * params.put("a", true);<br>
	 * params.putList("b").add(1).add(2).add(3);<br>
	 * broker.broadcastLocal("user.modified", params);
	 * 
	 * @param name
	 *            name of event (eg. "user.created")
	 * @param payload
	 *            {@link Tree} structure (payload of the event)
	 */
	public void broadcastLocal(String name, Tree payload) {
		eventbus.broadcast(name, payload, null, true);
	}

	// --- WAIT FOR SERVICE(S) ---

	/**
	 * Waits for one or more services. Sample code:<br>
	 * <br>
	 * broker.waitForServices("logger", "printer").then(in -&gt; {<br>
	 * broker.getLogger().info("Logger and printer started");<br>
	 * }
	 * 
	 * @param services
	 *            service names
	 * 
	 * @return a listenable Promise
	 */
	public Promise waitForServices(String... services) {
		return waitForServices(0, services);
	}

	/**
	 * Waits for one (or an array of) service(s). Sample code:<br>
	 * <br>
	 * broker.waitForServices(5000, "logger").then(in -&gt; {<br>
	 * broker.getLogger().info("Logger started successfully");<br>
	 * }.catchError(error -&gt; {<br>
	 * broker.getLogger().info("Logger did not start");<br>
	 * }
	 * 
	 * @param timeoutMillis
	 *            timeout in milliseconds
	 * @param services
	 *            array of service names
	 * 
	 * @return listenable Promise
	 */
	public Promise waitForServices(long timeoutMillis, String... services) {
		return waitForServices(timeoutMillis, Arrays.asList(services));
	}

	/**
	 * Waits for a collection of services. Sample code:<br>
	 * <br>
	 * Set&lt;String&gt; serviceNames = ...<br>
	 * broker.waitForServices(5000, serviceNames).then(in -&gt; {<br>
	 * broker.getLogger().info("Ok");<br>
	 * }.catchError(error -&gt; {<br>
	 * broker.getLogger().info("Failed / timeout");<br>
	 * }
	 * 
	 * @param timeoutMillis
	 *            timeout in milliseconds
	 * @param services
	 *            collection of service names
	 * 
	 * @return listenable Promise
	 */
	public Promise waitForServices(long timeoutMillis, Collection<String> services) {
		return serviceRegistry.waitForServices(timeoutMillis, services);
	}

	// --- PING LOCAL OR REMOTE NODE ---

	/**
	 * Sends a PING message to the specified node. The ping timeout is 3
	 * seconds. Sample:<br>
	 * <br>
	 * broker.ping("node2").then(in -&gt; {<br>
	 * broker.getLogger().info("Ok");<br>
	 * }.catchError(error -&gt; {<br>
	 * broker.getLogger().info("Ping timeouted");<br>
	 * }
	 * 
	 * @param nodeID
	 *            node ID of the destination node
	 * 
	 * @return listenable Promise
	 */
	public Promise ping(String nodeID) {
		return serviceRegistry.ping(3000, nodeID);
	}

	/**
	 * Sends a PING message to the specified node. Sample:<br>
	 * <br>
	 * broker.ping(5000, "node2").then(in -&gt; {<br>
	 * broker.getLogger().info("Ok");<br>
	 * }.catchError(error -&gt; {<br>
	 * broker.getLogger().info("Ping timeouted");<br>
	 * }
	 * 
	 * @param timeoutMillis
	 *            ping timeout in milliseconds
	 * @param nodeID
	 *            node ID of the destination node
	 * 
	 * @return listenable Promise
	 */
	public Promise ping(long timeoutMillis, String nodeID) {
		return serviceRegistry.ping(timeoutMillis, nodeID);
	}

	// --- STREAMED REQUEST OR RESPONSE ---

	/**
	 * Creates a stream what is suitable for transferring large files (or other
	 * "unlimited" media content) between Moleculer Nodes. Sample:<br>
	 * 
	 * <pre>
	 * public Action send = ctx -> {
	 *   PacketStream reqStream = broker.createStream();
	 *   
	 *   ctx.call("service.action", reqStream).then(rsp -> {
	 *   
	 *     // Receive bytes into file
	 *     PacketStream rspStream = (PacketStream) rsp.asObject();
	 *     rspStream.transferTo(new File("out"));
	 *   }
	 *   
	 *   // Send bytes from file
	 *   reqStream.transferFrom(new File("in"));
	 * }
	 * </pre>
	 * 
	 * @return new stream
	 */
	public PacketStream createStream() {
		return new PacketStream(config.getScheduler());
	}

	// --- START DEVELOPER CONSOLE ---

	/**
	 * Starts a local (System in/out) developer console. You must install
	 * "moleculer-java-repl" dependency for use this feature.
	 * 
	 * @return true if started
	 */
	public boolean repl() {
		return repl(true);
	}

	/**
	 * Starts a local (System in/out) or a remote (telnet-based) developer
	 * console. You must install "moleculer-java-repl" dependency to use this
	 * feature.
	 * 
	 * @param local
	 *            true = local console, false = telnet-based console
	 * 
	 * @return true if started
	 */
	public boolean repl(boolean local) {
		try {
			String className = local ? "Local" : "Remote";
			String serviceName = local ? "$repl" : "$repl-remote";
			try {
				serviceRegistry.getService(serviceName);
				return false;
			} catch (Exception ignored) {
			}
			createService(serviceName,
					(Service) Class.forName("services.moleculer.repl." + className + "Repl").newInstance());
			return true;
		} catch (ClassNotFoundException notFound) {
			logger.error("Unable to start REPL console!");
			suggestDependency("com.github.berkesa", "moleculer-java-repl", "1.0.1");
		} catch (Exception cause) {
			logger.error("Unable to start REPL console!", cause);
		}
		return false;
	}

}