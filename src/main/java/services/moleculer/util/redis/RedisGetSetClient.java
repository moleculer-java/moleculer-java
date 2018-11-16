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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

import com.lambdaworks.redis.KeyScanCursor;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.ScanArgs;
import com.lambdaworks.redis.ScanCursor;
import com.lambdaworks.redis.SetArgs;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.event.EventBus;

import io.datatree.Promise;
import services.moleculer.eventbus.Matcher;

/**
 * Promise-based get/set Redis client.
 */
public final class RedisGetSetClient extends AbstractRedisClient {

	// --- REDIS CLIENTS ---

	private RedisAsyncCommands<byte[], byte[]> client;
	private RedisAdvancedClusterAsyncCommands<byte[], byte[]> clusteredClient;

	// --- CONSTRUCTOR ---

	public RedisGetSetClient(String[] urls, String password, boolean secure, ExecutorService executor,
			EventBus eventBus) {
		super(urls, password, secure, executor, eventBus);
	}

	// --- CONNECT ---

	@Override
	public final void connect() {
		super.connect();
		List<RedisURI> redisURIs = parseURLs(urls, password, secure);
		ByteArrayCodec codec = new ByteArrayCodec();
		if (urls.length > 1) {

			// Clustered client
			clusteredClient = RedisClusterClient.create(resources, redisURIs).connect(codec).async();

		} else {

			// Single server connection
			client = RedisClient.create(resources, redisURIs.get(0)).connect(codec).async();
		}
	}

	// --- GET ---

	/**
	 * Gets a content by a key.
	 *
	 * @param key
	 *            cache key
	 * 
	 * @return Promise with cached value (or null, the returned Promise also can
	 *         be null)
	 */
	public final Promise get(String key) {
		byte[] binaryKey = key.getBytes(StandardCharsets.UTF_8);
		if (client != null) {
			return new Promise(client.get(binaryKey));
		}
		if (clusteredClient != null) {
			return new Promise(clusteredClient.get(binaryKey));
		}
		return Promise.resolve();
	}

	/**
	 * Sets a content by key.
	 *
	 * @param key
	 *            cache key
	 * @param value
	 *            new value
	 * @param args
	 *            Redis arguments (eg. TTL)
	 * 
	 * @return Promise with empty value
	 */
	public final Promise set(String key, byte[] value, SetArgs args) {
		byte[] binaryKey = key.getBytes(StandardCharsets.UTF_8);
		if (client != null) {
			if (args == null) {
				return new Promise(client.set(binaryKey, value));
			}
			return new Promise(client.set(binaryKey, value, args));
		}
		if (clusteredClient != null) {
			if (args == null) {
				return new Promise(clusteredClient.set(binaryKey, value));
			}
			return new Promise(clusteredClient.set(binaryKey, value, args));
		}
		return Promise.resolve();
	}

	/**
	 * Deletes a content with the specified key.
	 *
	 * @param key
	 *            cache key
	 * 
	 * @return Promise with empty value
	 */
	public final Promise del(String key) {
		byte[] binaryKey = key.getBytes(StandardCharsets.UTF_8);
		if (client != null) {
			return new Promise(client.del(binaryKey));
		}
		if (clusteredClient != null) {
			return new Promise(clusteredClient.del(binaryKey));
		}
		return Promise.resolve();
	}

	/**
	 * Deletes a group of items. Removes every key by a match string.
	 *
	 * @param match
	 *            regex
	 * 
	 * @return Promise with empty value
	 */
	public final Promise clean(String match) {
		ScanArgs args = new ScanArgs();
		args.limit(100);
		boolean singleStar = match.indexOf('*') > -1;
		boolean doubleStar = match.contains("**");
		if (doubleStar) {
			args.match(match.replace("**", "*"));
		} else if (singleStar) {
			if (match.length() > 1 && match.indexOf('.') == -1) {
				match += '*';
			}
			args.match(match);
		} else {
			args.match(match);
		}
		if (!singleStar || doubleStar) {
			match = null;
		}
		if (client != null) {
			return new Promise(clean(client.scan(args), args, match));
		}
		if (clusteredClient != null) {
			return new Promise(clean(clusteredClient.scan(args), args, match));
		}
		return Promise.resolve();
	}

	private final CompletionStage<Object> clean(RedisFuture<KeyScanCursor<byte[]>> future, ScanArgs args,
			String match) {
		return future.thenCompose(keyScanCursor -> {
			List<byte[]> keys = keyScanCursor.getKeys();
			if (keys == null || keys.isEmpty()) {
				return CompletableFuture.completedFuture(keyScanCursor);
			}
			if (match != null) {
				Iterator<byte[]> i = keys.iterator();
				while (i.hasNext()) {
					if (!Matcher.matches(new String(i.next(), StandardCharsets.UTF_8), match)) {
						i.remove();
					}
				}
			}
			if (keys.isEmpty()) {
				return CompletableFuture.completedFuture(keyScanCursor);
			}
			byte[][] array = new byte[keys.size()][];
			keys.toArray(array);
			return client.del(array).thenApply(nul -> keyScanCursor);
		}).thenApply(keyScanCursor -> {
			if (((KeyScanCursor<byte[]>) keyScanCursor).isFinished()) {
				return null;
			}
			return ((KeyScanCursor<byte[]>) keyScanCursor).getCursor();
		}).thenCompose(currentCursor -> {
			if (currentCursor == null) {
				return CompletableFuture.completedFuture(null);
			}
			return clean(new ScanCursor((String) currentCursor, false), args, match);
		});
	}

	private final CompletionStage<Object> clean(ScanCursor cursor, ScanArgs args, String match) {
		if (client != null) {
			return clean(client.scan(cursor, args), args, match);
		}
		return clean(clusteredClient.scan(cursor, args), args, match);
	}

	// --- DISCONNECT ---

	@Override
	public final Promise disconnect() {
		if (client != null) {
			client.close();
			client = null;
		} else if (clusteredClient != null) {
			clusteredClient.close();
			clusteredClient = null;
		}
		return super.disconnect();
	}

}