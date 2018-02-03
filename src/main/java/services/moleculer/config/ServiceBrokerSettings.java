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
package services.moleculer.config;

import static services.moleculer.config.ComponentRegistry.COMPONENTS_ID;
import static services.moleculer.config.ComponentRegistry.EXECUTOR_ID;
import static services.moleculer.config.ComponentRegistry.SCHEDULER_ID;
import static services.moleculer.util.CommonUtils.getFormat;
import static services.moleculer.util.CommonUtils.getHostName;
import static services.moleculer.util.CommonUtils.readTree;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import io.datatree.dom.TreeReader;
import io.datatree.dom.TreeReaderRegistry;
import io.datatree.dom.TreeWriter;
import io.datatree.dom.TreeWriterRegistry;
import services.moleculer.cacher.Cacher;
import services.moleculer.cacher.MemoryCacher;
import services.moleculer.context.ContextFactory;
import services.moleculer.context.DefaultContextFactory;
import services.moleculer.eventbus.DefaultEventBus;
import services.moleculer.eventbus.EventBus;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.monitor.Monitor;
import services.moleculer.repl.LocalRepl;
import services.moleculer.repl.Repl;
import services.moleculer.service.DefaultServiceRegistry;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.strategy.RoundRobinStrategyFactory;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.transporter.Transporter;
import services.moleculer.uid.IncrementalUIDGenerator;
import services.moleculer.uid.UIDGenerator;

/**
 * POJO-style ServiceBroker factory (eg. for Spring Framework). Sample of usage:
 * <br>
 * <br>
 * ServiceBrokerSettings settings = new ServiceBrokerSettings();<br>
 * settings.setCacher(cacher);<br>
 * ServiceBroker settings = new ServiceBroker(settings);
 */
public final class ServiceBrokerSettings {

	// --- LOGGER ---

	private static final Logger logger = LoggerFactory.getLogger(ServiceBrokerSettings.class);

	// --- THREAD POOLS ---

	private ExecutorService executor;
	private ScheduledExecutorService scheduler;

	private boolean shutDownThreadPools = true;

	// --- PROPERTIES ---

	private String namespace = "";
	private String nodeID;

	/**
	 * Install internal ($node) services?
	 */
	private boolean internalServices = true;

	// --- JSON API SERIALIZER / DESERIALIZER ---

	/**
	 * Name of the JSON deserializer API ("jackson", "boon", "builtin", "gson",
	 * "fastjson", "genson", etc., null = autodetect)
	 */
	private String jsonReader;

	/**
	 * Name of the JSON serializer API ("jackson", "boon", "builtin", "gson",
	 * "fast", "genson", "flex", "nano", etc., null = autodetect)
	 */
	private String jsonWriter;

	// --- COMPONENT REGISTRY (STANDALONE/SPRING/GUICE) ---

	private ComponentRegistry components = new StandaloneComponentRegistry();
	private Tree config = new Tree();

	// --- INTERNAL COMPONENTS ---

	private ContextFactory context = new DefaultContextFactory();
	private ServiceRegistry registry = new DefaultServiceRegistry();
	private EventBus eventbus = new DefaultEventBus();
	private UIDGenerator uid = new IncrementalUIDGenerator();
	private StrategyFactory strategy = new RoundRobinStrategyFactory();
	private Transporter transporter;
	private Cacher cacher = new MemoryCacher();
	private Monitor monitor;
	private Repl repl = new LocalRepl();

	// --- CUSTOM COMPONENTS ---

	private final LinkedHashMap<String, MoleculerComponentContainer> componentMap = new LinkedHashMap<>();

	// --- INSTALL JS PARSER ---

	static {
		try {
			if (!TreeReaderRegistry.isAvailable("js")) {
				TreeReaderRegistry.setReader("js", new JSReader());
			}
		} catch (Exception ignored) {
		}
	}

	// --- CONSTRUCTORS ---

	public ServiceBrokerSettings() {

		// Create default thread pools
		executor = ForkJoinPool.commonPool();
		scheduler = Executors.newScheduledThreadPool(ForkJoinPool.commonPool().getParallelism());

		// Set the default System Monitor
		monitor = tryToLoadMonitor("Sigar");
		if (monitor == null) {
			logger.info("Sigar System Monitoring API not available.");
			monitor = tryToLoadMonitor("JMX");
			if (monitor == null) {
				logger.info("JMX Monitoring API not available.");
				monitor = new ConstantMonitor();
			}
		}

		// Set the default NodeID
		nodeID = getHostName() + '-' + monitor.getPID();
	}

