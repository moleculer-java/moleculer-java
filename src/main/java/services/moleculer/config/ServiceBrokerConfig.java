package services.moleculer.config;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import co.paralleluniverse.fibers.FiberExecutorScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.Instrumented;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;
import services.moleculer.breakers.CircuitBreaker;
import services.moleculer.cachers.Cacher;
import services.moleculer.cachers.MemoryCacher;
import services.moleculer.context.ContextPool;
import services.moleculer.context.ThreadBasedContextPool;
import services.moleculer.eventbus.CachedArrayEventBus;
import services.moleculer.eventbus.EventBus;
import services.moleculer.fibers.SchedulerFactory;
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

	// --- STATIC PROPERTIES ---
	
	// Scheduler factory
	// -Dmoleculer.scheduler.factory=
	public static final SchedulerFactory SCHEDULER_FACTORY;

	// Enable lightweight threads (or use "heavyweight" native threads)
	// -Dmoleculer.agent.enabled=true
	public static final boolean AGENT_ENABLED;

	// Redirect logging events from lightweight threads into the heavyweight
	// thread pool
	// -Dmoleculer.async.logging.enabled=true
	public static final boolean ASYNC_LOGGING_ENABLED;
	
	// --- PROPERTIES AND COMPONENTS ---

	private String namespace = "";
	private String nodeID;

	private ContextPool contextPool = new ThreadBasedContextPool();
	private ServiceRegistry serviceRegistry = new DefaultServiceRegistry();
	private EventBus eventBus = new CachedArrayEventBus();
	private UIDGenerator uidGenerator = new TimeSequenceUIDGenerator();
	private InvocationStrategyFactory invocationStrategyFactory = new RoundRobinInvocationStrategyFactory();
		
	private Transporter transporter;

	private long requestTimeout;
	private int requestRetry;
	private int maxCallLevel = 100;
	private int heartbeatInterval = 5;
	private int heartbeatTimeout = 15;

	private boolean disableBalancer;

	private InvocationStrategyFactory strategyFactory = new RoundRobinInvocationStrategyFactory();
	private boolean preferLocal = true;

	private CircuitBreaker circuitBreaker = new CircuitBreaker();

	private Cacher cacher = new MemoryCacher();
	private String serializer = "json";

	// validation: true,
	// validator: null,
	// metrics: false,
	// metricsRate: 1,
	// statistics: false,
	// internalServices: true,

	// ServiceFactory: null,
	// ContextPool: null

	// --- STATIC CONSTRUCTOR ---
	
	// --- INIT PROPERTIES ---

	static {

		// Create faster scheduler for the suspendable non-blocking tasks, and a
		// scheduler for blocking tasks (eg. network and filesystem I/O tasks)
		String className = System.getProperty("moleculer.scheduler.factory");
		SchedulerFactory factory = null;
		if (className != null) {
			try {
				ClassLoader loader = Thread.currentThread().getContextClassLoader();
				factory = (SchedulerFactory) loader.loadClass(className).newInstance();
			} catch (Throwable cause) {
				cause.printStackTrace();
			}
		}
		if (factory == null) {
			SCHEDULER_FACTORY = new SchedulerFactory() {

				private final ExecutorService forkJoinPool = ForkJoinPool.commonPool();
				
				@Override
				public final FiberScheduler createNonBlockingScheduler() {
					return new FiberExecutorScheduler("fibers", forkJoinPool);
				}

				@Override
				public final ExecutorService createBlockingExecutor() {
					return forkJoinPool;
				}

			};
		} else {
			SCHEDULER_FACTORY = factory;
		}

		// Is Quasar Agent enabled and running?
		boolean agentEnabled = Boolean.parseBoolean(System.getProperty("moleculer.agent.enabled", "true"));
		if (agentEnabled) {
			agentEnabled = FiberTest.class.isAnnotationPresent(Instrumented.class);
		}
		AGENT_ENABLED = agentEnabled;

		// Logger never blocks lightweight thread pool
		ASYNC_LOGGING_ENABLED = Boolean.parseBoolean(System.getProperty("moleculer.async.logging.enabled", "false"));
	}

	private static final class FiberTest implements SuspendableCallable<Void> {

		private static final long serialVersionUID = 1L;

		@Override
		public Void run() throws SuspendExecution, InterruptedException {
			return null;
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

	public final InvocationStrategyFactory getStrategyFactory() {
		return strategyFactory;
	}

	public final void setStrategyFactory(InvocationStrategyFactory strategyFactory) {
		this.strategyFactory = strategyFactory;
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

}