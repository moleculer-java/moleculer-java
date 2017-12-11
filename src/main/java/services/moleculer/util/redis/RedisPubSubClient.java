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

import static services.moleculer.util.redis.RedisGetSetClient.parseURLs;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import com.lambdaworks.redis.resource.DefaultClientResources;
import com.lambdaworks.redis.resource.EventLoopGroupProvider;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import rx.Observable;
import services.moleculer.Promise;

/**
 * Promise-based pub/sub Redis client.
 */
public final class RedisPubSubClient {

	// --- VARIABLES ---

	private final String[] urls;
	private final String password;
	private final boolean secure;
	private final ExecutorService executor;
	private final EventBus eventBus;
	private final RedisPubSubListener<byte[], byte[]> listener;

	private ExecutorService acceptor;
	private NioEventLoopGroup group;
	private DefaultClientResources resources;
	private RedisPubSubAsyncCommands<byte[], byte[]> commands;

	// --- CONSTRUCTOR ---

	public RedisPubSubClient(String[] urls, String password, boolean secure, ExecutorService executor,
			EventBus eventBus, RedisPubSubListener<byte[], byte[]> listener) {
		this.urls = urls;
		this.password = password;
		this.secure = secure;
		this.executor = executor;
		this.eventBus = eventBus;
		this.listener = listener;
	}

	// --- CONNECT ---

	public final void connect() {
		DefaultClientResources.Builder builder = DefaultClientResources.builder();
		acceptor = Executors.newSingleThreadExecutor();
		group = new NioEventLoopGroup(1, acceptor);
		builder.eventLoopGroupProvider(new EventLoopGroupProvider() {

			@Override
			public final int threadPoolSize() {
				return 1;
			}

			@Override
			public final Future<Boolean> shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {
				return null;
			}

			@Override
			public final Future<Boolean> release(EventExecutorGroup eventLoopGroup, long quietPeriod, long timeout,
					TimeUnit unit) {
				return null;
			}

			@SuppressWarnings("unchecked")
			@Override
			public final <T extends EventLoopGroup> T allocate(Class<T> type) {
				return (T) group;
			}

		});
		builder.eventExecutorGroup(new DefaultEventExecutor(executor));
		if (eventBus == null) {
			builder.eventBus(new EventBus() {

				@Override
				public final void publish(Event event) {
				}

				@Override
				public final Observable<Event> get() {
					return null;
				}

			});
		} else {
			builder.eventBus(eventBus);
		}
		resources = builder.build();
		List<RedisURI> redisURIs = parseURLs(urls, password, secure);
		StatefulRedisPubSubConnection<byte[], byte[]> connection;
		ByteArrayCodec codec = new ByteArrayCodec();
		if (urls.length > 1) {

			// Clustered client
			connection = RedisClusterClient.create(resources, redisURIs).connectPubSub(codec);

		} else {

			// Single connection
			connection = RedisClient.create(resources, redisURIs.get(0)).connectPubSub(codec);
		}

		// Add listener
		if (listener != null) {
			connection.addListener(listener);
		}
		commands = connection.async();
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
		LinkedList<Promise> threads = new LinkedList<>();
		if (group != null) {
			threads.add(new Promise(group.shutdownGracefully(1, 1, TimeUnit.SECONDS)));
		}
		if (resources != null) {
			threads.add(new Promise(resources.shutdown()));
		}
		return Promise.all(threads).then(ok -> {
			if (acceptor != null) {
				acceptor.shutdownNow();
				acceptor = null;
			}
			resources = null;
		});
	}

}