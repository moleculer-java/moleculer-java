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
package services.moleculer.util.redis;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import com.lambdaworks.redis.resource.DefaultClientResources;
import com.lambdaworks.redis.resource.DefaultEventLoopGroupProvider;

import io.netty.util.concurrent.DefaultEventExecutor;
import services.moleculer.Promise;

public final class RedisPubSubClient {

	// --- VARIABLES ---

	private final String[] urls;
	private final String password;
	private final boolean useSSL;
	private final boolean startTLS;
	private final ExecutorService executor;
	private final EventBus eventBus;
	
	private final RedisPubSubListener<byte[], byte[]> listener;

	private DefaultClientResources clientResources;
	
	private StatefulRedisPubSubConnection<byte[], byte[]> connection;
	private RedisPubSubAsyncCommands<byte[], byte[]> commands;

	// --- CONSTRUCTOR ---

	public RedisPubSubClient(String[] urls, String password, boolean useSSL, boolean startTLS, ExecutorService executor,
			EventBus eventBus, RedisPubSubListener<byte[], byte[]> listener) {
		this.urls = urls;
		this.password = password;
		this.useSSL = useSSL;
		this.startTLS = startTLS;
		this.executor = executor;
		this.eventBus = eventBus;
		this.listener = listener;
	}

	// --- CONNECT ---

	public final Promise connect() {
		try {
			DefaultClientResources.Builder builder = DefaultClientResources.builder();
			builder.eventLoopGroupProvider(new DefaultEventLoopGroupProvider(1));
			builder.eventExecutorGroup(new DefaultEventExecutor(executor));
			builder.eventBus(eventBus);
			clientResources = builder.build();
			List<RedisURI> redisURIs = RedisUtilities.parseURLs(urls, password, useSSL, startTLS);
			if (urls.length > 1) {

				// Clustered client
				connection = RedisClusterClient.create(clientResources, redisURIs).connectPubSub(new ByteArrayCodec());

			} else {

				// Single connection
				connection = RedisClient.create(clientResources, redisURIs.get(0)).connectPubSub(new ByteArrayCodec());

			}

			// Add listener
			if (listener != null) {
				connection.addListener(listener);
			}
			commands = connection.async();
			return Promise.resolve();
		} catch (Throwable error) {
			return Promise.reject(error);
		}
	}

	// --- SUBSCRIBE ---

	public final Promise subscribe(String channel) {
		return new Promise(commands.subscribe(channel.getBytes(StandardCharsets.UTF_8)));
	}

	// --- PUBLISH ---

	public final void publish(String channel, byte[] message) {
		commands.publish(channel.getBytes(StandardCharsets.UTF_8), message);
	}

	// --- DISCONNECT ---

	public final Promise disconnect() {
		if (commands != null) {
			commands.close();
			commands = null;
		}
		if (connection != null) {
			connection.close();
			connection = null;
		}
		if (clientResources != null) {
			Promise promise = new Promise(clientResources.shutdown());
			return promise.then(ok -> {
				clientResources = null;				
			});
		}
		return Promise.resolve();
	}

}