package services.moleculer.transporters;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
import services.moleculer.utils.RedisUtilities;
import services.moleculer.utils.Serializer;

public final class RedisTransporter extends Transporter {

	// --- NAME OF THE MOLECULER COMPONENT ---

	@Override
	public final String name() {
		return "Redis Transporter";
	}

	// --- VARIABLES ---

	protected String[] urls = new String[] { "localhost" };
	protected String password;
	protected boolean useSSL;
	protected boolean startTLS;

	protected StatefulRedisPubSubConnection<byte[], byte[]> clientSub;
	protected RedisPubSubAsyncCommands<byte[], byte[]> clientPub;

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
	 */
	@Override
	public void init(ServiceBroker broker) throws Exception {
		
		// Init superclass
		super.init(broker);
		
		// Init Redis client
		if (clientSub == null) {

			// Create Redis connection
			final List<RedisURI> redisURIs = RedisUtilities.parseURLs(urls, password, useSSL, startTLS);
			final RedisTransporter self = this;
			DefaultClientResources clientResources = RedisUtilities.createClientResources(new EventBus() {

				@Override
				public final void publish(Event event) {

					// Redis server connected
					if (event instanceof ConnectionActivatedEvent) {
						ConnectionActivatedEvent e = (ConnectionActivatedEvent) event;
						logger.info("Redis server connected (" + e.remoteAddress() + ").");
						try {
							self.connected();
						} catch (Exception cause) {
							logger.error("Unexpected error occured in \"connected\" method!", cause);
						}
						return;
					}

					// Redis server disconnected
					if (event instanceof ConnectionDeactivatedEvent) {
						ConnectionDeactivatedEvent e = (ConnectionDeactivatedEvent) event;
						logger.info("Redis server disconnected (" + e.remoteAddress() + ").");
						try {
							self.disconnected();
						} catch (Exception cause) {
							logger.error("Unexpected error occured in \"disconnected\" method!", cause);
						}
						return;
					}
					
					System.out.println(event);

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
					try {

						// TODO Parse packet (move to superclass)

						/*
						 * 1. Deserialize data 2. Check sender !== this.nodeID
						 * 3. if PACKET_REQUEST call `this.requestHandler` 4. if
						 * PACKET_RESPONSE call `this.responseHandler` 5. if
						 * PACKET_EVENT call `this.broker.emitLocal` 6. if
						 * PACKET_INFO || PACKET_DISCOVER call
						 * `this.processNodeInfo` 7. if PACKET_DISCONNECT call
						 * `this.nodeDisconnected` 8. if PACKET_HEARTBEAT call
						 * `this.nodeHeartbeat` 8. else throw Invalid packet!
						 */
						
						Object data = Serializer.deserialize(message, format);
						String nameOfChannel = new String(channel, StandardCharsets.UTF_8);
						System.out.println(nameOfChannel + " -> " + data);

					} catch (Exception cause) {

						// TODO: invalid messaage format
					}
				}

				@Override
				public final void subscribed(byte[] channel, long count) {
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
	public void close() {
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
			logger.info("Redis channel \"" + channel + "\" subscribed.");
		}
	}

	// --- PUBLISH ---

	@Override
	public final void publish(String cmd, String nodeID, Object payload) {
		if (clientPub != null) {

			// TODO Serialize packet (move to superclass)
			byte[] channel = nameOfChannel(cmd, nodeID).getBytes(StandardCharsets.UTF_8);

			// Convert Object to JSON / MessagePack / etc
			Tree structure;
			byte[] outgoing;
			if (payload instanceof Tree) {
				structure = (Tree) payload;
			} else {
				structure = new Tree().setObject(payload);
			}
			outgoing = structure.toBinary("json", true);

			// Send in JSON / MessagePack / etc. format
			clientPub.publish(channel, outgoing);
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