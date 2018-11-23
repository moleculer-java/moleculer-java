/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
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

import io.datatree.Promise;

/**
 * Promise-based pub/sub Redis client.
 */
public final class RedisPubSubClient extends AbstractRedisClient {

	// --- VARIABLES ---

	private final RedisPubSubListener<byte[], byte[]> listener;
	private RedisPubSubAsyncCommands<byte[], byte[]> client;

	// --- CONSTRUCTOR ---

	public RedisPubSubClient(String[] urls, String password, boolean secure, ExecutorService executor,
			EventBus eventBus, RedisPubSubListener<byte[], byte[]> listener) {
		super(urls, password, secure, executor, eventBus);
		this.listener = listener;
	}

	// --- CONNECT ---

	public final void connect() {
		super.connect();
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
		client = connection.async();
	}

	// --- SUBSCRIBE ---

	public final Promise subscribe(String channel) {
		return new Promise(client.subscribe(channel.getBytes(StandardCharsets.UTF_8)));
	}

	// --- PUBLISH ---

	public final void publish(String channel, byte[] message) {
		if (client != null) {
			client.publish(channel.getBytes(StandardCharsets.UTF_8), message);
		}
	}

	// --- DISCONNECT ---

	@Override
	public final Promise disconnect() {
		if (client != null) {
			client.close();
			client = null;
		}
		return super.disconnect();
	}

}