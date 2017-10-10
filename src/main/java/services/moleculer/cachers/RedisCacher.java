package services.moleculer.cachers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.connection.ConnectionActivatedEvent;
import com.lambdaworks.redis.resource.DefaultClientResources;

import io.datatree.dom.TreeWriter;
import io.datatree.dom.TreeWriterRegistry;
import rx.Observable;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.services.Name;
import services.moleculer.utils.RedisUtilities;
import services.moleculer.utils.Serializer;

/**
 * Redis-based cache implementation. Supports SSL and password authentication.
 */
@Name("Redis Cacher")
public class RedisCacher extends Cacher {

	// --- PROPERTIES ---

	protected String[] urls = new String[] { "localhost" };
	protected String password;
	protected boolean useSSL;
	protected boolean startTLS;

	// --- REDIS CLIENTS ---

	protected RedisAsyncCommands<byte[], byte[]> client;
	protected RedisAdvancedClusterAsyncCommands<byte[], byte[]> clusteredClient;

	// --- CACHED JSON CONVERTER ---

	protected final TreeWriter writer = TreeWriterRegistry.getWriter(null);

	// --- COMMON EXECUTOR ---

	protected ExecutorService executorService;

	// --- CONSTUCTORS ---

	public RedisCacher() {
	}

	public RedisCacher(String... urls) {
		this(false, false, null, urls);
	}

	public RedisCacher(boolean useSSL, boolean startTLS, String password, String... urls) {
		this.useSSL = useSSL;
		this.startTLS = startTLS;
		this.password = password;
		this.urls = urls;
	}

	public RedisCacher(RedisAsyncCommands<byte[], byte[]> client, boolean useSSL, boolean startTLS, String password,
			String... urls) {
		this.client = client;
		this.useSSL = useSSL;
		this.startTLS = startTLS;
		this.password = password;
		this.urls = urls;
	}

	public RedisCacher(RedisAdvancedClusterAsyncCommands<byte[], byte[]> clusteredClient, boolean useSSL,
			boolean startTLS, String password, String... urls) {
		this.clusteredClient = clusteredClient;
		this.useSSL = useSSL;
		this.startTLS = startTLS;
		this.password = password;
		this.urls = urls;
	}

	// --- INIT CACHE INSTANCE ---

	/**
	 * Initializes cacher instance.
	 * 
	 * @param broker
	 */
	public final void init(ServiceBroker broker) throws Exception {

		// Init superclass
		super.init(broker);

		// Get the common executor
		executorService = broker.components().executorService();

		// Init Redis client
		if (client == null && clusteredClient == null) {

			// Create Redis connection
			List<RedisURI> redisURIs = RedisUtilities.parseURLs(urls, password, useSSL, startTLS);
			DefaultClientResources clientResources = RedisUtilities.createClientResources(new EventBus() {

				@Override
				public final void publish(Event event) {
					if (event instanceof ConnectionActivatedEvent) {
						ConnectionActivatedEvent e = (ConnectionActivatedEvent) event;
						logger.info("Redis cache connected successfully to " + e.remoteAddress() + '.');
					}
				}

				@Override
				public final Observable<Event> get() {
					return null;
				}

			});
			ByteArrayCodec codec = new ByteArrayCodec();
			if (urls.length > 1) {

				// Clustered client
				clusteredClient = RedisClusterClient.create(clientResources, redisURIs).connect(codec).async();

			} else {

				// Single connection
				client = RedisClient.create(clientResources, redisURIs.get(0)).connect(codec).async();

			}
		}
	}

	// --- CLOSE CACHE INSTANCE ---

	@Override
	public void close() {
		if (client != null) {
			client.close();
			client = null;
		}
		if (clusteredClient != null) {
			clusteredClient.close();
			clusteredClient = null;
		}
	}

	// --- CACHE METHODS ---

	@Override
	public Promise get(String key) {
		final Promise promise = new Promise();
		try {

			// Create cache key
			byte[] binaryKey = key.getBytes(StandardCharsets.UTF_8);

			// Get future
			final RedisFuture<byte[]> response;
			if (client != null) {
				response = client.get(binaryKey);
			} else if (clusteredClient != null) {
				response = clusteredClient.get(binaryKey);
			} else {
				return null;
			}

			// Async invocation
			response.whenComplete((bytes, error) -> {
				executorService.execute(() -> {
					Object value = null;
					try {
						if (error != null) {
							logger.warn("Unable to read data from Redis!", error);
						} else {
							if (bytes != null) {
								try {
									value = Serializer.deserialize(bytes, null);
								} catch (Throwable cause) {
									logger.warn("Unable to deserialize Redis data!", cause);
								}
							}
						}
					} finally {

						// Continue processing (without any error)
						promise.resolve(value);
					}
				});
			});
			return promise;
		} catch (Throwable cause) {
			logger.warn("Unable to communicate with Redis!", cause);

			// Continue processing (without any error)
			promise.resolve(null);
		}
		return null;
	}

	@Override
	public void set(String key, Object value) {
		byte[] binaryKey = key.getBytes(StandardCharsets.UTF_8);
		byte[] bytes = Serializer.serialize(value, null);

		// Send to Redis
		if (client != null) {
			if (value == null) {
				client.del(binaryKey);
			} else {
				client.set(binaryKey, bytes);
			}
			return;
		}
		if (clusteredClient != null) {
			if (value == null) {
				clusteredClient.del(binaryKey);
			} else {
				clusteredClient.set(binaryKey, bytes);
			}
		}
	}

	@Override
	public void del(String key) {
		byte[] binaryKey = key.getBytes(StandardCharsets.UTF_8);
		if (client != null) {
			client.del(binaryKey);
			return;
		}
		if (clusteredClient != null) {
			clusteredClient.del(binaryKey);
		}
	}

	@Override
	public void clean(String match) {
	}

}