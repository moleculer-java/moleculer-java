package services.moleculer.util.redis;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.resource.DefaultClientResources;
import com.lambdaworks.redis.resource.DefaultEventLoopGroupProvider;

import io.netty.util.concurrent.DefaultEventExecutor;
import services.moleculer.Promise;

public class RedisGetSetClient {

	// --- VARIABLES ---

	private final String[] urls;
	private final String password;
	private final boolean useSSL;
	private final boolean startTLS;
	private final ExecutorService executor;
	private final EventBus eventBus;
	
	// --- REDIS CLIENT ---

	private DefaultClientResources clientResources;
	
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

	public final Promise connect() {
		try {
			DefaultClientResources.Builder builder = DefaultClientResources.builder();
			builder.eventLoopGroupProvider(new DefaultEventLoopGroupProvider(1));
			builder.eventExecutorGroup(new DefaultEventExecutor(executor));
			builder.eventBus(eventBus);
			clientResources = builder.build();
			List<RedisURI> redisURIs = RedisUtilities.parseURLs(urls, password, useSSL, startTLS);
			
			
			
			return Promise.resolve();
		} catch (Throwable error) {
			return Promise.reject(error);
		}
	}
	
}
