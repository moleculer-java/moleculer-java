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
package services.moleculer.transporter;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.connection.ConnectedEvent;
import com.lambdaworks.redis.event.connection.DisconnectedEvent;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;

import io.datatree.Promise;
import io.datatree.Tree;
import rx.Observable;
import services.moleculer.service.Name;
import services.moleculer.util.redis.RedisPubSubClient;

/**
 * Redis Transporter. Redis is an open source (BSD licensed), in-memory data
 * structure store, used as a database, cache and message broker (website:
 * https://redis.io). Usage:
 * <pre>
 * ServiceBroker broker = ServiceBroker.builder().nodeID("node1")
 * .transporter(new RedisTransporter("localhost")).build();
 * </pre>
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/biz.paluch.redis/lettuce<br>
 * compile group: 'biz.paluch.redis', name: 'lettuce', version: '4.5.0.Final'
 *
 * @see AblyTransporter
 * @see TcpTransporter
 * @see NatsTransporter
 * @see MqttTransporter
 * @see JmsTransporter
 * @see GoogleTransporter
 * @see KafkaTransporter
 * @see AmqpTransporter
 */
@Name("Redis Transporter")
public class RedisTransporter extends Transporter implements EventBus, RedisPubSubListener<byte[], byte[]> {

	// --- LIST OF STATUS CODES ---

	protected static final int STATUS_DISCONNECTING = 1;
	protected static final int STATUS_DISCONNECTED = 2;
	protected static final int STATUS_CONNECTING_1 = 3;
	protected static final int STATUS_CONNECTING_2 = 4;
	protected static final int STATUS_CONNECTED = 5;

	// --- CONNECTION STATUS ---

	protected final AtomicInteger status = new AtomicInteger(STATUS_DISCONNECTED);

	// --- PROPERTIES ---

	protected String password;
	protected boolean secure;
	protected String[] urls = { "localhost" };

	// --- REDIS CLIENTS ---

	protected RedisPubSubClient clientSub;
	protected RedisPubSubClient clientPub;

	// --- CONSTUCTORS ---

	public RedisTransporter() {
	}

	public RedisTransporter(String... urls) {
		this.urls = urls;
	}

	public RedisTransporter(String password, boolean secure, String... urls) {
		this.password = password;
		this.secure = secure;
		this.urls = urls;
	}

	// --- CONNECT ---

	@Override
	public void connect() {
		if (clientSub != null || clientPub != null) {
			disconnect();
		}
		status.set(STATUS_CONNECTING_1);

		// Create redis clients
		clientSub = new RedisPubSubClient(urls, password, secure, executor, this, this);
		clientPub = new RedisPubSubClient(urls, password, secure, executor, this, null);

		// Connect sub
		try {
			clientSub.connect();
		} catch (Exception cause) {
			unableToConnect(cause);
			return;
		}

		// Connect pub
		try {
			clientPub.connect();
		} catch (Exception cause) {
			unableToConnect(cause);
		}
	}

	protected void unableToConnect(Exception cause) {
		String msg = cause.getMessage();
		if (msg == null || msg.isEmpty()) {
			msg = "Unable to connect to Redis server!";
		} else if (!msg.endsWith("!") && !msg.endsWith(".")) {
			msg += "!";
		}
		logger.warn(msg);
		reconnect();
	}

	// --- DISCONNECT ---

	protected Promise disconnect() {
		boolean notify = false;
		int s = status.get();
		if (s != STATUS_DISCONNECTED && s != STATUS_DISCONNECTING) {
			notify = true;
			status.set(STATUS_DISCONNECTING);
			if (clientSub != null) {
				clientSub.disconnect();
			}
			if (clientPub != null) {
				clientPub.disconnect();
			}
			status.set(STATUS_DISCONNECTED);			
		}
		
		// Notify internal listeners
		if (notify) {
			broadcastTransporterDisconnected();
		}
		
		return Promise.resolve();
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

	// --- ANY I/O ERROR ---

	@Override
	protected void error(Throwable cause) {
		logger.warn("Unexpected communication error occurred!", cause);
		reconnect();
	}

	// --- STOP TRANSPORTER ---

	/**
	 * Closes transporter.
	 */
	@Override
	public void stopped() {
		int s = status.get();
		if (s != STATUS_DISCONNECTED && s != STATUS_DISCONNECTING) {

			// Stop timers
			super.stopped();

			// Disconnect
			disconnect();

		}
	}

	// --- SUBSCRIBE ---

	@Override
	public Promise subscribe(String channel) {
		if (status.get() == STATUS_CONNECTED) {
			return clientSub.subscribe(channel);
		}
		return Promise.resolve();
	}

	// --- PUBLISH ---

	@Override
	public void publish(String channel, Tree message) {
		if (status.get() == STATUS_CONNECTED) {
			try {
				if (debug && (debugHeartbeats || !channel.endsWith(heartbeatChannel))) {
					logger.info("Submitting message to channel \"" + channel + "\":\r\n" + message.toString());
				}
				clientPub.publish(channel, serializer.write(message));
			} catch (Exception cause) {
				logger.warn("Unable to send message to Redis!", cause);
				reconnect();
			}
		}
	}

	// --- REDIS MESSAGE LISTENER METHODS ---

	@Override
	public void message(byte[] channel, byte[] message) {

		// Send messages into the shared executor
		received(new String(channel, StandardCharsets.UTF_8), message);
	}

	@Override
	public void message(byte[] pattern, byte[] channel, byte[] message) {
		received(new String(channel, StandardCharsets.UTF_8), message);
	}

	@Override
	public void subscribed(byte[] channel, long count) {

		// Do nothing
	}

	@Override
	public void psubscribed(byte[] pattern, long count) {

		// Do nothing
	}

	@Override
	public void unsubscribed(byte[] channel, long count) {

		// Do nothing
	}

	@Override
	public void punsubscribed(byte[] pattern, long count) {

		// Do nothing
	}

	// --- REDIS EVENT LISTENER METHODS ---

	@Override
	public void publish(Event event) {

		// Connected
		if (event instanceof ConnectedEvent) {
			if (status.compareAndSet(STATUS_CONNECTING_1, STATUS_CONNECTING_2)) {

				// First connection is Ok
				return;
			}
			if (status.compareAndSet(STATUS_CONNECTING_2, STATUS_CONNECTED)) {

				// Second connection is Ok
				logger.info("All Redis pub-sub connections estabilished.");
				connected(false);
			}
			return;
		}

		// Disconnected
		if (event instanceof DisconnectedEvent) {
			int s = status.getAndSet(STATUS_DISCONNECTED);
			if (s != STATUS_DISCONNECTED && s != STATUS_DISCONNECTING) {
				logger.info("Redis pub-sub connection aborted.");
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

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

}