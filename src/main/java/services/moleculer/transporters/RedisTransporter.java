package services.moleculer.transporters;

import java.nio.charset.StandardCharsets;
import java.util.List;

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

import io.datatree.Tree;
import rx.Observable;
import services.moleculer.utils.RedisUtilities;

public final class RedisTransporter extends Transporter {

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
	public final void connect() {
		if (clientSub == null) {

			// Create Redis connection
			List<RedisURI> redisURIs = RedisUtilities.parseURLs(urls, password, useSSL, startTLS);
			DefaultClientResources clientResources = RedisUtilities.createClientResources(new EventBus() {

				@Override
				public final void publish(Event event) {
					
					// TODO on connected (move to superclass):
					
					// Call `makeSubscriptions`
					// Call `discoverNodes`
					// Call `sendNodeInfo`
					// Set `this.connected = true`
					
					// Call `this.tx.connect()`
					// If failed, try again after 5 sec.
					// If success
					// 		Start heartbeat timer
					//		Start checkNodes timer 
					
					// Subscribe to broadcast events
					//this.subscribe(P.PACKET_EVENT),

					// Subscribe to requests
					//this.subscribe(P.PACKET_REQUEST, this.nodeID),

					// Subscribe to node responses of requests
					//this.subscribe(P.PACKET_RESPONSE, this.nodeID),

					// Discover handler
					//this.subscribe(P.PACKET_DISCOVER),

					// NodeInfo handler
					//this.subscribe(P.PACKET_INFO), // Broadcasted INFO. If a new node connected
					//this.subscribe(P.PACKET_INFO, this.nodeID), // Response INFO to DISCOVER packet

					// Disconnect handler
					//this.subscribe(P.PACKET_DISCONNECT),

					// Heart-beat handler
					//this.subscribe(P.PACKET_HEARTBEAT),	
					
					// TODO on disconnected (move to superclass):
					
					// Stop heartbeat timer
					// Stop checkNodes timer
					// Call `sendDisconnectPacket()`
					// Call `this.tx.disconnect()`
				}

				@Override
				public final Observable<Event> get() {
					return null;
				}

			});
			if (urls.length > 1) {

				// Clustered client
				RedisClusterClient client = RedisClusterClient.create(clientResources, redisURIs);
				clientSub = client.connectPubSub(new ByteArrayCodec());

			} else {

				// Single connection
				RedisClient client = RedisClient.create(clientResources, redisURIs.get(0));
				clientSub = client.connectPubSub(new ByteArrayCodec());

			}

			// Add listener
			clientSub.addListener(new RedisPubSubListener<byte[], byte[]>() {

				@Override
				public final void message(byte[] pattern, byte[] channel, byte[] message) {
					message(channel, message);
				}

				@Override
				public final void message(byte[] channel, byte[] message) {
					try {

						// TODO Parse packet (move to superclass)

						/*
						 * 1. Deserialize data 2. Check sender !== this.nodeID
						 * 3. if PACKET_REQUEST call `this.requestHandler` 4. if
						 * PACKET_RESPONSE call `this.responseHandler` 5. if
						 * PACKET_EVENT call `this.broker.emitLocal` 6. if
						 * PACKET_INFO || PACKET_DISCOVER call
						 * `this.processNodeInfo` 7. if PACKET_DISCONNECT call
						 * `this.nodeDisconnected` 8. if PACKET_HEARTBEAT call
						 * `this.nodeHeartbeat` 8. else throw Invalid packet!
						 */

						Tree deserialized = new Tree(message, "json");
						Object incoming;
						if (deserialized.isPrimitive()) {
							incoming = deserialized.asObject();
						} else {
							incoming = deserialized;
						}

					} catch (Exception cause) {

						// TODO: invalid messaage format
					}
				}

				@Override
				public final void subscribed(byte[] channel, long count) {
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
		}
		clientPub = clientSub.async();
	}

	// --- DISCONNECT ---

	@Override
	public final void disconnect() {
		clientPub = null;
		if (clientSub != null) {
			clientSub.close();
			clientSub = null;
		}
	}

	// --- SUBSCRIBE ---

	@Override
	public final void subscribe(String cmd, String nodeID) {
		if (clientPub != null) {
			clientPub.subscribe(nameOf(cmd, nodeID).getBytes(StandardCharsets.UTF_8));
		}
	}

	// --- PUBLISH ---

	@Override
	public final void publish(String cmd, String nodeID, Object payload) {
		if (clientPub != null) {

			// TODO Serialize packet (move to superclass)
			byte[] channel = nameOf(cmd, nodeID).getBytes(StandardCharsets.UTF_8);

			// Convert Object to JSON / MessagePack / etc
			Tree structure;
			byte[] outgoing;
			if (payload instanceof Tree) {
				structure = (Tree) payload;
			} else {
				structure = new Tree().setObject(payload);
			}
			outgoing = structure.toBinary("json", true);

			// Send in JSON / MessagePack / etc. format
			clientPub.publish(channel, outgoing);
		}
	}

	// --- GETTERS / SETTERS ---

	public final String[] getUrls() {
		return urls;
	}

	public final void setUrls(String[] urls) {
		this.urls = urls;
	}

	public final String getPassword() {
		return password;
	}

	public final void setPassword(String password) {
		this.password = password;
	}

	public final boolean isUseSSL() {
		return useSSL;
	}

	public final void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

	public final boolean isStartTLS() {
		return startTLS;
	}

	public final void setStartTLS(boolean startTLS) {
		this.startTLS = startTLS;
	}

	public final StatefulRedisPubSubConnection<byte[], byte[]> getClientSub() {
		return clientSub;
	}

	public final void setClientSub(StatefulRedisPubSubConnection<byte[], byte[]> clientSub) {
		this.clientSub = clientSub;
	}

	public final RedisPubSubAsyncCommands<byte[], byte[]> getClientPub() {
		return clientPub;
	}

	public final void setClientPub(RedisPubSubAsyncCommands<byte[], byte[]> clientPub) {
		this.clientPub = clientPub;
	}

}