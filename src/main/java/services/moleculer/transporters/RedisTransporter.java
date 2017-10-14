package services.moleculer.transporters;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.connection.ConnectionActivatedEvent;
import com.lambdaworks.redis.event.connection.ConnectionDeactivatedEvent;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import com.lambdaworks.redis.resource.DefaultClientResources;

import io.datatree.Tree;
import rx.Observable;
import services.moleculer.ServiceBroker;
import services.moleculer.services.Name;
import services.moleculer.services.ServiceRegistry;
import services.moleculer.utils.RedisUtilities;
import services.moleculer.utils.Serializer;

@Name("Redis Transporter")
public final class RedisTransporter extends Transporter {

	// --- VARIABLES ---

	protected String[] urls = new String[] { "localhost" };
	protected String password;
	protected boolean useSSL;
	protected boolean startTLS;

	protected StatefulRedisPubSubConnection<byte[], byte[]> clientSub;
	protected RedisPubSubAsyncCommands<byte[], byte[]> clientPub;

	protected ServiceRegistry serviceRegistry;

	// --- CONSTUCTORS ---

	public RedisTransporter() {
		super();
	}

	public RedisTransporter(String prefix) {
		super(prefix);
	}

	public RedisTransporter(String prefix, String... urls) {
		super(prefix);
		this.urls = urls;
	}

	public RedisTransporter(String prefix, boolean useSSL, boolean startTLS, String... urls) {
		super(prefix);
		this.useSSL = useSSL;
		this.startTLS = startTLS;
		this.urls = urls;
	}

	public RedisTransporter(String prefix, StatefulRedisPubSubConnection<byte[], byte[]> clientSub) {
		super(prefix);
		this.clientSub = clientSub;
	}

	// --- START TRANSPORTER ---

	/**
	 * Initializes transporter instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public final void start(ServiceBroker broker, Tree config) throws Exception {

		// Process basic properties (eg. "prefix")
		super.start(broker, config);

		// Process config
		Tree urlNode = config.get("urls");
		if (urlNode == null) {
			urlNode = config.get("url");
		}
		if (urlNode != null) {
			List<String> urlList;
			if (urlNode.isPrimitive()) {
				urlList = new LinkedList<>();
				String url = urlNode.asString().trim();
				if (!url.isEmpty()) {
					urlList.add(url);
				}
			} else {
				urlList = urlNode.asList(String.class);
			}
			if (!urlList.isEmpty()) {
				urls = new String[urlList.size()];
				urlList.toArray(urls);
			}
		}
		password = config.get("password", password);
		useSSL = config.get("useSSL", useSSL);
		startTLS = config.get("startTLS", startTLS);
		
		// Get the common executor
		final Executor executor = broker.components().executor();

		// Get the service registry
		this.serviceRegistry = broker.components().serviceRegistry();

		// Init Redis client
		if (clientSub == null) {

			// Create Redis connection
			final List<RedisURI> redisURIs = RedisUtilities.parseURLs(urls, password, useSSL, startTLS);
			final RedisTransporter self = this;
			DefaultClientResources clientResources = RedisUtilities.createClientResources(new EventBus() {

				@Override
				public final void publish(Event event) {

					// Connected to Redis server
					if (event instanceof ConnectionActivatedEvent) {
						ConnectionActivatedEvent e = (ConnectionActivatedEvent) event;
						logger.info("Redis transporter connected to " + e.remoteAddress() + ".");
						executor.execute(() -> {
							try {
								self.connected();
							} catch (Exception cause) {
								logger.error("Unexpected error occured in \"connected\" method!", cause);
							}
						});
						return;
					}

					// Disconnected from Redis server
					if (event instanceof ConnectionDeactivatedEvent) {
						ConnectionDeactivatedEvent e = (ConnectionDeactivatedEvent) event;
						logger.info("Redis transporter disconnected from " + e.remoteAddress() + ".");
						try {
							self.disconnected();
						} catch (Exception cause) {
							logger.error("Unexpected error occured in \"disconnected\" method!", cause);
						}
						return;
					}
				}

				@Override
				public final Observable<Event> get() {
					return null;
				}

			});
			if (urls.length > 1) {

				// Clustered client
				RedisClusterClient client = RedisClusterClient.create(clientResources, redisURIs);
				clientSub = client.connectPubSub(new ByteArrayCodec());

			} else {

				// Single connection
				RedisClient client = RedisClient.create(clientResources, redisURIs.get(0));
				clientSub = client.connectPubSub(new ByteArrayCodec());

			}

			// Add listener
			clientSub.addListener(new RedisPubSubListener<byte[], byte[]>() {

				@Override
				public final void message(byte[] pattern, byte[] channel, byte[] message) {
					message(channel, message);
				}

				@Override
				public final void message(byte[] channel, byte[] message) {
					executor.execute(() -> {
						try {
							Object data = Serializer.deserialize(message, format);
							if (!(data instanceof Tree)) {
								logger.warn("Invalid message received from Redis (\"" + data + "\")!");
								return;
							}
							serviceRegistry.receive((Tree) data);

							String nameOfChannel = new String(channel, StandardCharsets.UTF_8);

							System.out.println("message: " + Thread.currentThread());
							System.out.println(nameOfChannel + " -> " + data);
						} catch (Exception cause) {
							logger.warn("Unable to parse Redis message!", cause);
						}
					});
				}

				@Override
				public final void subscribed(byte[] channel, long count) {
					logger.info("Redis channel \"" + new String(channel, StandardCharsets.UTF_8) + "\" subscribed.");
				}

				@Override
				public final void psubscribed(byte[] pattern, long count) {
				}

				@Override
				public final void unsubscribed(byte[] channel, long count) {
				}

				@Override
				public final void punsubscribed(byte[] pattern, long count) {
				}

			});
		}
		clientPub = clientSub.async();
	}

	// --- STOP TRANSPORTER ---

	/**
	 * Closes transporter.
	 */
	@Override
	public final void stop() {
		clientPub = null;
		if (clientSub != null) {
			clientSub.close();
			clientSub = null;
		}
	}

	// --- SUBSCRIBE ---

	@Override
	public final void subscribe(String cmd, String nodeID) {
		if (clientPub != null) {
			String channel = nameOfChannel(cmd, nodeID);
			clientPub.subscribe(channel.getBytes(StandardCharsets.UTF_8));
		}
	}

	// --- PUBLISH ---

	@Override
	public final void publish(String cmd, String nodeID, Tree message) {
		if (clientPub != null) {

			// Serialize data
			byte[] channel = nameOfChannel(cmd, nodeID).getBytes(StandardCharsets.UTF_8);
			byte[] bytes = Serializer.serialize(message, format);

			// Send in JSON / MessagePack / etc. format
			clientPub.publish(channel, bytes);
		}
	}

	// --- IS CONNECTED ---

	public final boolean isConnected() {
		if (clientPub != null && clientSub != null) {
			try {
				return clientPub.isOpen() && clientSub.isOpen();
			} catch (Exception ignored) {
			}
		}
		return false;
	}

}