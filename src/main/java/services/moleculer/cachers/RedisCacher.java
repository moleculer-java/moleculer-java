package services.moleculer.cachers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.connection.ConnectionActivatedEvent;
import com.lambdaworks.redis.resource.DefaultClientResources;

import io.datatree.dom.TreeWriter;
import io.datatree.dom.TreeWriterRegistry;
import rx.Observable;
import services.moleculer.ServiceBroker;
import services.moleculer.fibers.FiberEngine;
import services.moleculer.utils.RedisUtilities;
import services.moleculer.utils.Serializer;

/**
 * Redis-based cache implementation. Supports SSL and password authentication.
 */
public class RedisCacher extends Cacher {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	public String name() {
		return "Redis Cacher";
	}
	
	// --- PROPERTIES ---

	protected String[] urls = new String[] { "localhost" };
	protected String password;
	protected boolean useSSL;
	protected boolean startTLS;

	// --- REDIS CLIENTS ---

	protected RedisAsyncCommands<String, String> client;
	protected RedisAdvancedClusterAsyncCommands<String, String> clusteredClient;

	// --- CACHED JSON CONVERTER ---

	protected final TreeWriter writer = TreeWriterRegistry.getWriter(null);

	// --- CONSTUCTORS ---

	public RedisCacher() {
	}

	public RedisCacher(String... urls) {
		this(false, false, urls);
	}

	public RedisCacher(boolean useSSL, boolean startTLS, String... urls) {
		this.urls = urls;
		this.useSSL = useSSL;
		this.startTLS = startTLS;
	}

	public RedisCacher(RedisAsyncCommands<String, String> client) {
		this.client = client;
	}

	public RedisCacher(RedisAdvancedClusterAsyncCommands<String, String> clusteredClient) {
		this.clusteredClient = clusteredClient;
	}

	// --- INIT CACHE INSTANCE ---

	/**
	 * Initializes cacher instance.
	 * 
	 * @param broker
	 */
	public final void init(ServiceBroker broker) throws Exception {
		super.init(broker);
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
			if (urls.length > 1) {

				// Clustered client
				clusteredClient = RedisClusterClient.create(clientResources, redisURIs).connect().async();

			} else {

				// Single connection
				client = RedisClient.create(clientResources, redisURIs.get(0)).connect().async();

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
	public Object get(String key) {
		try {
			
			// Get future
			final RedisFuture<String> response;
			if (client != null) {
				response = client.get(key);
			} else if (clusteredClient != null) {
				response = clusteredClient.get(key);
			} else {
				return null;
			}

			// Read non-blocking mode
			return FiberEngine.invokeCallback((callback) -> {
				response.thenAccept((packet) -> {
					callback.invoke(packet, null);
				});				
			});
			
		} catch (Throwable cause) {
			logger.warn("Unable to read data from Redis!", cause);
		}
		return null;
	}

	@Override
	public void set(String key, Object value) {

		// Convert value to JSON
		String json = new String(Serializer.serialize(value, null), StandardCharsets.UTF_8);
		
		// Send to Redis
		if (client != null) {
			if (value == null) {
				client.del(key);
			} else {
				client.set(key, json);
			}
			return;
		}
		if (clusteredClient != null) {
			if (value == null) {
				clusteredClient.del(key);
			} else {
				clusteredClient.set(key, json);
			}
		}
	}

	@Override
	public void del(String key) {
		if (client != null) {
			client.del(key);
			return;
		}
		if (clusteredClient != null) {
			clusteredClient.del(key);
		}
	}

	@Override
	public void clean(String match) {
	}

	// --- GETTERS / SETTERS FOR SPRING FRAMEWORK ---

	public String[] getUrls() {
		return urls;
	}

	public void setUrls(String[] urls) {
		this.urls = urls;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isUseSSL() {
		return useSSL;
	}

	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

	public boolean isStartTLS() {
		return startTLS;
	}

	public void setStartTLS(boolean startTLS) {
		this.startTLS = startTLS;
	}

	public RedisAsyncCommands<String, String> getClient() {
		return client;
	}

	public void setClient(RedisAsyncCommands<String, String> client) {
		this.client = client;
	}

	public RedisAdvancedClusterAsyncCommands<String, String> getClusteredClient() {
		return clusteredClient;
	}

	public void setClusteredClient(RedisAdvancedClusterAsyncCommands<String, String> clusteredClient) {
		this.clusteredClient = clusteredClient;
	}

}