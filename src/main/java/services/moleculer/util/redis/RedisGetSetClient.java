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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.SetArgs;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
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
 * Promise-based get/set Redis client. 
 */
public final class RedisGetSetClient {

	// --- VARIABLES ---

	private final String[] urls;
	private final String password;
	private final boolean useSSL;
	private final boolean startTLS;
	private final ExecutorService executor;
	private final EventBus eventBus;

	// --- REDIS CLIENT ---

	private ExecutorService acceptor;
	private NioEventLoopGroup group;
	private DefaultClientResources resources;

	private RedisAsyncCommands<byte[], byte[]> client;
	private RedisAdvancedClusterAsyncCommands<byte[], byte[]> clusteredClient;

	// --- CONSTRUCTOR ---

	public RedisGetSetClient(String[] urls, String password, boolean useSSL, boolean startTLS, ExecutorService executor,
			EventBus eventBus) {
		this.urls = urls;
		this.password = password;
		this.useSSL = useSSL;
		this.startTLS = startTLS;
		this.executor = executor;
		this.eventBus = eventBus;
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
		List<RedisURI> redisURIs = parseURLs(urls, password, useSSL, startTLS);
		ByteArrayCodec codec = new ByteArrayCodec();
		if (urls.length > 1) {

			// Clustered client
			clusteredClient = RedisClusterClient.create(resources, redisURIs).connect(codec).async();

		} else {

			// Single connection
			client = RedisClient.create(resources, redisURIs.get(0)).connect(codec).async();
		}
	}

	// --- GET ---

	/**
	 * Gets a content by a key.
	 * 
	 * @param key
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
	 * @param value
	 * @param args
	 */
	public final void set(String key, byte[] value, SetArgs args) {
		byte[] binaryKey = key.getBytes(StandardCharsets.UTF_8);
		if (client != null) {
			client.set(binaryKey, value, args);
		}
		if (clusteredClient != null) {
			clusteredClient.set(binaryKey, value, args);
		}
	}

	/**
	 * Deletes a content with the specified key.
	 * 
	 * @param key
	 */
	public final void del(String key) {
		byte[] binaryKey = key.getBytes(StandardCharsets.UTF_8);
		if (client != null) {
			client.del(binaryKey);
		}
		if (clusteredClient != null) {
			clusteredClient.del(binaryKey);
		}
	}

	/**
	 * Deletes a group of items. Removes every key by a match string. The
	 * default match string is "**".
	 * 
	 * @param match
	 */
	public final void clean(String match) {

		// TODO not imlemented!

	}

	// --- DISCONNECT ---

	public final Promise disconnect() {
		if (client != null) {
			client.close();
			client = null;
		} else if (clusteredClient != null) {
			clusteredClient.close();
			clusteredClient = null;
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

	// --- CONFIG PARSER ---

	static final List<RedisURI> parseURLs(String[] urls, String password, boolean useSSL, boolean startTLS) {
		ArrayList<RedisURI> list = new ArrayList<>(urls.length);
		for (String url : urls) {
			url = url.trim();
			if (url.startsWith("redis://")) {
				url = url.substring(8);
			}
			if (url.endsWith("/")) {
				url = url.substring(0, url.length() - 1);
			}
			int i = url.indexOf(':');
			String host = "localhost";
			int port = 6379;
			if (i > -1) {
				host = url.substring(0, i);
				port = Integer.parseInt(url.substring(i + 1));
			} else {
				host = url;
			}
			RedisURI.Builder builder = RedisURI.builder().withHost(host).withPort(port).withSsl(useSSL)
					.withStartTls(startTLS);
			if (password != null && !password.isEmpty()) {
				builder.withPassword(password);
			}
			list.add(builder.build());
		}
		return list;
	}

}