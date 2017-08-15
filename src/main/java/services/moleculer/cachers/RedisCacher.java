package services.moleculer.cachers;

import java.util.HashMap;
import java.util.List;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.resource.DefaultClientResources;

import io.datatree.Tree;
import io.datatree.dom.TreeWriter;
import io.datatree.dom.TreeWriterRegistry;
import services.moleculer.ServiceBroker;
import services.moleculer.utils.RedisUtilities;

public class RedisCacher extends Cacher {

	// --- CONSTANTS ---

	public static final String RAW_VALUE = "_raw";

	// --- VARIABLES ---

	protected String[] urls = new String[] { "localhost" };
	protected String password;
	protected boolean useSSL;
	protected boolean startTLS;

	protected RedisAsyncCommands<String, String> client;
	protected RedisAdvancedClusterAsyncCommands<String, String> clusteredClient;

	// --- CACHED JSON CONVERTER ---

	protected final TreeWriter writer = TreeWriterRegistry.getWriter(null);

	// --- CONSTUCTORS ---

	public RedisCacher() {
		super();
	}

	public RedisCacher(String prefix) {
		super(prefix);
	}

	public RedisCacher(String prefix, long ttl) {
		super(prefix, ttl);
	}

	public RedisCacher(String prefix, String... urls) {
		super(prefix);
		this.urls = urls;
	}

	public RedisCacher(String prefix, boolean useSSL, boolean startTLS, String... urls) {
		super(prefix);
		this.useSSL = useSSL;
		this.startTLS = startTLS;
		this.urls = urls;
	}

	public RedisCacher(String prefix, RedisAsyncCommands<String, String> client) {
		super(prefix);
		this.client = client;
	}

	public RedisCacher(String prefix, RedisAdvancedClusterAsyncCommands<String, String> clusteredClient) {
		super(prefix);
		this.clusteredClient = clusteredClient;
	}

	// --- INIT CACHE INSTANCE ---

	/**
	 * Initializes cacher instance.
	 * 
	 * @param broker
	 */
	public void init(ServiceBroker broker) {
		super.init(broker);
		if (client == null && clusteredClient == null) {

			// Create Redis connection
			List<RedisURI> redisURIs = RedisUtilities.parseURLs(urls, password, useSSL, startTLS);
			DefaultClientResources clientResources = RedisUtilities.getDefaultClientResources();
			if (urls.length > 1) {

				// Clustered client
				clusteredClient = RedisClusterClient.create(clientResources, redisURIs).connect().async();

			} else {

				// Single connection
				client = RedisClient.create(clientResources, redisURIs.get(0)).connect().async();

			}
		}
	}

	// --- CLOSE CACHE INSTANCE ---

	@Override
	public void close() {
		if (client != null) {
			client.close();
			client = null;
		}
		if (clusteredClient != null) {
			clusteredClient.close();
			clusteredClient = null;
		}
	}

	// --- CACHE METHODS ---

	@Override
	public Object get(String key) {
		try {
			RedisFuture<String> future;
			if (client != null) {
				future = client.get(key);
			} else if (clusteredClient != null) {
				future = clusteredClient.get(key);
			} else {
				return null;
			}

			// TODO
			return future.get();

		} catch (Exception cause) {
			cause.printStackTrace();
		}
		return null;
	}

	@Override
	public void set(String key, Object value) {

		// Delete null value
		if (value == null) {
			del(key);
			return;
		}

		// Convert Object to JSON
		String json;
		if (value instanceof Tree) {
			json = ((Tree) value).toString(null, false, true);
		} else {
			HashMap<String, Object> map = new HashMap<>();
			map.put(RAW_VALUE, value);
			json = writer.toString(map, null, false, true);
		}

		// TODO wait for finished state?
		if (client != null) {
			client.set(key, json);
			return;
		}
		if (clusteredClient != null) {
			clusteredClient.set(key, json);
		}
	}

	@Override
	public void del(String key) {

		// TODO wait for finished state?
		if (client != null) {
			client.del(key);
			return;
		}
		if (clusteredClient != null) {
			clusteredClient.del(key);
		}
	}

	@Override
	public void clean(String match) {
	}

	// --- GETTERS / SETTERS ---

	public String[] getUrls() {
		return urls;
	}

	public void setUrls(String[] urls) {
		this.urls = urls;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isUseSSL() {
		return useSSL;
	}

	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

	public boolean isStartTLS() {
		return startTLS;
	}

	public void setStartTLS(boolean startTLS) {
		this.startTLS = startTLS;
	}

	public RedisAsyncCommands<String, String> getClient() {
		return client;
	}

	public void setClient(RedisAsyncCommands<String, String> client) {
		this.client = client;
	}

	public RedisAdvancedClusterAsyncCommands<String, String> getClusteredClient() {
		return clusteredClient;
	}

	public void setClusteredClient(RedisAdvancedClusterAsyncCommands<String, String> clusteredClient) {
		this.clusteredClient = clusteredClient;
	}

}