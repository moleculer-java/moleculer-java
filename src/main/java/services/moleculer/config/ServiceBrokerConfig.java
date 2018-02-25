package services.moleculer.config;

import static services.moleculer.util.CommonUtils.getHostName;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

import services.moleculer.context.ContextFactory;
import services.moleculer.context.DefaultContextFactory;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.service.DefaultServiceRegistry;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.strategy.RoundRobinStrategyFactory;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.uid.IncrementalUIDGenerator;
import services.moleculer.uid.UIDGenerator;

public class ServiceBrokerConfig {

	// --- THREAD POOLS ---

	protected ExecutorService executor;
	protected ScheduledExecutorService scheduler;

	protected boolean shutDownThreadPools = true;

	// --- PROPERTIES ---

	protected String namespace = "";
	protected String nodeID;

	/**
	 * Install internal ($node) services?
	 */
	protected boolean internalServices = true;

	// --- JSON API SERIALIZER / DESERIALIZER ---

	/**
	 * Name of the JSON deserializer API ("jackson", "boon", "builtin", "gson",
	 * "fastjson", "genson", etc., null = autodetect)
	 */
	protected String jsonReader;

	/**
	 * Name of the JSON serializer API ("jackson", "boon", "builtin", "gson",
	 * "fast", "genson", "flex", "nano", etc., null = autodetect)
	 */
	protected String jsonWriter;
	
	// --- INTERNAL COMPONENTS ---

	protected UIDGenerator uidGenerator = new IncrementalUIDGenerator();
	protected StrategyFactory strategyFactory = new RoundRobinStrategyFactory();
	protected ContextFactory contextFactory = new DefaultContextFactory();
	protected Eventbus eventbus = null;
	protected ServiceRegistry serviceRegistry = new DefaultServiceRegistry();
	
	// --- CONSTRUCTORS ---

	public ServiceBrokerConfig() {

		// Create default thread pools
		executor = ForkJoinPool.commonPool();
		scheduler = Executors.newScheduledThreadPool(ForkJoinPool.commonPool().getParallelism());

		// Set the default System Monitor
		// monitor = defaultMonitor;

		// Set the default NodeID
		// nodeID = getHostName() + '-' + monitor.getPID();
		nodeID = getHostName() + '-' + System.currentTimeMillis();
	}
	
	// --- GETTERS AND SETTERS ---
	
	public ExecutorService getExecutor() {
		return executor;
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = Objects.requireNonNull(executor);
	}

	public ScheduledExecutorService getScheduler() {
		return scheduler;
	}

	public void setScheduler(ScheduledExecutorService scheduler) {
		this.scheduler = Objects.requireNonNull(scheduler);
	}

	public boolean isShutDownThreadPools() {
		return shutDownThreadPools;
	}

	public void setShutDownThreadPools(boolean shutDownThreadPools) {
		this.shutDownThreadPools = shutDownThreadPools;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getNodeID() {
		return nodeID;
	}

	public void setNodeID(String nodeID) {
		this.nodeID = nodeID;
	}

	public boolean isInternalServices() {
		return internalServices;
	}

	public void setInternalServices(boolean internalServices) {
		this.internalServices = internalServices;
	}

	public String getJsonReader() {
		return jsonReader;
	}

	public void setJsonReader(String jsonReader) {
		this.jsonReader = jsonReader;
	}

	public String getJsonWriter() {
		return jsonWriter;
	}

	public void setJsonWriter(String jsonWriter) {
		this.jsonWriter = jsonWriter;
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = Objects.requireNonNull(serviceRegistry);
	}

	public StrategyFactory getStrategyFactory() {
		return strategyFactory;
	}

	public void setStrategyFactory(StrategyFactory strategyFactory) {
		this.strategyFactory = Objects.requireNonNull(strategyFactory);
	}

	public UIDGenerator getUidGenerator() {
		return uidGenerator;
	}

	public void setUidGenerator(UIDGenerator uidGenerator) {
		this.uidGenerator = Objects.requireNonNull(uidGenerator);
	}

	public Eventbus getEventbus() {
		return eventbus;
	}

	public void setEventbus(Eventbus eventbus) {
		this.eventbus = Objects.requireNonNull(eventbus);
	}

	public ContextFactory getContextFactory() {
		return contextFactory;
	}

	public void setContextFactory(ContextFactory contextFactory) {
		this.contextFactory = Objects.requireNonNull(contextFactory);
	}	
	
}