	private static final Monitor tryToLoadMonitor(String type) {
		try {
			Class<?> c = ServiceBrokerSettings.class.getClassLoader()
					.loadClass("services.moleculer.monitor." + type + "Monitor");
			Monitor m = (Monitor) c.newInstance();
			m.start(null, new Tree());
			m.getTotalCpuPercent();
			return m;
		} catch (Throwable ignored) {
		}
		return null;
	}

	public ServiceBrokerSettings(String nodeID, Transporter transporter, Cacher cacher) {
		setNodeID(nodeID);
		setTransporter(transporter);
		setCacher(cacher);
	}

	public ServiceBrokerSettings(String configPath) throws Exception {
		this();
		if (configPath == null || configPath.isEmpty()) {
			return;
		}
		String format = getFormat(configPath.toString());
		logger.info("Loading configuration from \"" + configPath + "\" in "
				+ (format == null ? "JSON" : format.toUpperCase()) + " format...");
		if (configPath.startsWith("http:") || configPath.startsWith("https:") || configPath.startsWith("file:")) {
			config = readTree(new URL(configPath).openStream(), format);
			applyConfiguration();
			return;
		}
		URL url = getClass().getResource(configPath);
		if (url == null) {
			url = getClass().getResource('/' + configPath);
		}
		if (url != null) {
			config = readTree(url.openStream(), format);
			applyConfiguration();
			return;
		}
		File file = new File(configPath);
		if (file.isFile()) {
			config = readTree(new FileInputStream(file), format);
			applyConfiguration();
			return;
		}
		throw new IllegalArgumentException("Resource \"" + configPath + "\" not found!");
	}

	// --- INTERNAL SETTERS ---

	private final void applyConfiguration() throws Exception {

		// Debug
		if (logger.isDebugEnabled()) {
			logger.debug("Apply configuration:\r\n" + config);
		}

		// TODO Set base proeprties
		setNamespace(config.get("namespace", namespace));
		setNodeID(config.get("nodeID", nodeID));

		internalServices = config.get("internalServices", internalServices);

		// Set JSON API
		applyJsonAPI();
		 
		// Create executor
		String value = config.get(EXECUTOR_ID + '.' + "type", "");
		if (!value.isEmpty()) {
			setExecutor((ExecutorService) Class.forName(value).newInstance());
		}

		// Create scheduler
		value = config.get(SCHEDULER_ID + '.' + "type", "");
		if (!value.isEmpty()) {
			setScheduler((ScheduledExecutorService) Class.forName(value).newInstance());
		}

		// Should terminate thread pools on stop?
		value = config.get("shutDownThreadPools", "");
		if (!value.isEmpty()) {
			shutDownThreadPools = "true".equals(value);
		}

		// Create Component Registry
		value = config.get(COMPONENTS_ID + '.' + "type", "");
		if (!value.isEmpty()) {
			String test = value.toLowerCase();
			if (test.equals("standalone")) {
				value = "services.moleculer.config.StandaloneComponentRegistry";
			} else if (test.equals("spring")) {
				value = "services.moleculer.config.SpringComponentRegistry";
			} else if (test.equals("guice")) {
				value = "services.moleculer.config.GuiceComponentRegistry";
			}
			setComponents((ComponentRegistry) Class.forName(value).newInstance());
		}
	}

	private final void applyJsonAPI() {
		
		// Set the JSON deserializer API
		jsonReader = config.get("jsonReader", jsonReader);
		if (jsonReader != null && !jsonReader.isEmpty()) {
			String test = jsonReader.toLowerCase();
			Set<String> readers = TreeReaderRegistry.getReadersByFormat("json");
			for (String reader : readers) {
				if (reader.toLowerCase().contains(test)) {
					try {
						TreeReaderRegistry.setReader("json", (TreeReader) Class.forName(reader).newInstance());
					} catch (Exception ignored) {
					}
					break;
				}
			}
		}

		// Set the JSON serializer API
		jsonWriter = config.get("jsonWriter", jsonWriter);
		if (jsonWriter != null && !jsonWriter.isEmpty()) {
			String test = jsonReader.toLowerCase();
			Set<String> writers = TreeWriterRegistry.getWritersByFormat("json");
			for (String writer : writers) {
				if (writer.toLowerCase().contains(test)) {
					try {
						TreeWriterRegistry.setWriter("json", (TreeWriter) Class.forName(writer).newInstance());
					} catch (Exception ignored) {
					}
					break;
				}
			}
		}
	}
	
