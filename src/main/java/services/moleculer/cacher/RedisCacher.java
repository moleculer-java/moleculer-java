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

import io.datatree.Promise;
import io.datatree.Tree;
import rx.Observable;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.metrics.MetricCounter;
import services.moleculer.metrics.StoppableTimer;
import services.moleculer.serializer.JsonSerializer;
import services.moleculer.serializer.Serializer;
import services.moleculer.service.Name;
import services.moleculer.util.CheckedTree;
import services.moleculer.util.redis.RedisGetSetClient;

/**
 * Redis-based <b>distributed</b> cache. Supports SSL, clustering and password
 * authentication. It's the one of the fastest distributed cache. Supports
 * global and entry-level TTL configuration. <br>
 * <br>
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/biz.paluch.redis/lettuce<br>
 * compile group: 'biz.paluch.redis', name: 'lettuce', version: '4.5.0.Final'
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

	// --- DEFAULT TTL ---

	protected SetArgs expiration;

	// --- COUNTERS ---

	protected MetricCounter counterGet;
	protected MetricCounter counterSet;
	protected MetricCounter counterDel;
	protected MetricCounter counterClean;
	protected MetricCounter counterFound;
	
	// --- CONSTUCTORS ---

	public RedisCacher() {
	}

	public RedisCacher(String... urls) {
		this(null, 0, false, urls);
	}

	public RedisCacher(String password, int defaultTtl, boolean secure, String... urls) {
		this.password = password;
		this.ttl = defaultTtl;
		this.secure = secure;
		this.urls = urls;
	}

	// --- START CACHER ---

	/**
	 * Initializes cacher instance.
	 *
	 * @param broker
	 *            parent ServiceBroker
	 */
	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);
		serializer.started(broker);
		logger.info(nameOf(this, true) + " will use " + nameOf(serializer, true) + '.');

		// Get components
		ServiceBrokerConfig cfg = broker.getConfig();
		executor = cfg.getExecutor();
		scheduler = cfg.getScheduler();

		// Default ttl
		if (ttl > 0) {
			expiration = SetArgs.Builder.ex(ttl);
		} else {
			expiration = null;
		}

		// Create counters
		if (metrics != null) {
			counterGet = metrics.increment(MOLECULER_CACHER_GET_TOTAL, MOLECULER_CACHER_GET_TOTAL_DESC, 0);
			counterSet = metrics.increment(MOLECULER_CACHER_SET_TOTAL, MOLECULER_CACHER_SET_TOTAL_DESC, 0);
			counterDel = metrics.increment(MOLECULER_CACHER_DEL_TOTAL, MOLECULER_CACHER_DEL_TOTAL_DESC, 0);
			counterClean = metrics.increment(MOLECULER_CACHER_CLEAN_TOTAL, MOLECULER_CACHER_CLEAN_TOTAL_DESC, 0);
			counterFound = metrics.increment(MOLECULER_CACHER_FOUND_TOTAL, MOLECULER_CACHER_FOUND_TOTAL_DESC, 0);
		}
		
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
		}).catchError(cause -> {
			logger.error("Unable to disconnect Redis server!", cause);
			return Promise.resolve();
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
		}
	}

	// --- CACHE METHODS ---

	@Override
	public Promise get(String key) {
		if (status.get() == STATUS_CONNECTED) {
			
			// Metrics
			StoppableTimer getTimer;
			if (metrics == null) {
				getTimer = null;
			} else {
				counterGet.increment();
				getTimer = metrics.timer(MOLECULER_CACHER_GET_TIME, MOLECULER_CACHER_GET_TIME_DESC);
			}
			
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
					if (getTimer != null) {
						getTimer.stop();
					}
					return Promise.resolve((Object) null);
				}).catchError(err -> {
					if (getTimer != null) {
						getTimer.stop();
					}					
					return err;
				});
			} catch (Exception cause) {
				if (getTimer != null) {
					getTimer.stop();
				}				
				logger.warn("Unable to get data from Redis!", cause);
			}
		}
		return Promise.resolve((Object) null);
	}

	@Override
	public Promise set(String key, Tree value, int ttl) {
		if (status.get() == STATUS_CONNECTED) {

			// Metrics
			StoppableTimer setTimer;
			if (metrics == null) {
				setTimer = null;
			} else {
				counterSet.increment();
				setTimer = metrics.timer(MOLECULER_CACHER_SET_TIME, "Response time for cache SET operations");
			}

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
				byte[] bytes = serializer.write(root);
				if (setTimer == null) {
					return client.set(key, bytes, args);
				}
				return client.set(key, bytes, args).then(rsp -> {
					setTimer.stop();
					return rsp;
				}).catchError(err -> {
					setTimer.stop();
					return err;
				});
			} catch (Exception cause) {
				if (setTimer != null) {
					setTimer.stop();
				}
				logger.warn("Unable to put data into Redis!", cause);
			}
		}
		return Promise.resolve();
	}

	@Override
	public Promise del(String key) {
		if (status.get() == STATUS_CONNECTED) {
			
			// Metrics
			StoppableTimer delTimer;
			if (metrics == null) {
				delTimer = null;
			} else {
				counterDel.increment();
				delTimer = metrics.timer(MOLECULER_CACHER_DEL_TIME, "Response time for cache DEL operations");
			}
			
			try {
				if (delTimer == null) {
					return client.del(key);
				}
				return client.del(key).then(rsp -> {
					delTimer.stop();
					return rsp;
				}).catchError(err -> {
					delTimer.stop();
					return err;
				});				
			} catch (Exception cause) {
				if (delTimer != null) {
					delTimer.stop();
				}
				logger.warn("Unable to delete data from Redis!", cause);
			}
		}
		return Promise.resolve();
	}

	@Override
	public Promise clean(String match) {
		if (status.get() == STATUS_CONNECTED) {
			
			// Metrics
			StoppableTimer cleanTimer;
			if (metrics == null) {
				cleanTimer = null;
			} else {
				counterClean.increment();
				cleanTimer = metrics.timer(MOLECULER_CACHER_CLEAN_TIME, "Response time for cache CLEAN operations");
			}
			
			try {
				if (cleanTimer == null) {
					return client.clean(match);
				}
				return client.clean(match).then(rsp -> {
					cleanTimer.stop();
					return rsp;
				}).catchError(err -> {
					cleanTimer.stop();
					return err;
				});	
			} catch (Exception cause) {
				if (cleanTimer != null) {
					cleanTimer.stop();
				}
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

	public void setUrls(String... urls) {
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