package services.moleculer.config;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import io.datatree.dom.TreeReaderRegistry;
import services.moleculer.cachers.Cacher;
import services.moleculer.cachers.MemoryCacher;
import services.moleculer.context.ContextFactory;
import services.moleculer.context.DefaultContextFactory;
import services.moleculer.eventbus.CachedArrayEventBus;
import services.moleculer.eventbus.EventBus;
import services.moleculer.services.DefaultServiceRegistry;
import services.moleculer.services.ServiceRegistry;
import services.moleculer.strategies.InvocationStrategyFactory;
import services.moleculer.strategies.RoundRobinInvocationStrategyFactory;
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

	// --- PROPERTIES AND COMPONENTS ---

	private String namespace = "";
	private String nodeID;

	private Executor executor = ForkJoinPool.commonPool();
	private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	private Tree config = new Tree();

	private ContextFactory contextFactory = new DefaultContextFactory();
	private ServiceRegistry serviceRegistry = new DefaultServiceRegistry();
	private EventBus eventBus = new CachedArrayEventBus();
	private UIDGenerator uidGenerator = new TimeSequenceUIDGenerator();
	private InvocationStrategyFactory invocationStrategyFactory = new RoundRobinInvocationStrategyFactory();
	private Transporter transporter;
	private Cacher cacher = new MemoryCacher();

	private final LinkedHashMap<String, MoleculerComponentContainer> components = new LinkedHashMap<>();

	// --- CONSTRUCTORS ---

	public ServiceBrokerConfig() {
		try {
			nodeID = InetAddress.getLocalHost().getHostName();
		} catch (Exception ignored) {
		}
		if (nodeID == null || nodeID.isEmpty()) {
			nodeID = "default";
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
			loadConfig(new URL(configPath).openStream(), format);
			return;
		}
		URL url = getClass().getResource(configPath);
		if (url != null) {
			loadConfig(url.openStream(), format);
			return;
		}
		File file = new File(configPath);
		if (file.isFile()) {
			loadConfig(new FileInputStream(file), format);
			return;
		}
		throw new IllegalArgumentException("Resource \"" + configPath + "\" not found!");
	}

	// --- INTERNAL SETTERS ---

	private final void applyConfiguration() throws Exception {

		// Set base proeprties
		setNamespace(config.get("namespace", namespace));
		setNodeID(config.get("nodeID", nodeID));

		// Create executor
		String clazz = config.get("executor", "");
		if (!clazz.isEmpty()) {
			executor = (Executor) Class.forName(clazz).newInstance();
		}

		// Create scheduler
		clazz = config.get("scheduler", "");
		if (!clazz.isEmpty()) {
			scheduler = (ScheduledExecutorService) Class.forName(clazz).newInstance();
		}

	}

	// --- GETTERS AND SETTERS ---

	public final Map<String, MoleculerComponentContainer> getComponents() {
		return components;
	}

	public final void setComponents(Map<String, MoleculerComponentContainer> components) {
		Objects.nonNull(components);
		components.clear();
		components.putAll(components);
	}

	public final String getNamespace() {
		return namespace;
	}

	public final void setNamespace(String namespace) {
		Objects.nonNull(namespace);
		this.namespace = namespace;
	}

	public final String getNodeID() {
		return nodeID;
	}

	public final void setNodeID(String nodeID) {
		Objects.nonNull(nodeID);
		nodeID = nodeID.trim();
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
		Objects.nonNull(eventBus);
		this.eventBus = eventBus;
	}

	public final UIDGenerator getUIDGenerator() {
		return uidGenerator;
	}

	public final void setUIDGenerator(UIDGenerator uidGenerator) {
		Objects.nonNull(uidGenerator);
		this.uidGenerator = uidGenerator;
	}

	public final ContextFactory getContextFactory() {
		return contextFactory;
	}

	public final void setContextFactory(ContextFactory contextFactory) {
		Objects.nonNull(contextFactory);
		this.contextFactory = contextFactory;
	}

	public final InvocationStrategyFactory getInvocationStrategyFactory() {
		return invocationStrategyFactory;
	}

	public final void setInvocationStrategyFactory(InvocationStrategyFactory invocationStrategyFactory) {
		Objects.nonNull(invocationStrategyFactory);
		this.invocationStrategyFactory = invocationStrategyFactory;
	}

	public final Executor getExecutor() {
		return executor;
	}

	public final void setExecutor(Executor executor) {
		Objects.nonNull(executor);
		this.executor = executor;
	}

	public final ScheduledExecutorService getScheduler() {
		return scheduler;
	}

	public final void setScheduler(ScheduledExecutorService scheduler) {
		Objects.nonNull(scheduler);
		this.scheduler = scheduler;
	}

	public final Tree getConfig() {
		return config;
	}

	public final void setConfig(Tree config) {
		Objects.nonNull(config);
		this.config = config;
		try {
			applyConfiguration();
		} catch (Exception cause) {
			throw new IllegalArgumentException("Invalid configuration!", cause);
		}
	}

	// --- PRIVATE UTILITIES ---

	private final void loadConfig(InputStream in, String format) throws Exception {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int length;
			while ((length = in.read(buffer)) != -1) {
				bytes.write(buffer, 0, length);
			}
			config = new Tree(bytes.toByteArray(), format);
		} finally {
			if (in != null) {
				in.close();
			}
		}
		applyConfiguration();
		logger.info("Configuration loaded successfully.");
	}

	private static final String getFormat(String path) {
		path = path.toLowerCase();
		int i = path.lastIndexOf('.');
		if (i > 0) {
			String format = path.substring(i + 1);
			try {

				// Is format valid?
				TreeReaderRegistry.getReader(format);
			} catch (Exception notSupported) {

				// JSON
				return null;
			}
		}

		// JSON
		return null;
	}

}