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

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.datatree.Promise;
import io.datatree.Tree;
import net.sf.xenqtt.client.AsyncClientListener;
import net.sf.xenqtt.client.AsyncMqttClient;
import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.MqttClientConfig;
import net.sf.xenqtt.client.NullReconnectStrategy;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;
import services.moleculer.service.Name;

/**
 * MQTT Transporter (eg. for Mosquitto MQTT Server or ActiveMQ Server). MQTT is
 * a machine-to-machine (M2M)/"Internet of Things" connectivity protocol. It was
 * designed as an extremely lightweight publish/subscribe messaging transport
 * (website: http://mqtt.org). Usage:
 * <pre>
 * ServiceBroker broker = ServiceBroker.builder().nodeID("node1")
 * .transporter(new MqttTransporter("localhost")).build();
 * </pre>
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/net.sf.xenqtt/xenqtt<br>
 * compile group: 'net.sf.xenqtt', name: 'xenqtt', version: '0.9.7'
 *
 * @see TcpTransporter
 * @see RedisTransporter
 * @see NatsTransporter
 * @see NatsStreamingTransporter
 * @see JmsTransporter
 * @see KafkaTransporter
 * @see AmqpTransporter
 */
@Name("MQTT Transporter")
public class MqttTransporter extends Transporter implements AsyncClientListener {

	// --- PROPERTIES ---

	protected String username;
	protected String password;
	protected String url = "localhost";

	// --- OTHER MQTT PROPERTIES ---

	protected boolean cleanSession = true;
	protected short keepAliveSeconds = 300;
	protected int connectTimeoutSeconds = 30;
	protected int messageResendIntervalSeconds = 30;
	protected int blockingTimeoutSeconds = 0;
	protected int maxInFlightMessages = 0xffff;
	protected QoS qos = QoS.AT_MOST_ONCE;

	// --- SUBSCRIPTIONS ---

	protected final ConcurrentHashMap<String, Promise> subscriptions = new ConcurrentHashMap<>();

	// --- MQTT CONNECTION ---

	protected AsyncMqttClient client;

	// --- CONSTUCTORS ---

	public MqttTransporter() {
	}

	public MqttTransporter(String url) {
		this.url = url;
	}

	public MqttTransporter(String username, String password, String url) {
		this.username = username;
		this.password = password;
		this.url = url;
	}

	// --- CONNECT ---

	@Override
	public void connect() {
		try {

			// Create MQTT client config
			final MqttClientConfig config = new MqttClientConfig();
			String uri = url;
			if (uri.indexOf(':') == -1) {
				uri = uri + ":1883";
			}
			uri = uri.replace("mqtt://", "tcp://");
			if (url.indexOf("://") == -1) {
				uri = "tcp://" + uri;
			}

			// Set properties
			config.setReconnectionStrategy(new NullReconnectStrategy());
			config.setKeepAliveSeconds(keepAliveSeconds);
			config.setConnectTimeoutSeconds(connectTimeoutSeconds);
			config.setMessageResendIntervalSeconds(messageResendIntervalSeconds);
			config.setBlockingTimeoutSeconds(blockingTimeoutSeconds);
			config.setMaxInFlightMessages(maxInFlightMessages);

			// Create MQTT client
			disconnect();
			client = new AsyncMqttClient(uri, this, executor, config);
			client.connect(nodeID + '-' + broker.getConfig().getUidGenerator().nextUID(), cleanSession, username,
					password);

		} catch (Exception cause) {
			reconnect(cause);
		}
	}

	// --- CONNECTED ---

	@Override
	public void connected(MqttClient client, ConnectReturnCode returnCode) {
		logger.info("MQTT pub-sub connection estabilished.");
		scheduler.schedule(this::connected, 100, TimeUnit.MILLISECONDS);
	}

	// --- DISCONNECT ---

	@Override
	public void disconnected(MqttClient client, Throwable cause, boolean reconnecting) {
		if (cause != null) {
			String msg = String.valueOf(cause).toLowerCase();
			if (!msg.contains("refused")) {
				logger.info("MQTT pub-sub connection aborted.");
			}
			reconnect(cause);
		}
	}

	protected void disconnect() {
		boolean notify = false;
		if (client != null) {
			notify = true;
			try {
				if (!client.isClosed()) {
					client.disconnect();
					client.close();
				}
			} catch (Throwable cause) {
				logger.warn("Unexpected error occurred while closing MQTT client!", cause);
			} finally {
				client = null;
				subscriptions.clear();
			}
			try {
				Thread.sleep(500);
				ThreadGroup group = Thread.currentThread().getThreadGroup();
				Thread[] list = new Thread[group.activeCount() + 10];
				group.enumerate(list);
				for (Thread t : list) {
					if (t == null) {
						continue;
					}
					if ("CommandCleanup".equals(t.getName())) {
						t.interrupt();
					}
				}
			} catch (Exception ignored) {
			}
		}
		
		// Notify internal listeners
		if (notify) {
			broadcastTransporterDisconnected();
		}
	}

