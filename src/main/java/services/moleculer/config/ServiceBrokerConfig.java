package services.moleculer.config;

import static services.moleculer.utils.CommonUtils.getFormat;
import static services.moleculer.utils.CommonUtils.readTree;
import static services.moleculer.utils.CommonUtils.getProperty;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import io.datatree.dom.TreeReaderRegistry;
import services.moleculer.cachers.Cacher;
import services.moleculer.cachers.MemoryCacher;
import services.moleculer.context.ContextFactory;
import services.moleculer.context.DefaultContextFactory;
import services.moleculer.eventbus.DefaultEventBus;
import services.moleculer.eventbus.EventBus;
import services.moleculer.services.DefaultServiceRegistry;
import services.moleculer.services.ServiceRegistry;
import services.moleculer.strategies.StrategyFactory;
import services.moleculer.strategies.RoundRobinStrategyFactory;
import services.moleculer.transporters.Transporter;
import services.moleculer.uids.TimeSequenceUIDGenerator;
import services.moleculer.uids.UIDGenerator;

/**
 * POJO-style ServiceBroker factory (eg. for Spring Framework). Sample of usage:
 * <br>
 * <br>
 * ServiceBrokerConfig config = new ServiceBrokerConfig();<br>
 * config.setCacher(cacher);<br>
 * ServiceBroker broker = new ServiceBroker(config);
 */
public final class ServiceBrokerConfig {

	// --- LOGGER ---

	private static final Logger logger = LoggerFactory.getLogger(ServiceBrokerConfig.class);

	// --- THREAD POOLS ---
		
	private ExecutorService executor;
	private ScheduledExecutorService scheduler;
	
	private boolean shutDownThreadPools = true;

	// --- PROPERTIES AND COMPONENTS ---

	private String namespace = "";
	private String nodeID;

	private ComponentRegistry componentRegistry = new StandaloneComponentRegistry();
	private Tree config = new Tree();

	private ContextFactory contextFactory = new DefaultContextFactory();
	private ServiceRegistry serviceRegistry = new DefaultServiceRegistry();
	private EventBus eventBus = new DefaultEventBus();
	private UIDGenerator uidGenerator = new TimeSequenceUIDGenerator();
	private StrategyFactory strategyFactory = new RoundRobinStrategyFactory();
	private Transporter transporter;
	private Cacher cacher = new MemoryCacher();

	private final LinkedHashMap<String, MoleculerComponentContainer> components = new LinkedHashMap<>();

	// --- INSTALL JS PARSER ---
	
	static {
		try {
			TreeReaderRegistry.getReader("js");
		} catch (Exception ignored) {
			TreeReaderRegistry.setReader("js", new JSReader());
		}
	}
	
	// --- CONSTRUCTORS ---

	public ServiceBrokerConfig() {		
		try {
			nodeID = InetAddress.getLocalHost().getHostName();
		} catch (Exception ignored) {
		}
		if (nodeID == null || nodeID.isEmpty()) {
			nodeID = "default";
		}
		Object nettyExecutor = null;
		try {
			nettyExecutor = Class.forName("io.netty.channel.nio.NioEventLoopGroup").newInstance();
		} catch (Throwable ignored) {
		}
		if (nettyExecutor == null) {
			executor = Executors.newWorkStealingPool();
			scheduler = Executors.newSingleThreadScheduledExecutor();
		} else {
			executor = (ExecutorService) nettyExecutor;
			scheduler = (ScheduledExecutorService) nettyExecutor;
		}
	}

	public ServiceBrokerConfig(String nodeID, Transporter transporter, Cacher cacher) {
		setNodeID(nodeID);
		setTransporter(transporter);
		setCacher(cacher);
	}

	public ServiceBrokerConfig(String configPath) throws Exception {
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
		setNamespace(getProperty(config, "namespace", namespace).asString());
		setNodeID(getProperty(config, "nodeID", nodeID).asString());

		// Create executor
		String value = config.get("executor.class", "");
		if (!value.isEmpty()) {
			setExecutor((ExecutorService) Class.forName(value).newInstance());
		}

		// Create scheduler
		value = config.get("scheduler.class", "");
		if (!value.isEmpty()) {
			setScheduler((ScheduledExecutorService) Class.forName(value).newInstance());
		}
		
		// Should terminate thread pools on stop()?
		value = config.get("shutDownThreadPools", "");
		if (!value.isEmpty()) {
			shutDownThreadPools = "true".equals(value);
		}
		
		// Create component registry
		value = config.get("componentRegistry.class", "");
		if (!value.isEmpty()) {
			setComponentRegistry((ComponentRegistry) Class.forName(value).newInstance());
		}
	}

	// --- GETTERS AND SETTERS ---

	public final Map<String, MoleculerComponentContainer> getComponents() {
		return components;
	}

	public final void setComponents(Map<String, MoleculerComponentContainer> components) {
		Objects.requireNonNull(components);
		components.clear();
		components.putAll(components);
	}

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
		nodeID = Objects.requireNonNull(nodeID).trim();
		if (nodeID.isEmpty()) {
			throw new IllegalArgumentException("Empty nodeID is not allowed!");
		}
		this.nodeID = nodeID;
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

	public final ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public final void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public final EventBus getEventBus() {
		return eventBus;
	}

	public final void setEventBus(EventBus eventBus) {
		this.eventBus = Objects.requireNonNull(eventBus);
	}

	public final UIDGenerator getUidGenerator() {
		return uidGenerator;
	}

	public final void setUidGenerator(UIDGenerator uidGenerator) {
		this.uidGenerator = Objects.requireNonNull(uidGenerator);
	}

	public final ContextFactory getContextFactory() {
		return contextFactory;
	}

	public final void setContextFactory(ContextFactory contextFactory) {
		this.contextFactory = Objects.requireNonNull(contextFactory);
	}

	public final StrategyFactory getStrategyFactory() {
		return strategyFactory;
	}

	public final void setStrategyFactory(StrategyFactory strategyFactory) {
		this.strategyFactory = Objects.requireNonNull(strategyFactory);
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

	public final ComponentRegistry getComponentRegistry() {
		return componentRegistry;
	}

	public final void setComponentRegistry(ComponentRegistry componentRegistry) {
		this.componentRegistry = Objects.requireNonNull(componentRegistry);
	}

	public final boolean getShutDownThreadPools() {
		return shutDownThreadPools;
	}

	public final void setShutDownThreadPools(boolean shutDownThreadPools) {
		this.shutDownThreadPools = shutDownThreadPools;
	}
	
}