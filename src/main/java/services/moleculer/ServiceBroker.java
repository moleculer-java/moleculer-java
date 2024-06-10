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
import static services.moleculer.util.CommonUtils.suggestDependency;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import services.moleculer.cacher.Cacher;
import services.moleculer.config.ServiceBrokerBuilder;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.Context;
import services.moleculer.context.ContextSource;
import services.moleculer.error.MoleculerServerError;
import services.moleculer.internal.NodeService;
import services.moleculer.metrics.DefaultMetrics;
import services.moleculer.metrics.MetricConstants;
import services.moleculer.metrics.MetricMiddleware;
import services.moleculer.metrics.Metrics;
import services.moleculer.service.Action;
import services.moleculer.service.Middleware;
import services.moleculer.service.MoleculerComponent;
import services.moleculer.service.MoleculerLifecycle;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.transporter.Transporter;

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
 * 	Action add = ctx -&gt; {
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
@Name("Default Metric Registry")
public class ServiceBroker extends ContextSource implements MetricConstants {

	// --- VERSIONS ---

	/**
	 * Version of the Java ServiceBroker API.
	 */
	public static final String SOFTWARE_VERSION = "1.2.26";

	// --- LOGGER ---

	/**
	 * SLF4J logger of this class.
	 */
	protected static final Logger logger = LoggerFactory.getLogger(ServiceBroker.class);

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
	protected final ConcurrentHashMap<String, Service> services = new ConcurrentHashMap<>();

	/**
	 * Service names (keys).
	 */
	protected final LinkedHashSet<String> serviceNames = new LinkedHashSet<>();
	
	// --- ENQUED MIDDLEWARES ---

	/**
	 * Middlewares which defined and added to the Broker before the boot
	 * process.
	 */
	protected final LinkedHashSet<Middleware> middlewares = new LinkedHashSet<>();

	// --- INTERNAL COMPONENTS ---

	/**
	 * Default (round-robin) service invocation factory. Use
	 * <code>getConfig().getStrategyFactory()</code> to access this instance.
	 */
	protected StrategyFactory strategyFactory;

	/**
	 * Implementation of the service registry of the current node. Use
	 * <code>getConfig().getServiceRegistry()</code> to access this instance.
	 * 
	 * @see #call(String, Object...)
	 */
	protected ServiceRegistry serviceRegistry;

	/**
	 * Implementation of the Transporter. Use
	 * <code>getConfig().getTransporter()</code> to access this instance. Can be
	 * <code>null</code>.
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
		super(config.getServiceInvoker(), config.getEventbus(), config.getUidGenerator(), config.getNodeID());
		this.config = config;
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

	// --- PROTOCOL VERSION ---

	/**
	 * Returns the version of the implemented Moleculer Protocol. Can be
	 * configured with the "moleculer.protocol.version" System Property.
	 * 
	 * @return version of the implemented protocol (eg. "4")
	 */
	public String getProtocolVersion() {
		return System.getProperty("moleculer.protocol.version", "4");
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
	 * @return this ServiceBroker instance (from "method chaining")
	 */
	public ServiceBroker start() throws Exception {

		// Check state
		if (serviceRegistry != null) {
			throw new MoleculerServerError("Moleculer Service Broker has already been started!", nodeID,
					"ALREADY_STARTED");
		}
		try {

			// Start internal components, services, middlewares...
			logger.info("Starting Moleculer Service Broker (version " + SOFTWARE_VERSION + ")...");

			// Set global JSON reader API (Jackson, Gson, Boon, FastJson, etc.)
			initJsonReader();

			// Set global JSON writer API (Jackson, Gson, Boon, FastJson, etc.)
			initJsonWriter();

			// Add MetricsRegistry to middlewares
			Metrics metrics = start(config.getMetrics());
			boolean metricsEnabled = config.isMetricsEnabled();
			if (metricsEnabled && metrics != null) {
				middlewares.add(new MetricMiddleware());
				logger.info("Metrics are enabled (turn off the measurement if you do not collect performance data).");
				if (metrics instanceof DefaultMetrics) {
					DefaultMetrics dm = (DefaultMetrics) metrics;
					ExecutorService executor = config.getExecutor();
					if (executor != null) {
						dm.addExecutorServiceMetrics(executor, MOLECULER_EXECUTOR);
					}
					ScheduledExecutorService scheduler = config.getScheduler();
					if (scheduler != null && scheduler != executor) {
						dm.addExecutorServiceMetrics(scheduler, MOLECULER_SCHEDULER);
					}
				}
			} else {
				logger.info("Metrics are disabled.");
			}

			// Set internal components
			start(uidGenerator);
			strategyFactory = start(config.getStrategyFactory());
			start(serviceInvoker);
			start(eventbus);
			serviceRegistry = start(config.getServiceRegistry());
			transporter = start(config.getTransporter());

			// Register enqued middlewares
			Cacher cacher = config.getCacher();
			if (cacher != null) {
				middlewares.add(cacher);
				logger.info(nameOf(cacher, true) + " started.");
			}
			serviceRegistry.use(middlewares);

			// Install internal services
			if (config.isInternalServices()) {
				createService("$node", new NodeService());
			}

			// Register and start enqued services and listeners
			int serviceCount = serviceNames.size();
			if (serviceCount > 0) {
				Promise[] allLoaded = new Promise[serviceCount];
				int index = 0;
				for (String serviceName: serviceNames) {
					Service service = services.get(serviceName);
					if (service == null) {
						continue;
					}
					allLoaded[index++] = serviceRegistry.addActions(serviceName, service).then(deployed -> {

						// Add listeners...
						eventbus.addListeners(serviceName, service);

						// Notify local listeners about the new LOCAL service
						broadcastServicesChanged();
					});
				}
				Promise.all(allLoaded).waitFor();
			}

			// Start transporter's connection loop
			if (transporter != null) {
				transporter.connect();
			}

			// Notify listeners
			eventbus.broadcast(new Context(serviceInvoker, eventbus, uidGenerator, uidGenerator.nextUID(),
					"$broker.started", null, 1, null, null, null, null, nodeID), null, true);

			// Ok, services, transporter and gateway started
			logger.info("Node \"" + config.getNodeID() + "\" started successfully.");

		} catch (Throwable cause) {
			logger.error("Moleculer Service Broker could not be started!", cause);
			stop();
		} finally {
			middlewares.clear();
			serviceNames.clear();
			services.clear();
		}
		return this;
	}

	/**
	 * Set global JSON reader API (Jackson, Gson, Boon, FastJson, etc.).
	 */
	protected void initJsonReader() {
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
							logger.info("Default JSON deserializer/reader is \"" + selectedReader.getClass() + "\".");
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
	}

	/**
	 * Set global JSON writer API (Jackson, Gson, Boon, FastJson, etc.)
	 */
	protected void initJsonWriter() {
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
	protected <TYPE extends MoleculerLifecycle> TYPE start(TYPE component) throws Exception {
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
	 * 
	 * @return this ServiceBroker instance (from "method chaining")
	 */
	public ServiceBroker stop() {

		// Stop transporter and services
		stop(transporter);
		stop(serviceRegistry);

		// Notify listeners
		if (eventbus != null && serviceInvoker != null && uidGenerator != null) {
			eventbus.broadcast(new Context(serviceInvoker, eventbus, uidGenerator, uidGenerator.nextUID(),
					"$broker.stopped", null, 1, null, null, null, null, nodeID), null, true);
		}

		// Stop other internal components
		stop(eventbus);
		stop(serviceInvoker);
		stop(strategyFactory);
		stop(uidGenerator);

		Metrics metrics = config.getMetrics();
		if (metrics != null) {
			metrics.stopped();
		}

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
		return this;
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
	 * @return this ServiceBroker instance (from "method chaining")
	 */
	public ServiceBroker createService(Service service) {
		createService(service.getName(), service);
		return this;
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
	 * @return this ServiceBroker instance (from "method chaining")
	 */
	public ServiceBroker createService(String name, Service service) {
		if (serviceRegistry == null) {

			// Start service later
			serviceNames.add(name);
			services.put(name, service);
		} else {

			// Register and start service now
			serviceRegistry.addActions(name, service).then(deployed -> {
				eventbus.addListeners(name, service);

				// Notify local listeners about the new LOCAL service
				broadcastServicesChanged();

				// Notify other nodes
				if (transporter != null) {
					transporter.broadcastInfoPacket();
				}
			});
		}
		return this;
	}

	// --- NOTIFY OTHER SERVICES ---

	protected void broadcastServicesChanged() {
		Tree msg = new Tree();
		msg.put("localService", true);
		eventbus.broadcast(new Context(serviceInvoker, eventbus, uidGenerator, uidGenerator.nextUID(),
				"$services.changed", msg, 1, null, null, null, null, nodeID), null, true);
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
		Service service = services.get(serviceName);
		if (service != null) {
			return service;
		}
		return serviceRegistry.getService(serviceName);
	}

	// --- ADD MIDDLEWARE ---

	/**
	 * Installs a collection of middlewares.
	 * 
	 * @param middlewares
	 *            collection of middlewares
	 * @return this ServiceBroker instance (from "method chaining")
	 */
	public ServiceBroker use(Collection<Middleware> middlewares) {
		if (serviceRegistry == null) {

			// Apply middlewares later
			this.middlewares.addAll(middlewares);
		} else {

			// Apply middlewares now
			serviceRegistry.use(middlewares);
		}
		return this;
	}

	/**
	 * Installs one or an array of middleware(s).
	 * 
	 * @param middlewares
	 *            array of middlewares
	 * @return this ServiceBroker instance (from "method chaining")
	 */
	public ServiceBroker use(Middleware... middlewares) {
		return use(Arrays.asList(middlewares));
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
			suggestDependency("com.github.berkesa", "moleculer-java-repl", "1.3.0");
		} catch (Exception cause) {
			logger.error("Unable to start REPL console!", cause);
		}
		return false;
	}

}