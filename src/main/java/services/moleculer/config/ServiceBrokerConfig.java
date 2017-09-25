package services.moleculer.config;

import services.moleculer.CircuitBreaker;
import services.moleculer.cachers.Cacher;
import services.moleculer.logger.LoggerFactory;
import services.moleculer.strategies.InvocationStrategyFactory;
import services.moleculer.transporters.Transporter;

/**
 * POJO-style ServiceBroker factory (eg. for Spring Framework). Sample of usage:<br>
 * <br>
 * ServiceBrokerConfig config = new ServiceBrokerConfig();<br>
 * config.setCacher(cacher);<br>
 * ServiceBroker broker = new ServiceBroker(config);
 */
public final class ServiceBrokerConfig {

	// --- PROPERTIES AND COMPONENTS ---

	private String namespace = "";
	private String nodeID;

	private LoggerFactory loggerFactory;

	private Transporter transporter;
	private long requestTimeout;
	private int requestRetry;
	private int maxCallLevel = 100;
	private int heartbeatInterval = 5;
	private int heartbeatTimeout = 15;

	private boolean disableBalancer;

	private InvocationStrategyFactory strategyFactory;
	private boolean preferLocal;

	private CircuitBreaker circuitBreaker;

	private Cacher cacher;
	private String serializer;

	// validation: true,
	// validator: null,
	// metrics: false,
	// metricsRate: 1,
	// statistics: false,
	// internalServices: true,

	// ServiceFactory: null,
	// ContextFactory: null

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

	public final LoggerFactory getLoggerFactory() {
		return loggerFactory;
	}

	public final void setLoggerFactory(LoggerFactory loggerFactory) {
		this.loggerFactory = loggerFactory;
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

}