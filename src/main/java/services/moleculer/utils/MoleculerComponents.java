package services.moleculer.utils;

import java.util.concurrent.ExecutorService;

import services.moleculer.cachers.Cacher;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.ContextPool;
import services.moleculer.eventbus.EventBus;
import services.moleculer.services.ServiceRegistry;
import services.moleculer.strategies.InvocationStrategyFactory;
import services.moleculer.transporters.Transporter;
import services.moleculer.uids.UIDGenerator;

public class MoleculerComponents {

	// --- INTERNAL COMPONENTS ---

	private final ExecutorService executorService;
	private final ContextPool contextPool;
	private final UIDGenerator uidGenerator;
	private final InvocationStrategyFactory invocationStrategyFactory;
	private final ServiceRegistry serviceRegistry;
	private final Cacher cacher;
	private final EventBus eventBus;
	private final Transporter transporter;
	
	// --- CONSTRUCTOR ---
	
	public MoleculerComponents(ServiceBrokerConfig config) {

		// Set components
		executorService = config.getExecutorService();
		contextPool = config.getContextPool();
		uidGenerator = config.getUIDGenerator();
		invocationStrategyFactory = config.getInvocationStrategyFactory();
		serviceRegistry = config.getServiceRegistry();
		cacher = config.getCacher();
		eventBus = config.getEventBus();
		transporter = config.getTransporter();
	}
	
	// --- GET COMPONENTS ---

	public final ExecutorService executorService() {
		return executorService;
	}

	public final ContextPool contextPool() {
		return contextPool;
	}

	public final UIDGenerator uidGenerator() {
		return uidGenerator;
	}

	public final InvocationStrategyFactory invocationStrategyFactory() {
		return invocationStrategyFactory;
	}

	public final ServiceRegistry serviceRegistry() {
		return serviceRegistry;
	}

	public final Cacher cacher() {
		return cacher;
	}

	public final EventBus eventBus() {
		return eventBus;
	}

	public final Transporter transporter() {
		return transporter;
	}
	
}
