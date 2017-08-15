package services.moleculer.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.async.RedisStringAsyncCommands;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.resource.DefaultClientResources;
import com.lambdaworks.redis.resource.EventLoopGroupProvider;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import rx.Observable;

public final class RedisUtilities {

	// --- GET/SET CONNECTIONS ---

	public static final RedisStringAsyncCommands<String, String> getAsyncCommands(String[] urls, String password,
			boolean useSSL, boolean startTLS) {

		// Open new connection
		List<RedisURI> redisURIs = parseURLs(urls, password, useSSL, startTLS);
		DefaultClientResources clientResources = getDefaultClientResources();
		RedisStringAsyncCommands<String, String> commands;
		if (urls.length > 1) {

			// Clustered client
			RedisClusterClient client = RedisClusterClient.create(clientResources, redisURIs);
			commands = client.connect().async();

		} else {

			// Single connection
			RedisClient client = RedisClient.create(clientResources, redisURIs.get(0));
			commands = client.connect().async();

		}
		return commands;
	}

	// --- PRIVATE UTILITIES ---

	public static final List<RedisURI> parseURLs(String[] urls, String password, boolean useSSL, boolean startTLS) {
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

	public static final DefaultClientResources getDefaultClientResources() {
		DefaultClientResources.Builder builder = DefaultClientResources.builder();
		EventLoopGroup group = new NioEventLoopGroup(1, Executors.newSingleThreadExecutor());
		builder.eventExecutorGroup(group);
		builder.eventLoopGroupProvider(new EventLoopGroupProvider() {

			@Override
			public int threadPoolSize() {
				return 1;
			}

			@Override
			public Future<Boolean> shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {
				return null;
			}

			@Override
			public Future<Boolean> release(EventExecutorGroup eventLoopGroup, long quietPeriod, long timeout,
					TimeUnit unit) {
				return null;
			}

			@SuppressWarnings("unchecked")
			@Override
			public <T extends EventLoopGroup> T allocate(Class<T> type) {
				return (T) group;
			}

		});
		builder.eventBus(new EventBus() {

			@Override
			public void publish(Event event) {
			}

			@Override
			public Observable<Event> get() {
				return null;
			}

		});
		return builder.build();
	}

}