	// --- RECONNECT ---

	protected void reconnect(Throwable cause) {
		if (cause != null) {
			String msg = cause.getMessage();
			if (msg == null || msg.isEmpty()) {
				msg = "Unable to connect to MQTT server!";
			} else if (!msg.endsWith("!") && !msg.endsWith(".")) {
				msg += "!";
			}
			logger.warn(msg);
		}
		disconnect();
		logger.info("Trying to reconnect...");
		scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
	}

	// --- ANY I/O ERROR ---

	@Override
	protected void error(Throwable cause) {
		reconnect(cause);
	}

	// --- STOP TRANSPORTER ---

	/**
	 * Closes transporter.
	 */
	@Override
	public void stopped() {

		// Stop timers
		super.stopped();

		// Disconnect
		disconnect();
	}

	// --- SUBSCRIBE ---

	@Override
	public Promise subscribe(String channel) {
		Promise promise = new Promise();
		if (client != null) {
			try {
				client.subscribe(new Subscription[] { new Subscription(channel, qos) });
				subscriptions.put(channel, promise);
			} catch (Exception cause) {
				promise.complete(cause);
			}
		} else {
			promise.complete(new Throwable("Not connected!"));
		}
		return promise;
	}

	@Override
	public void subscribed(MqttClient client, Subscription[] requestedSubscriptions,
			Subscription[] grantedSubscriptions, boolean requestsGranted) {
		for (Subscription s : grantedSubscriptions) {
			String channel = s.getTopic();
			Promise promise = subscriptions.remove(channel);
			if (promise != null) {
				promise.complete();
			}
			if (debug) {
				logger.info("Channel \"" + channel + "\" subscribed successfully.");
			}
		}
	}

	@Override
	public void unsubscribed(MqttClient client, String[] topics) {
		if (debug) {
			for (String s : topics) {
				logger.info("Channel \"" + s + "\" unsubscribed successfully.");
			}
		}
	}

	// --- PUBLISH ---

	@Override
	public void publish(String channel, Tree message) {
		if (client != null) {
			try {
				if (debug && (debugHeartbeats || !channel.endsWith(heartbeatChannel))) {
					logger.info("Submitting message to channel \"" + channel + "\":\r\n" + message.toString());
				}
				
				// Metrics
				byte[] bytes = serializer.write(message);
				if (metrics != null) {
					metrics.increment(MOLECULER_TRANSPORTER_PACKETS_SENT_TOTAL, "Number of sent packets");
					metrics.increment(MOLECULER_TRANSPORTER_PACKETS_SENT_BYTES, "Amount of total bytes sent",
							bytes.length);
				}
				
				// Send
				client.publish(new PublishMessage(channel, qos, bytes, false));
			} catch (Exception cause) {
				logger.warn("Unable to send message to MQTT server!", cause);
			}
		}
	}

	@Override
	public void published(MqttClient client, PublishMessage message) {
		if (debugHeartbeats) {
			logger.info(
					"Submitted message received by the server at " + new Date(message.getReceivedTimestamp()) + ".");
		}
	}

	// --- RECEIVE ---

	@Override
	public void publishReceived(MqttClient client, PublishMessage message) {

		// We are running in the shared executor's pool,
		// do not create new task.
		processReceivedMessage(message.getTopic(), message.getPayload());
	}

	// --- GETTERS / SETTERS ---

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}

	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public short getKeepAliveSeconds() {
		return keepAliveSeconds;
	}

	public void setKeepAliveSeconds(short keepAliveInterval) {
		this.keepAliveSeconds = keepAliveInterval;
	}

	public int getConnectTimeoutSeconds() {
		return connectTimeoutSeconds;
	}

	public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
		this.connectTimeoutSeconds = connectTimeoutSeconds;
	}

	public int getMessageResendIntervalSeconds() {
		return messageResendIntervalSeconds;
	}

	public void setMessageResendIntervalSeconds(int messageResendIntervalSeconds) {
		this.messageResendIntervalSeconds = messageResendIntervalSeconds;
	}

	public int getBlockingTimeoutSeconds() {
		return blockingTimeoutSeconds;
	}

	public void setBlockingTimeoutSeconds(int blockingTimeoutSeconds) {
		this.blockingTimeoutSeconds = blockingTimeoutSeconds;
	}

	public int getMaxInFlightMessages() {
		return maxInFlightMessages;
	}

	public void setMaxInFlightMessages(int maxInFlightMessages) {
		this.maxInFlightMessages = maxInFlightMessages;
	}

	public QoS getQos() {
		return qos;
	}

	public void setQos(QoS qos) {
		this.qos = qos;
	}

}