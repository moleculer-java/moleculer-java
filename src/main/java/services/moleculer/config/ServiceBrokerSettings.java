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
package services.moleculer.config;

import static services.moleculer.config.ComponentRegistry.COMPONENTS_ID;
import static services.moleculer.config.ComponentRegistry.EXECUTOR_ID;
import static services.moleculer.config.ComponentRegistry.SCHEDULER_ID;
import static services.moleculer.util.CommonUtils.getFormat;
import static services.moleculer.util.CommonUtils.readTree;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import io.datatree.dom.TreeReaderRegistry;
import services.moleculer.cacher.Cacher;
import services.moleculer.cacher.MemoryCacher;
import services.moleculer.context.ContextFactory;
import services.moleculer.context.DefaultContextFactory;
import services.moleculer.eventbus.DefaultEventBus;
import services.moleculer.eventbus.EventBus;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.monitor.Monitor;
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
public final class ServiceBrokerSettings implements CommonNames {

	// --- LOGGER ---

	private static final Logger logger = LoggerFactory.getLogger(ServiceBrokerSettings.class);

	// --- THREAD POOLS ---

	private ExecutorService executor;
	private ScheduledExecutorService scheduler;

	private boolean shutDownThreadPools = true;

	// --- PROPERTIES AND COMPONENTS ---

	private String namespace = "";
	private String nodeID;

	private ComponentRegistry components = new StandaloneComponentRegistry();
	private Tree config = new Tree();

	private ContextFactory context = new DefaultContextFactory();
	private ServiceRegistry registry = new DefaultServiceRegistry();
	private EventBus eventbus = new DefaultEventBus();
	private UIDGenerator uid = new IncrementalUIDGenerator();
	private StrategyFactory strategy = new RoundRobinStrategyFactory();
	private Transporter transporter;
	private Cacher cacher = new MemoryCacher();
	private Monitor monitor;

	// --- CUSTOM COMPONENTS ---

	private final LinkedHashMap<String, MoleculerComponentContainer> componentMap = new LinkedHashMap<>();

	// --- INSTALL JS PARSER ---

	static {
		try {
			TreeReaderRegistry.getReader("js");
		} catch (Exception notInstaller) {
			TreeReaderRegistry.setReader("js", new JSReader());
		}
	}

	// --- CONSTRUCTORS ---

	public ServiceBrokerSettings() {

		// Set the default NodeID
		try {
			nodeID = InetAddress.getLocalHost().getHostName();
		} catch (Exception ignored) {
		}
		if (nodeID == null || nodeID.isEmpty()) {
			nodeID = "node" + System.currentTimeMillis();
		}

		// Create thread pools
		executor = ForkJoinPool.commonPool();
		scheduler = Executors.newSingleThreadScheduledExecutor();

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

		// Set base proeprties
		setNamespace(config.get(NAMESPACE, namespace));
		setNodeID(config.get(NODE_ID, nodeID));

		// Create executor
		String value = config.get(EXECUTOR_ID + '.' + TYPE, "");
		if (!value.isEmpty()) {
			setExecutor((ExecutorService) Class.forName(value).newInstance());
		}

		// Create scheduler
		value = config.get(SCHEDULER_ID + '.' + TYPE, "");
		if (!value.isEmpty()) {
			setScheduler((ScheduledExecutorService) Class.forName(value).newInstance());
		}

		// Should terminate thread pools on stop()?
		value = config.get(SHUT_DOWN_THREAD_POOLS, "");
		if (!value.isEmpty()) {
			shutDownThreadPools = "true".equals(value);
		}

		// Create Component Registry
		value = config.get(COMPONENTS_ID + '.' + TYPE, "");
		if (!value.isEmpty()) {
			String test = value.toLowerCase();
			if (test.equals("standalone")) {
				setComponents(new StandaloneComponentRegistry());
			} else if (test.equals("spring")) {
				setComponents(new SpringComponentRegistry());
			} else if (test.equals("guice")) {
				setComponents(new GuiceComponentRegistry());
			} else {
				setComponents((ComponentRegistry) Class.forName(value).newInstance());
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

}