package services.moleculer.config;

import java.util.concurrent.ExecutorService;

import services.moleculer.ServiceBroker;
import services.moleculer.breakers.CircuitBreaker;
import services.moleculer.cachers.Cacher;
import services.moleculer.context.ContextPool;
import services.moleculer.eventbus.EventBus;
import services.moleculer.services.ServiceRegistry;
import services.moleculer.strategies.InvocationStrategyFactory;
import services.moleculer.transporters.Transporter;
import services.moleculer.uids.UIDGenerator;

/**
 * Builder-style ServiceBroker factory. Sample of usage:<br>
 * <br>
 * ServiceBroker broker = ServiceBrokerBuilder.newBuilder().cacher(cacher).build();
 */
public class ServiceBrokerBuilder {

	// --- CONFIGURATION ---

	private final ServiceBrokerConfig config;

	// --- CONSTRUCTOR ---

	public ServiceBrokerBuilder(ServiceBrokerConfig config) {
		this.config = config;
	}

	// --- BUILD METHOD ---

	public final ServiceBroker build() {
		return new ServiceBroker(config);
	}

	// --- SETTER METHODS ---

	public final ServiceBrokerBuilder executorService(ExecutorService executorService) {
		config.setExecutorService(executorService);
		return this;
	}
	
	public final ServiceBrokerBuilder contextPool(ContextPool contextPool) {
		config.setContextPool(contextPool);
		return this;		
	}
	
	public final ServiceBrokerBuilder serviceRegistry(ServiceRegistry serviceRegistry) {
		config.setServiceRegistry(serviceRegistry);
		return this;				
	}
	
	public final ServiceBrokerBuilder eventBus(EventBus eventBus) {
		config.setEventBus(eventBus);
		return this;						
	}
	
	public final ServiceBrokerBuilder uidGenerator(UIDGenerator uidGenerator) {
		config.setUIDGenerator(uidGenerator);
		return this;								
	}
	
	public final ServiceBrokerBuilder invocationStrategyFactory(InvocationStrategyFactory invocationStrategyFactory) {
		config.setInvocationStrategyFactory(invocationStrategyFactory);
		return this;										
	}
	
	public final ServiceBrokerBuilder namespace(String namespace) {
		config.setNamespace(namespace);
		return this;
	}

	public final ServiceBrokerBuilder nodeID(String nodeID) {
		config.setNodeID(nodeID);
		return this;
	}

	public final ServiceBrokerBuilder transporter(Transporter transporter) {
		config.setTransporter(transporter);
		return this;
	}

	public final ServiceBrokerBuilder requestTimeout(long requestTimeout) {
		config.setRequestTimeout(requestTimeout);
		return this;
	}

	public final ServiceBrokerBuilder requestRetry(int requestRetry) {
		config.setRequestRetry(requestRetry);
		return this;
	}

	public final ServiceBrokerBuilder maxCallLevel(int maxCallLevel) {
		config.setMaxCallLevel(maxCallLevel);
		return this;
	}

	public final ServiceBrokerBuilder heartbeatInterval(int heartbeatInterval) {
		config.setHeartbeatInterval(heartbeatInterval);
		return this;
	}

	public final ServiceBrokerBuilder heartbeatTimeout(int heartbeatTimeout) {
		config.setHeartbeatTimeout(heartbeatTimeout);
		return this;
	}

	public final ServiceBrokerBuilder disableBalancer(boolean disableBalancer) {
		config.setDisableBalancer(disableBalancer);
		return this;
	}

	public final ServiceBrokerBuilder preferLocal(boolean preferLocal) {
		config.setPreferLocal(preferLocal);
		return this;
	}

	public final ServiceBrokerBuilder circuitBreaker(CircuitBreaker circuitBreaker) {
		config.setCircuitBreaker(circuitBreaker);
		return this;
	}

	public final ServiceBrokerBuilder cacher(Cacher cacher) {
		config.setCacher(cacher);
		return this;
	}

	public final ServiceBrokerBuilder serializer(String serializer) {
		config.setSerializer(serializer);
		return this;
	}

}