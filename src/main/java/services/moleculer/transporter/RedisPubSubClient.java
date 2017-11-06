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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.connection.ConnectionActivatedEvent;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import com.lambdaworks.redis.resource.DefaultClientResources;

import io.netty.channel.nio.NioEventLoopGroup;
import rx.Observable;
import services.moleculer.Promise;
import services.moleculer.util.RedisUtilities;

public final class RedisPubSubClient {

	// --- VARIABLES ---

	private StatefulRedisPubSubConnection<byte[], byte[]> connection;
	private RedisPubSubAsyncCommands<byte[], byte[]> commands;

	private NioEventLoopGroup closeableGroup;

	// --- CONNECT TO REDIS ---

	public final Promise connect(String[] urls, String password, boolean useSSL, boolean startTLS,
			ExecutorService executor, Consumer<String> subscribeListener, BiConsumer<String, byte[]> messageListener) {
		Promise connected = new Promise();
		try {

			// Get or create NioEventLoopGroup
			NioEventLoopGroup redisGroup;
			if (executor instanceof NioEventLoopGroup) {
				redisGroup = (NioEventLoopGroup) executor;
			} else {
				redisGroup = new NioEventLoopGroup(1);
				closeableGroup = redisGroup;
			}

			// Create Redis connection
			final List<RedisURI> redisURIs = RedisUtilities.parseURLs(urls, password, useSSL, startTLS);
			DefaultClientResources clientResources = RedisUtilities.createClientResources(new EventBus() {

				@Override
				public final void publish(Event event) {

					// Connected to Redis server
					if (event instanceof ConnectionActivatedEvent) {
						connected.complete(event);
					}

				}

				@Override
				public final Observable<Event> get() {
					return null;
				}

			}, redisGroup);
			if (urls.length > 1) {

				// Clustered client
				connection = RedisClusterClient.create(clientResources, redisURIs).connectPubSub(new ByteArrayCodec());

			} else {

				// Single connection
				connection = RedisClient.create(clientResources, redisURIs.get(0)).connectPubSub(new ByteArrayCodec());

			}

			// Add listener
			connection.addListener(new RedisPubSubListener<byte[], byte[]>() {

				@Override
				public final void message(byte[] pattern, byte[] channel, byte[] message) {
					messageListener.accept(new String(channel, StandardCharsets.UTF_8), message);
				}

				@Override
				public final void message(byte[] channel, byte[] message) {
					messageListener.accept(new String(channel, StandardCharsets.UTF_8), message);
				}

				@Override
				public final void subscribed(byte[] channel, long count) {
					subscribeListener.accept(new String(channel, StandardCharsets.UTF_8));
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
			commands = connection.async();
		} catch (Exception cause) {
			connected.complete(cause);
		}
		return connected;
	}

	// --- SUBSCRIBE ---

	public final void subscribe(String channel) {
		commands.subscribe(channel.getBytes(StandardCharsets.UTF_8));
	}

	// --- PUBLISH ---

	public final void publish(String channel, byte[] message) {
		commands.publish(channel.getBytes(StandardCharsets.UTF_8), message);
	}

	// --- DISCONNECT ---

	public final void disconnect() {
		if (commands != null) {
			commands.close();
			commands = null;
		}
		if (connection != null) {
			connection.close();
			connection = null;
		}
		if (closeableGroup != null) {
			closeableGroup.shutdownGracefully();
		}
	}

}