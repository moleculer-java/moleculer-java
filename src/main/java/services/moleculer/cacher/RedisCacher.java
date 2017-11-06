/**
 * This software is licensed under MIT license.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.cacher;

import static services.moleculer.util.CommonUtils.nameOf;
import static services.moleculer.util.CommonUtils.serializerTypeToClass;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.SetArgs;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.connection.ConnectionActivatedEvent;
import com.lambdaworks.redis.event.connection.ConnectionDeactivatedEvent;
import com.lambdaworks.redis.resource.DefaultClientResources;

import io.datatree.Tree;
import io.datatree.dom.TreeWriter;
import io.datatree.dom.TreeWriterRegistry;
import io.netty.channel.nio.NioEventLoopGroup;
import rx.Observable;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.serializer.JsonSerializer;
import services.moleculer.serializer.Serializer;
import services.moleculer.service.Name;
import services.moleculer.util.RedisUtilities;

/**
 * Redis-based cache implementation. Supports SSL, clustering and password
 * authentication.
 */
@Name("Redis Cacher")
public final class RedisCacher extends Cacher {

	// --- PROPERTIES ---

	private String[] urls = new String[] { "localhost" };
	private String password;
	private boolean useSSL;
	private boolean startTLS;

	/**
	 * Expire time, in seconds (0 = never expires)
	 */
	private int ttl;

	/**
	 * TCP acceptor group of the Redis client.
	 */
	private NioEventLoopGroup closeableGroup;

	// --- REDIS CLIENTS ---

	protected RedisAsyncCommands<byte[], byte[]> client;
	protected RedisAdvancedClusterAsyncCommands<byte[], byte[]> clusteredClient;

	// --- CACHED JSON CONVERTER ---

	protected final TreeWriter writer = TreeWriterRegistry.getWriter(null);

	// --- COMMON EXECUTOR ---

	protected Executor executor;

	// --- SERIALIZER / DESERIALIZER ---

	protected Serializer serializer;
	
	// --- CONSTUCTORS ---

	public RedisCacher() {
	}

	public RedisCacher(String... urls) {
		this(false, false, null, 0, urls);
	}

	public RedisCacher(boolean useSSL, boolean startTLS, String password, int ttl, String... urls) {
		this.useSSL = useSSL;
		this.startTLS = startTLS;
		this.password = password;
		this.ttl = ttl;
		this.urls = urls;
	}

	public RedisCacher(RedisAsyncCommands<byte[], byte[]> client, boolean useSSL, boolean startTLS, String password,
			int ttl, String... urls) {
		this.client = client;
		this.useSSL = useSSL;
		this.startTLS = startTLS;
		this.password = password;
		this.ttl = ttl;
		this.urls = urls;
	}

	public RedisCacher(RedisAdvancedClusterAsyncCommands<byte[], byte[]> clusteredClient, boolean useSSL,
			boolean startTLS, String password, int ttl, String... urls) {
		this.clusteredClient = clusteredClient;
		this.useSSL = useSSL;
		this.startTLS = startTLS;
		this.password = password;
		this.ttl = ttl;
		this.urls = urls;
	}

	// --- INIT CACHE INSTANCE ---

	private SetArgs expiration;

