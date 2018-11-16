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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.resource.DefaultClientResources;
import com.lambdaworks.redis.resource.EventLoopGroupProvider;

import io.datatree.Promise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import rx.Observable;

/**
 * Promise-based abstract Redis client.
 */
public abstract class AbstractRedisClient {

	// --- PROPERTIES ---

	protected final String[] urls;
	protected final String password;
	protected final boolean secure;

	// --- COMPONENTS ---

	private final ExecutorService executor;
	private final EventBus eventBus;

	// --- INTERNAL VARIABLES ---

	private ExecutorService acceptor;
	private NioEventLoopGroup group;

	protected DefaultClientResources resources;

	// --- CONSTRUCTOR ---

	protected AbstractRedisClient(String[] urls, String password, boolean secure, ExecutorService executor,
			EventBus eventBus) {
		this.urls = urls;
		this.password = password;
		this.secure = secure;
		this.executor = executor;
		this.eventBus = eventBus;
	}

	// --- CONNECT ---

	protected void connect() {
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

					// Do nothing
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
	}

	// --- DISCONNECT ---

	protected Promise disconnect() {
		if (group != null) {
			try {
				group.shutdownGracefully(1, 1, TimeUnit.SECONDS).await(1, TimeUnit.SECONDS);
			} catch (InterruptedException ignored) {
			} finally {
				group = null;
			}
		}
		if (resources != null) {
			try {
				resources.shutdown(1, 1, TimeUnit.SECONDS).await(1, TimeUnit.SECONDS);
			} catch (InterruptedException ignored) {
			} finally {
				resources = null;
			}
		}
		if (acceptor != null) {
			try {
				acceptor.shutdownNow();
			} catch (Exception ignored) {
			} finally {
				acceptor = null;
			}
		}
		return Promise.resolve();
	}

	// --- CONFIG PARSER ---

	protected final List<RedisURI> parseURLs(String[] urls, String password, boolean secure) {
		ArrayList<RedisURI> list = new ArrayList<>(urls.length);
		for (String url : urls) {
			url = url.trim();
			if ((password == null || password.isEmpty()) && !secure) {

				// URL contains all of the connection parameters
				try {
					list.add(RedisURI.create(url));
					continue;
				} catch (Exception ignored) {

					// Ignore, use old parsing below.
				}
			}

			// Simple "host" or "host:port" or "redis://host:port" syntax
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
			RedisURI.Builder builder = RedisURI.builder().withHost(host).withPort(port).withSsl(secure);
			if (password != null && !password.isEmpty()) {
				builder.withPassword(password);
			}
			list.add(builder.build());
		}
		return list;
	}

}