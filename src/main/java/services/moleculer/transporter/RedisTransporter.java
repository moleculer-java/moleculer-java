package services.moleculer.transporter;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.connection.ConnectionActivatedEvent;
import com.lambdaworks.redis.event.connection.ConnectionDeactivatedEvent;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;

import io.datatree.Tree;
import rx.Observable;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;
import services.moleculer.util.redis.RedisPubSubClient;

@Name("Redis Transporter")
public class RedisTransporter extends Transporter implements EventBus, RedisPubSubListener<byte[], byte[]> {

	// --- PROPERTIES ---

	private String[] urls = new String[] { "localhost" };
	private String password;
	private boolean useSSL;
	private boolean startTLS;

	// --- REDIS CLIENTS ---

	private RedisPubSubClient clientSub;
	private RedisPubSubClient clientPub;

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

	// --- START TRANSPORTER ---

	/**
	 * Initializes transporter instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public final void start(ServiceBroker broker, Tree config) throws Exception {

		// Process basic properties (eg. "prefix")
		super.start(broker, config);

		// Process config
		Tree urlNode = config.get("urls");
		if (urlNode == null) {
			urlNode = config.get("url");
		}
		if (urlNode != null) {
			List<String> urlList;
			if (urlNode.isPrimitive()) {
				urlList = new LinkedList<>();
				String url = urlNode.asString().trim();
				if (!url.isEmpty()) {
					urlList.add(url);
				}
			} else {
				urlList = urlNode.asList(String.class);
			}
			if (!urlList.isEmpty()) {
				urls = new String[urlList.size()];
				urlList.toArray(urls);
			}
		}
		password = config.get("password", password);
		useSSL = config.get("useSSL", useSSL);
		startTLS = config.get("startTLS", startTLS);

		// Connect to Redis server
		connect();
	}

	// --- CONNECT ---

	private final void connect() {

		// Create redis clients
		clientSub = new RedisPubSubClient(urls, password, useSSL, startTLS, executor, this, this);
		clientPub = new RedisPubSubClient(urls, password, useSSL, startTLS, executor, this, null);

		// Connect
		clientSub.connect().then(subConnected -> {

			// Ok, sub connected
			logger.info("Redis-sub client is connected.");

			clientPub.connect().then(pubConnected -> {

				// Ok, all connected
				logger.info("Redis-pub client is connected.");
				connected();

			}).Catch(error -> {

				// Failed
				logger.warn("Redis-pub error (" + error.getMessage() + ")!");
				reconnect();

			});
		}).Catch(error -> {

			// Failed
			logger.warn("Redis-sub error (" + error.getMessage() + ")!");
			reconnect();

		});
	}

	// --- DISCONNECT ---

	private final Promise disconnect() {
		List<Promise> promises = new LinkedList<>();
		if (clientSub != null) {
			promises.add(clientSub.disconnect());
		}
		if (clientPub != null) {
			promises.add(clientPub.disconnect());
		}
		return Promise.all(promises);
	}

	// --- RECONNECT ---

	protected final void reconnect() {
		disconnect().then(ok -> {
			scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
		}).Catch(error -> {
			scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
		});
	}

	// --- STOP TRANSPORTER ---

	/**
	 * Closes transporter.
	 */
	@Override
	public final void stop() {
		disconnect();
	}

	// --- SUBSCRIBE ---

	@Override
	public final Promise subscribe(String channel) {
		return clientSub.subscribe(channel);
	}

	// --- PUBLISH ---

	@Override
	public final void publish(String channel, Tree message) {
		try {
			clientPub.publish(channel, serializer.write(message));
		} catch (Exception cause) {
			logger.warn("Unable to send message to Redis!", cause);
		}
	}

	// --- REDIS EVENT LISTENER METHODS ---

	@Override
	public final void message(byte[] channel, byte[] message) {
		received(new String(channel, StandardCharsets.UTF_8), message);
	}

	@Override
	public final void message(byte[] pattern, byte[] channel, byte[] message) {
		received(new String(channel, StandardCharsets.UTF_8), message);
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

	@Override
	public final Observable<Event> get() {
		return null;
	}

	@Override
	public final void publish(Event event) {
		
		// Connected to Redis server
		if (event instanceof ConnectionActivatedEvent) {
			connected();
			return;
		}

		// Disconnected from Redis server
		if (event instanceof ConnectionDeactivatedEvent) {
			disconnected();
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

}
