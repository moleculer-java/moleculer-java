package services.moleculer.transporters;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
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
						self.connected();
						return;
					}

					// Disconnected from Redis server
					if (event instanceof ConnectionDeactivatedEvent) {
						ConnectionDeactivatedEvent e = (ConnectionDeactivatedEvent) event;
						logger.info("Redis transporter disconnected from " + e.remoteAddress() + ".");
						self.disconnected();
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
				clientSub = RedisClusterClient.create(clientResources, redisURIs).connectPubSub(new ByteArrayCodec());

			} else {

				// Single connection
				clientSub = RedisClient.create(clientResources, redisURIs.get(0)).connectPubSub(new ByteArrayCodec());

			}

			// Add listener
			clientSub.addListener(new RedisPubSubListener<byte[], byte[]>() {

				@Override
				public final void message(byte[] pattern, byte[] channel, byte[] message) {
					self.received(new String(channel, StandardCharsets.UTF_8), message, null);
				}

				@Override
				public final void message(byte[] channel, byte[] message) {
					self.received(new String(channel, StandardCharsets.UTF_8), message, null);
				}

				@Override
				public final void subscribed(byte[] channel, long count) {
					self.subscribed(new String(channel, StandardCharsets.UTF_8));
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
	public final void subscribe(String channel) {
		System.out.println("subscribe: " + channel);
		if (clientPub != null) {
			clientPub.subscribe(channel.getBytes(StandardCharsets.UTF_8));
		}
	}

	// --- PUBLISH ---

	@Override
	public final void publish(String channel, Tree message) {
		System.out.println("publish: " + channel + "\r\n" + message);
		if (clientPub != null) {
			try {
				clientPub.publish(channel.getBytes(StandardCharsets.UTF_8),
						Serializer.serialize(message, format));
			} catch (Exception cause) {
				logger.warn("Unable to send message to Redis!", cause);
			}
		}
	}

	// --- GETTERS / SETTERS ---
	
	public final String[] getUrls() {
		return urls;
	}

	public final void setUrls(String[] urls) {
		this.urls = urls;
	}

	public final String getPassword() {
		return password;
	}

	public final void setPassword(String password) {
		this.password = password;
	}

	public final boolean isUseSSL() {
		return useSSL;
	}

	public final void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

	public final boolean isStartTLS() {
		return startTLS;
	}

	public final void setStartTLS(boolean startTLS) {
		this.startTLS = startTLS;
	}

	public final StatefulRedisPubSubConnection<byte[], byte[]> getClientSub() {
		return clientSub;
	}

	public final void setClientSub(StatefulRedisPubSubConnection<byte[], byte[]> clientSub) {
		this.clientSub = clientSub;
	}

	public final RedisPubSubAsyncCommands<byte[], byte[]> getClientPub() {
		return clientPub;
	}

	public final void setClientPub(RedisPubSubAsyncCommands<byte[], byte[]> clientPub) {
		this.clientPub = clientPub;
	}
	
}