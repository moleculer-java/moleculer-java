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
package services.moleculer.cacher;

import static services.moleculer.util.CommonUtils.nameOf;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.lambdaworks.redis.SetArgs;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.connection.ConnectedEvent;
import com.lambdaworks.redis.event.connection.DisconnectedEvent;

import io.datatree.Tree;
import rx.Observable;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.serializer.JsonSerializer;
import services.moleculer.serializer.Serializer;
import services.moleculer.service.Name;
import services.moleculer.util.CheckedTree;
import services.moleculer.util.redis.RedisGetSetClient;

/**
 * Redis-based <b>distributed</b> cache. Supports SSL, clustering and password
 * authentication. It's the one of the fastest distributed cache. Supports
 * global and entry-level TTL configuration. <br>
 *
 * @see MemoryCacher
 * @see OHCacher
 */
@Name("Redis Cacher")
public class RedisCacher extends DistributedCacher implements EventBus {

	// --- LIST OF STATUS CODES ---

	protected static final int STATUS_DISCONNECTING = 1;
	protected static final int STATUS_DISCONNECTED = 2;
	protected static final int STATUS_CONNECTING = 3;
	protected static final int STATUS_CONNECTED = 4;
	protected static final int STATUS_STOPPED = 5;

	// --- CONNECTION STATUS ---

	protected final AtomicInteger status = new AtomicInteger(STATUS_DISCONNECTED);

	// --- PROPERTIES ---

	protected String password;
	protected int ttl;
	protected boolean secure;
	protected String[] urls = new String[] { "localhost" };

	// --- REDIS CLIENT ---

	protected RedisGetSetClient client;

	// --- SERIALIZER / DESERIALIZER ---

	protected Serializer serializer = new JsonSerializer();

	// --- COMPONENTS ---

	protected ExecutorService executor;
	protected ScheduledExecutorService scheduler;

	// --- CONSTUCTORS ---

	public RedisCacher() {
	}

	public RedisCacher(String... urls) {
		this(null, 0, false, urls);
	}

	public RedisCacher(String password, int ttl, boolean secure, String... urls) {
		this.password = password;
		this.ttl = ttl;
		this.secure = secure;
		this.urls = urls;
	}

	// --- START CACHER ---

	protected SetArgs expiration;

	/**
	 * Initializes cacher instance.
	 *
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);
		logger.info(nameOf(this, true) + " will use " + nameOf(serializer, true) + '.');

		// Get components
		executor = broker.getConfig().getExecutor();
		scheduler = broker.getConfig().getScheduler();

		// Connect to Redis server
		connect();
	}

	// --- CONNECT ---

	protected void connect() {
		status.set(STATUS_CONNECTING);

		// Create redis client
		client = new RedisGetSetClient(urls, password, secure, executor, this);

		// Connecting to Redis...
		try {
			client.connect();
		} catch (Exception cause) {
			String msg = cause.getMessage();
			if (msg == null || msg.isEmpty()) {
				msg = "Unable to connect to Redis server!";
			} else if (!msg.endsWith("!") && !msg.endsWith(".")) {
				msg += "!";
			}
			logger.warn(msg);
			reconnect();
			return;
		}
	}

	// --- DISCONNECT ---

	protected Promise disconnect() {
		if (client == null) {
			status.set(STATUS_DISCONNECTED);
			return Promise.resolve();
		}
		status.set(STATUS_DISCONNECTING);
		return client.disconnect().then(ok -> {
			status.set(STATUS_DISCONNECTED);
		});
	}

	// --- RECONNECT ---

	protected void reconnect() {
		disconnect().then(ok -> {
			logger.info("Trying to reconnect...");
			scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
		}).catchError(cause -> {
			logger.warn("Unable to disconnect from Redis server!", cause);
			scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
		});
	}

	// --- CLOSE CACHE INSTANCE ---

	@Override
	public void stopped() {
		int s = status.getAndSet(STATUS_STOPPED);
		if (s != STATUS_STOPPED) {
			disconnect();
		} else {
			throw new IllegalStateException("Redis Cacher is already stopped!");
		}
	}

	// --- CACHE METHODS ---

	@Override
	public Promise get(String key) {
		if (status.get() == STATUS_CONNECTED) {
			try {
				return client.get(key).then(in -> {
					if (in != null) {
						byte[] source = in.asBytes();
						if (source != null) {
							try {
								Tree root = serializer.read(source);
								Tree content = root.get(CONTENT);
								if (content != null) {
									return content;
								}
								return root;
							} catch (Exception cause) {
								logger.warn("Unable to deserialize cached data!", cause);
							}
						}
					}
					return Promise.resolve();
				});
			} catch (Exception cause) {
				logger.warn("Unable to get data from Redis!", cause);
			}
		}
		return Promise.resolve();
	}

	@Override
	public Promise set(String key, Tree value, int ttl) {
		if (status.get() == STATUS_CONNECTED) {
			try {
				SetArgs args;
				if (ttl > 0) {

					// Entry-level TTL (in seconds)
					args = SetArgs.Builder.ex(ttl);
				} else {

					// Use the default TTL
					args = expiration;
				}
				Tree root = new CheckedTree(Collections.singletonMap(CONTENT, value.asObject()));
				return client.set(key, serializer.write(root), args);
			} catch (Exception cause) {
				logger.warn("Unable to put data into Redis!", cause);
			}
		}
		return Promise.resolve();
	}

	@Override
	public Promise del(String key) {
		if (status.get() == STATUS_CONNECTED) {
			try {
				return client.del(key);
			} catch (Exception cause) {
				logger.warn("Unable to delete data from Redis!", cause);
			}
		}
		return Promise.resolve();
	}

	@Override
	public Promise clean(String match) {
		if (status.get() == STATUS_CONNECTED) {
			try {
				return client.clean(match);
			} catch (Exception cause) {
				logger.warn("Unable to delete data from Redis!", cause);
			}
		}
		return Promise.resolve();
	}

	// --- REDIS EVENT LISTENER METHODS ---

	@Override
	public void publish(Event event) {

		// Check state
		if (status.get() == STATUS_STOPPED) {
			return;
		}

		// Connected
		if (event instanceof ConnectedEvent) {
			if (status.compareAndSet(STATUS_CONNECTING, STATUS_CONNECTED)) {

				// Redis connection is Ok
				logger.info("Redis get-set connection are estabilished.");
			}
			return;
		}

		// Disconnected
		if (event instanceof DisconnectedEvent) {
			int s = status.getAndSet(STATUS_DISCONNECTED);
			if (s != STATUS_DISCONNECTED) {
				logger.info("Redis get-set connection aborted.");
				reconnect();
			}
		}

	}

	@Override
	public Observable<Event> get() {
		return null;
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

	public boolean isSecure() {
		return secure;
	}

	public void setSecure(boolean useSSL) {
		this.secure = useSSL;
	}

	public int getTtl() {
		return ttl;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public Serializer getSerializer() {
		return serializer;
	}

	public void setSerializer(Serializer serializer) {
		this.serializer = Objects.requireNonNull(serializer);
	}

}