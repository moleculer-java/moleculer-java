module.exports = {

	namespace: "dev",
	nodeID: null,

	logger: true,
	logLevel: "info",
	logFormatter: "default",

	transporter: "NATS",
	transporter: new Moleculer.Transporters.NATS({}),
	transporter: "nats://localhost:4222",
	transporter: {
		type: "NATS",
		settings: {}
	},
	
	cacher: new Moleculer.Cachers.Redis({}),
	cacher: "Redis",
	cacher: "redis://redis-server",
	cacher: {
		type: "memory",
		settings: {
			ttl: 60
		}
	},

	serializer: new Moleculer.Serializers.JsonSerializer,
	serializer: "ProtoBuf",

	requestTimeout: 0 * 1000,
	requestRetry: 0,
	maxCallLevel: 0,
	heartbeatInterval: 5,
	heartbeatTimeout: 15,

	disableBalancer: false,

	registry: {
		strategy: Moleculer.Strategies.RoundRobin,
		preferLocal: true				
	},

	circuitBreaker: {
		enabled: false,
		maxFailures: 3,
		halfOpenTime: 10 * 1000,
		failureOnTimeout: true,
		failureOnReject: true
	},

	validation: true,
	validator: null,
	metrics: false,
	metricsRate: 1,
	statistics: false,
	internalActions: true,

	hotReload: false
};