/**
 * MOLECULER MICROSERVICES FRAMEWORK<br>
 * <br>
 * This project is based on the idea of Moleculer Microservices
 * Framework for NodeJS (https://moleculer.services). Special thanks to
 * the Moleculer's project owner (https://github.com/icebob) for the
 * consultations.<br>
 * <br>
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
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

import static services.moleculer.util.CommonUtils.parseURLs;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * MQTT Transporter (eg. for Mosquitto MQTT Server or ActiveMQ Server). MQTT is
 * a machine-to-machine (M2M)/"Internet of Things" connectivity protocol. It was
 * designed as an extremely lightweight publish/subscribe messaging transport
 * (website: http://mqtt.org).<br>
 * <br>
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/net.sf.xenqtt/xenqtt<br>
 * compile group: 'net.sf.xenqtt', name: 'xenqtt', version: '0.9.7'
 * 
 * @see RedisTransporter
 * @see NatsTransporter
 * @see AmqpTransporter
 * @see JmsTransporter
 * @see GoogleTransporter
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
	protected QoS qos = QoS.AT_LEAST_ONCE;

	// --- MQTT CONNECTION ---

	protected AsyncMqttClient client;

	// --- CONSTUCTORS ---

	public MqttTransporter() {
		super();
	}

	public MqttTransporter(String prefix) {
		super(prefix);
	}

	public MqttTransporter(String prefix, String url) {
		super(prefix);
		this.url = url;
	}

	public MqttTransporter(String prefix, String username, String password, String url) {
		super(prefix);
		this.username = username;
		this.password = password;
		this.url = url;
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
	public void start(ServiceBroker broker, Tree config) throws Exception {

		// Process basic properties (eg. "prefix")
		super.start(broker, config);

		// Process config
		if (url == null || url.isEmpty()) {
			url = "localhost";
		}
		url = parseURLs(config, new String[] { url })[0];
		username = config.get("username", username);
		password = config.get("password", password);
		cleanSession = config.get("cleanSession", cleanSession);
		keepAliveSeconds = config.get("keepAliveSeconds", keepAliveSeconds);
		connectTimeoutSeconds = config.get("connectTimeoutSeconds", connectTimeoutSeconds);
		messageResendIntervalSeconds = config.get("messageResendIntervalSeconds", messageResendIntervalSeconds);
		blockingTimeoutSeconds = config.get("blockingTimeoutSeconds", blockingTimeoutSeconds);
		maxInFlightMessages = config.get("maxInFlightMessages", maxInFlightMessages);
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
			client.connect(nodeID + '-' + broker.components().uid().nextUID(), cleanSession, username, password);

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
		if (client != null) {
			try {
				if (!client.isClosed()) {
					client.disconnect();
					client.close();
				}
			} catch (Throwable cause) {
				logger.warn("Unexpected error occured while closing MQTT client!", cause);
			} finally {
				client = null;
				subscriptions.clear();
			}
			try {
				ThreadGroup group = Thread.currentThread().getThreadGroup();
				Thread[] list = new Thread[group.activeCount()];
				group.enumerate(list);
				for (Thread t : list) {
					if ("CommandCleanup".equals(t.getName())) {
						t.interrupt();
					}
				}
			} catch (Exception ignored) {
			}
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
	public void stop() {
		
		// Stop timers
		super.stop();
		
		// Disconnect
		disconnect();
	}

	// --- SUBSCRIBE ---

	protected final ConcurrentHashMap<String, Promise> subscriptions = new ConcurrentHashMap<>();

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
				if (debug) {
					logger.info("Submitting message to channel \"" + channel + "\":\r\n" + message.toString());
				}
				client.publish(new PublishMessage(channel, qos, serializer.write(message), false));
			} catch (Exception cause) {
				logger.warn("Unable to send message to MQTT server!", cause);
			}
		}
	}

	@Override
	public void published(MqttClient client, PublishMessage message) {
		if (debug) {
			logger.info(
					"Submitted message received by the server at " + new Date(message.getReceivedTimestamp()) + ".");
		}
	}

	// --- RECEIVE ---

	@Override
	public void publishReceived(MqttClient client, PublishMessage message) {
		received(message.getTopic(), message.getPayload());
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