	// --- GETTERS AND SETTERS ---

	public final String getNamespace() {
		return namespace;
	}

	public final void setNamespace(String namespace) {
		this.namespace = Objects.requireNonNull(namespace);
	}

	public final String getNodeID() {
		return nodeID;
	}

	public final void setNodeID(String nodeID) {
		if (nodeID != null) {
			nodeID = Objects.requireNonNull(nodeID).trim();
			if (nodeID.isEmpty()) {
				throw new IllegalArgumentException("Empty nodeID is not allowed!");
			}
			this.nodeID = nodeID;
		}
	}

	public final Map<String, MoleculerComponentContainer> getComponentMap() {
		return componentMap;
	}

	public final void setComponentMap(Map<String, MoleculerComponentContainer> components) {
		Objects.requireNonNull(components);
		components.clear();
		components.putAll(components);
	}

	public final Transporter getTransporter() {
		return transporter;
	}

	public final void setTransporter(Transporter transporter) {
		this.transporter = transporter;
	}

	public final Cacher getCacher() {
		return cacher;
	}

	public final void setCacher(Cacher cacher) {
		this.cacher = cacher;
	}

	public final ServiceRegistry getRegistry() {
		return registry;
	}

	public final void setRegistry(ServiceRegistry serviceRegistry) {
		this.registry = serviceRegistry;
	}

	public final EventBus getEventbus() {
		return eventbus;
	}

	public final void setEventbus(EventBus eventBus) {
		this.eventbus = Objects.requireNonNull(eventBus);
	}

	public final UIDGenerator getUid() {
		return uid;
	}

	public final void setUid(UIDGenerator uidGenerator) {
		this.uid = Objects.requireNonNull(uidGenerator);
	}

	public final ContextFactory getContext() {
		return context;
	}

	public final void setContext(ContextFactory contextFactory) {
		this.context = Objects.requireNonNull(contextFactory);
	}

	public final StrategyFactory getStrategy() {
		return strategy;
	}

	public final void setStrategy(StrategyFactory strategyFactory) {
		this.strategy = Objects.requireNonNull(strategyFactory);
	}

	public final ExecutorService getExecutor() {
		return executor;
	}

	public final void setExecutor(ExecutorService executor) {
		this.executor = Objects.requireNonNull(executor);
	}

	public final ScheduledExecutorService getScheduler() {
		return scheduler;
	}

	public final void setScheduler(ScheduledExecutorService scheduler) {
		this.scheduler = Objects.requireNonNull(scheduler);
	}

	public final Tree getConfig() {
		return config;
	}

	public final void setConfig(Tree config) {
		this.config = Objects.requireNonNull(config);
		try {
			applyConfiguration();
		} catch (Exception cause) {
			throw new IllegalArgumentException("Invalid configuration!", cause);
		}
	}

	public final ComponentRegistry getComponents() {
		return components;
	}

	public final void setComponents(ComponentRegistry componentRegistry) {
		this.components = Objects.requireNonNull(componentRegistry);
	}

	public final boolean getShutDownThreadPools() {
		return shutDownThreadPools;
	}

	public final void setShutDownThreadPools(boolean shutDownThreadPools) {
		this.shutDownThreadPools = shutDownThreadPools;
	}

	public final Monitor getMonitor() {
		return monitor;
	}

	public final void setMonitor(Monitor monitor) {
		this.monitor = monitor;
	}

	public final Repl getRepl() {
		return repl;
	}

	public final void setRepl(Repl repl) {
		this.repl = repl;
	}

	public final boolean isInternalServices() {
		return internalServices;
	}

	public final void setInternalServices(boolean internalServices) {
		this.internalServices = internalServices;
	}

	public final String getJsonReader() {
		return jsonReader;
	}

	public final void setJsonReader(String jsonReader) {
		this.jsonReader = jsonReader;
		applyJsonAPI();
	}

	public final String getJsonWriter() {
		return jsonWriter;
	}

	public final void setJsonWriter(String jsonWriter) {
		this.jsonWriter = jsonWriter;
		applyJsonAPI();
	}

}