	/**
	 * Initializes cacher instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public final void start(ServiceBroker broker, Tree config) throws Exception {

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
		ttl = config.get("ttl", ttl);
		if (ttl > 0) {

			// Set the default expire time, in seconds.
			expiration = SetArgs.Builder.ex(ttl);
		} else {
			expiration = null;
		}

		// Create serializer
		Tree serializerNode = config.get("serializer");
		if (serializerNode != null) {
			String type;
			if (serializerNode.isPrimitive()) {
				type = serializerNode.asString();
			} else {
				type = serializerNode.get("type", "json");
			}

			@SuppressWarnings("unchecked")
			Class<? extends Serializer> c = (Class<? extends Serializer>) Class.forName(serializerTypeToClass(type));
			serializer = c.newInstance();
		} else {
			serializerNode = config.putMap("serializer");
		}
		if (serializer == null) {
			serializer = new JsonSerializer();
		}
		
		// Start serializer
		logger.info(nameOf(this, true) + " is using " + nameOf(serializer, true) + '.');
		serializer.start(broker, serializerNode);
		
		// Get the common executor
		executor = broker.components().executor();

		// Init Redis client
		if (client == null && clusteredClient == null) {

			// Get or create NioEventLoopGroup
			NioEventLoopGroup redisGroup;
			ExecutorService executor = broker.components().executor();
			if (executor instanceof NioEventLoopGroup) {
				redisGroup = (NioEventLoopGroup) executor;
			} else {
				redisGroup = new NioEventLoopGroup(1);
				closeableGroup = redisGroup;
			}

			// Create Redis connection
			List<RedisURI> redisURIs = RedisUtilities.parseURLs(urls, password, useSSL, startTLS);
			DefaultClientResources clientResources = RedisUtilities.createClientResources(new EventBus() {

				@Override
				public final void publish(Event event) {

					// Connected to Redis server
					if (event instanceof ConnectionActivatedEvent) {
						ConnectionActivatedEvent e = (ConnectionActivatedEvent) event;
						logger.info("Redis cacher connected to " + e.remoteAddress() + ".");
						return;
					}

					// Disconnected from Redis server
					if (event instanceof ConnectionDeactivatedEvent) {
						ConnectionDeactivatedEvent e = (ConnectionDeactivatedEvent) event;
						logger.info("Redis cacher disconnected from " + e.remoteAddress() + ".");
						return;
					}
				}

				@Override
				public final Observable<Event> get() {
					return null;
				}

			}, redisGroup);
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
	public final void stop() {
		if (client != null) {
			client.close();
			client = null;
		}
		if (clusteredClient != null) {
			clusteredClient.close();
			clusteredClient = null;
		}
		if (closeableGroup != null) {
			closeableGroup.shutdownGracefully(1, 3, TimeUnit.SECONDS);
		}
	}

	// --- CACHE METHODS ---

	@Override
	public final Promise get(String key) {
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
			return new Promise(response.thenApplyAsync((bytes) -> {
				try {
					return serializer.read(bytes);
				} catch (Throwable cause) {
					logger.warn("Unable to deserialize data!", cause);
				}
				return null;
			}, executor));

		} catch (Throwable cause) {
			logger.warn("Unable to get data from from Redis!", cause);
		}
		return null;
	}

	@Override
	public final void set(String key, Tree value) {
		byte[] binaryKey = key.getBytes(StandardCharsets.UTF_8);
		byte[] bytes;
		try {
			bytes = serializer.write(value);			
		} catch (Exception cause) {
			logger.warn("Unable to serialize data!", cause);
			return;
		}

		// Send to Redis
		if (client != null) {
			if (value == null) {
				client.del(binaryKey);
			} else {
				if (expiration == null) {
					client.set(binaryKey, bytes);
				} else {
					client.set(binaryKey, bytes, expiration);
				}
			}
			return;
		}
		if (clusteredClient != null) {
			if (value == null) {
				clusteredClient.del(binaryKey);
			} else {
				if (expiration == null) {
					clusteredClient.set(binaryKey, bytes);
				} else {
					clusteredClient.set(binaryKey, bytes, expiration);
				}
			}
		}
	}

	@Override
	public final void del(String key) {
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
	public final void clean(String match) {
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

	public final int getTtl() {
		return ttl;
	}

	public final void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public final RedisAsyncCommands<byte[], byte[]> getClient() {
		return client;
	}

	public final void setClient(RedisAsyncCommands<byte[], byte[]> client) {
		this.client = client;
	}

	public final RedisAdvancedClusterAsyncCommands<byte[], byte[]> getClusteredClient() {
		return clusteredClient;
	}

	public final void setClusteredClient(RedisAdvancedClusterAsyncCommands<byte[], byte[]> clusteredClient) {
		this.clusteredClient = clusteredClient;
	}

	public final Serializer getSerializer() {
		return serializer;
	}

	public final void setSerializer(Serializer serializer) {
		this.serializer = Objects.requireNonNull(serializer);
	}
	
}