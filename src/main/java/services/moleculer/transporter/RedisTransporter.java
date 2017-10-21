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
package services.moleculer.transporter;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

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
import io.netty.channel.nio.NioEventLoopGroup;
import rx.Observable;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;
import services.moleculer.util.RedisUtilities;
import services.moleculer.util.Serializer;

/**
 * 
 */
@Name("Redis Transporter")
public final class RedisTransporter extends Transporter {

	// --- PROPERTIES ---

	private String[] urls = new String[] { "localhost" };
	private String password;
	private boolean useSSL;
	private boolean startTLS;

	private StatefulRedisPubSubConnection<byte[], byte[]> clientSub;
	private RedisPubSubAsyncCommands<byte[], byte[]> clientPub;
	
	private NioEventLoopGroup closeableGroup;
	
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
			final List<RedisURI> redisURIs = RedisUtilities.parseURLs(urls, password, useSSL, startTLS);
			final RedisTransporter self = this;
			DefaultClientResources clientResources = RedisUtilities.createClientResources(new EventBus() {

				@Override
				public final void publish(Event event) {

					// Connected to Redis server
					if (event instanceof ConnectionActivatedEvent) {
						ConnectionActivatedEvent e = (ConnectionActivatedEvent) event;
						logger.info("Redis Transporter connected to " + e.remoteAddress() + ".");
						self.connected();
						return;
					}

					// Disconnected from Redis server
					if (event instanceof ConnectionDeactivatedEvent) {
						ConnectionDeactivatedEvent e = (ConnectionDeactivatedEvent) event;
						logger.info("Redis Transporter disconnected from " + e.remoteAddress() + ".");
						self.disconnected();
						return;
					}
				}

				@Override
				public final Observable<Event> get() {
					return null;
				}

			}, redisGroup);
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
		if (clientPub != null) {
			clientPub.close();
			clientPub = null;
		}
		if (clientSub != null) {
			clientSub.close();
			clientSub = null;
		}
		if (closeableGroup != null) {
			closeableGroup.shutdownGracefully();
		}
	}

	// --- SUBSCRIBE ---

	@Override
	public final void subscribe(String channel) {
		if (clientPub != null) {
			clientPub.subscribe(channel.getBytes(StandardCharsets.UTF_8));
		}
	}

	// --- PUBLISH ---

	@Override
	public final void publish(String channel, Tree message) {
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