package services.moleculer.config;

import services.moleculer.CircuitBreaker;
import services.moleculer.ServiceBroker;
import services.moleculer.cachers.Cacher;
import services.moleculer.logger.LoggerFactory;
import services.moleculer.strategies.InvocationStrategyFactory;
import services.moleculer.transporters.Transporter;

/**
 * Builder-style ServiceBroker factory. Sample of usage:<br>
 * <br>
 * ServiceBroker broker = ServiceBrokerBuilder.newBuilder().cacher(cacher).build();
 */
public class ServiceBrokerBuilder {

	// --- CONFIGURATION ---

	private final ServiceBrokerConfig config;

	// --- STATIC CONSTRUCTOR ---

	public static final ServiceBrokerBuilder newBuilder() {
		return new ServiceBrokerBuilder(new ServiceBrokerConfig());
	}

	// --- PRIVATE CONSTRUCTOR ---

	private ServiceBrokerBuilder(ServiceBrokerConfig config) {
		this.config = config;
	}

	// --- BUILD METHOD ---

	public final ServiceBroker build() {
		return new ServiceBroker(config);
	}

	// --- SETTER METHODS ---

	public final ServiceBrokerBuilder namespace(String namespace) {
		config.setNamespace(namespace);
		return this;
	}

	public final ServiceBrokerBuilder nodeID(String nodeID) {
		config.setNodeID(nodeID);
		return this;
	}

	public final ServiceBrokerBuilder loggerFactory(LoggerFactory loggerFactory) {
		config.setLoggerFactory(loggerFactory);
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

	public final ServiceBrokerBuilder strategyFactory(InvocationStrategyFactory strategyFactory) {
		config.setStrategyFactory(strategyFactory);
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