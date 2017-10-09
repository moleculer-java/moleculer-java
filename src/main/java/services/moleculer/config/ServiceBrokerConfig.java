package services.moleculer.config;

import java.net.InetAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import services.moleculer.breakers.CircuitBreaker;
import services.moleculer.cachers.Cacher;
import services.moleculer.cachers.MemoryCacher;
import services.moleculer.context.ContextPool;
import services.moleculer.context.ThreadBasedContextPool;
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

	// --- PROPERTIES AND COMPONENTS ---

	private String namespace = "";
	private String nodeID;

	private Executor executor = ForkJoinPool.commonPool();
	private ContextPool contextPool = new ThreadBasedContextPool();
	private ServiceRegistry serviceRegistry = new DefaultServiceRegistry();
	private EventBus eventBus = new CachedArrayEventBus();
	private UIDGenerator uidGenerator = new TimeSequenceUIDGenerator();
	private InvocationStrategyFactory invocationStrategyFactory = new RoundRobinInvocationStrategyFactory();
	private Transporter transporter;

	private boolean preferLocal = true;
	
	private long requestTimeout;
	private int requestRetry;
	private int maxCallLevel = 100;
	private int heartbeatInterval = 5;
	private int heartbeatTimeout = 15;

	private boolean disableBalancer;

	private CircuitBreaker circuitBreaker = new CircuitBreaker();

	private Cacher cacher = new MemoryCacher();
	private String serializer = "json";

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

	// --- GETTERS AND SETTERS ---

	public final String getNamespace() {
		return namespace;
	}

	public final void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public final String getNodeID() {
		return nodeID;
	}

	public final void setNodeID(String nodeID) {
		this.nodeID = nodeID;
	}

	public final Transporter getTransporter() {
		return transporter;
	}

	public final void setTransporter(Transporter transporter) {
		this.transporter = transporter;
	}

	public final long getRequestTimeout() {
		return requestTimeout;
	}

	public final void setRequestTimeout(long requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public final int getRequestRetry() {
		return requestRetry;
	}

	public final void setRequestRetry(int requestRetry) {
		this.requestRetry = requestRetry;
	}

	public final int getMaxCallLevel() {
		return maxCallLevel;
	}

	public final void setMaxCallLevel(int maxCallLevel) {
		this.maxCallLevel = maxCallLevel;
	}

	public final int getHeartbeatInterval() {
		return heartbeatInterval;
	}

	public final void setHeartbeatInterval(int heartbeatInterval) {
		this.heartbeatInterval = heartbeatInterval;
	}

	public final int getHeartbeatTimeout() {
		return heartbeatTimeout;
	}

	public final void setHeartbeatTimeout(int heartbeatTimeout) {
		this.heartbeatTimeout = heartbeatTimeout;
	}

	public final boolean isDisableBalancer() {
		return disableBalancer;
	}

	public final void setDisableBalancer(boolean disableBalancer) {
		this.disableBalancer = disableBalancer;
	}

	public final boolean isPreferLocal() {
		return preferLocal;
	}

	public final void setPreferLocal(boolean preferLocal) {
		this.preferLocal = preferLocal;
	}

	public final CircuitBreaker getCircuitBreaker() {
		return circuitBreaker;
	}

	public final void setCircuitBreaker(CircuitBreaker circuitBreaker) {
		this.circuitBreaker = circuitBreaker;
	}

	public final Cacher getCacher() {
		return cacher;
	}

	public final void setCacher(Cacher cacher) {
		this.cacher = cacher;
	}

	public final String getSerializer() {
		return serializer;
	}

	public final void setSerializer(String serializer) {
		this.serializer = serializer;
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
		this.eventBus = eventBus;
	}

	public final UIDGenerator getUIDGenerator() {
		return uidGenerator;
	}

	public final void setUIDGenerator(UIDGenerator uidGenerator) {
		this.uidGenerator = uidGenerator;
	}

	public final ContextPool getContextPool() {
		return contextPool;
	}

	public final void setContextPool(ContextPool contextPool) {
		this.contextPool = contextPool;
	}

	public final InvocationStrategyFactory getInvocationStrategyFactory() {
		return invocationStrategyFactory;
	}

	public final void setInvocationStrategyFactory(InvocationStrategyFactory invocationStrategyFactory) {
		this.invocationStrategyFactory = invocationStrategyFactory;
	}

	public final Executor getExecutor() {
		return executor;
	}

	public final void setExecutor(Executor executor) {
		this.executor = executor;
	}

}