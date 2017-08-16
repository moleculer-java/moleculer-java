package services.moleculer.transporters;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import com.lambdaworks.redis.resource.DefaultClientResources;

import io.datatree.Tree;
import io.datatree.dom.TreeWriterRegistry;
import services.moleculer.Packet;
import services.moleculer.utils.RedisUtilities;

public class RedisTransporter extends Transporter {

	// --- CONSTANTS ---

	public static final String RAW_VALUE = "_raw";
	
	// --- VARIABLES ---

	protected String[] urls = new String[] { "localhost" };
	protected String password;
	protected boolean useSSL;
	protected boolean startTLS;

	protected StatefulRedisPubSubConnection<byte[], byte[]> clientSub;
	protected RedisPubSubAsyncCommands<byte[], byte[]> clientPub;
	
	// --- CONSTUCTORS ---

	public RedisTransporter() {
		super();
	}

	public RedisTransporter(String prefix) {
		super(prefix);
	}

	public RedisTransporter(String prefix, String... urls) {
		super(prefix);
		this.urls = urls;
	}

	public RedisTransporter(String prefix, boolean useSSL, boolean startTLS, String... urls) {
		super(prefix);
		this.useSSL = useSSL;
		this.startTLS = startTLS;
		this.urls = urls;
	}

	public RedisTransporter(String prefix, StatefulRedisPubSubConnection<byte[], byte[]> clientSub) {
		super(prefix);
		this.clientSub = clientSub;
	}

	// --- CONNECT ---

	@Override
	public void connect() {
		if (clientSub == null) {

			// Create Redis connection
			List<RedisURI> redisURIs = RedisUtilities.parseURLs(urls, password, useSSL, startTLS);
			DefaultClientResources clientResources = RedisUtilities.getDefaultClientResources();
			if (urls.length > 1) {

				// Clustered client
				RedisClusterClient client = RedisClusterClient.create(clientResources, redisURIs);
				clientSub = client.connectPubSub(new ByteArrayCodec());

			} else {

				// Single connection
				RedisClient client = RedisClient.create(clientResources, redisURIs.get(0));
				clientSub = client.connectPubSub(new ByteArrayCodec());

			}
			
			clientSub.addListener(new RedisPubSubListener<byte[], byte[]>() {

				@Override
				public final void message(byte[] pattern, byte[] channel, byte[] message) {
					message(channel, message);
				}

				@Override
				public final void message(byte[] channel, byte[] message) {

					// TODO format?
					try {

						Tree parsed = new Tree(message, "json");
						Tree rawNode = parsed.get(RAW_VALUE);
						Object rawValue;
						if (rawNode == null) {
							rawValue = parsed;
						} else {
							rawValue = rawNode.asObject();
						}
						System.out.println(rawValue);

					} catch (Exception cause) {

						// TODO: invalid messaage format
					}
				}

				@Override
				public void subscribed(byte[] channel, long count) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void psubscribed(byte[] pattern, long count) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void unsubscribed(byte[] channel, long count) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void punsubscribed(byte[] pattern, long count) {
					// TODO Auto-generated method stub
					
				}

			});
			
			
		}
		clientPub = clientSub.async();
		
	}

	// --- DISCONNECT ---

	@Override
	public void disconnect() {
		clientPub = null;
		if (clientSub != null) {
			clientSub.close();
			clientSub = null;
		}
	}

	// --- SUBSCRIBE ---

	public void subscribe(String cmd, String nodeID) {
		if (clientPub != null) {
			clientPub.subscribe(getTopicName(cmd, nodeID).getBytes(StandardCharsets.UTF_8));		
		}
	}

	// --- PUBLISH ---

	public void publish(Packet packet) {
		if (clientPub != null) {

			byte[] channel = getTopicName(packet.type, packet.target).getBytes(StandardCharsets.UTF_8);

			// Convert Object to JSON / MessagePack / etc			
			byte[] bytes;
			if (packet.payload instanceof Tree) {
				bytes = ((Tree) packet.payload).toBinary("json", true);
			} else {
				HashMap<String, Object> map = new HashMap<>();
				map.put(RAW_VALUE, packet.payload);
				bytes = TreeWriterRegistry.getWriter("json").toBinary(map, null, true);
			}

			// Send in JSON / MessagePack / etc. format
			clientPub.publish(channel, bytes);
		}
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

	public StatefulRedisPubSubConnection<byte[], byte[]> getClientSub() {
		return clientSub;
	}

	public void setClientSub(StatefulRedisPubSubConnection<byte[], byte[]> clientSub) {
		this.clientSub = clientSub;
	}

	public RedisPubSubAsyncCommands<byte[], byte[]> getClientPub() {
		return clientPub;
	}

	public void setClientPub(RedisPubSubAsyncCommands<byte[], byte[]> clientPub) {
		this.clientPub = clientPub;
	}

}