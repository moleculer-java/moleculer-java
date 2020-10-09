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

import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.service.Name;

/**
 * MQTT Transporter (eg. for Mosquitto MQTT Server or ActiveMQ Server). MQTT is
 * a machine-to-machine (M2M)/"Internet of Things" connectivity protocol. It was
 * designed as an extremely lightweight publish/subscribe messaging transport
 * (website: http://mqtt.org). Usage:
 * 
 * <pre>
 * ServiceBroker broker = ServiceBroker.builder().nodeID("node1").transporter(new MqttTransporter("localhost"))
 * 		.build();
 * </pre>
 * 
 * <b>Required dependency:</b><br>
 * <br>
 * //
 * https://mvnrepository.com/artifact/org.eclipse.paho/org.eclipse.paho.client.
 * mqttv3<br>
 * compile group: 'org.eclipse.paho', name: 'org.eclipse.paho.client.mqttv3',
 * version: '1.2.5'
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
public class MqttTransporter extends Transporter implements MqttCallback {

	// --- PROPERTIES ---

	protected String username;
	protected String password;
	protected String[] urls = { "localhost" };
	protected int qos = 0;
	protected boolean cleanSession = MqttConnectOptions.CLEAN_SESSION_DEFAULT;
	protected MemoryPersistence persistence = new MemoryPersistence();
	protected int connectionTimeout = MqttConnectOptions.CONNECTION_TIMEOUT_DEFAULT;
	protected int executorServiceTimeout = 1;
	protected boolean httpsHostnameVerificationEnabled = true;
	protected int keepAliveInterval = MqttConnectOptions.KEEP_ALIVE_INTERVAL_DEFAULT;
	protected int maxInflight = MqttConnectOptions.MAX_INFLIGHT_DEFAULT;
	protected int mqttVersion = MqttConnectOptions.MQTT_VERSION_DEFAULT;
	protected SocketFactory socketFactory;
	protected HostnameVerifier hostnameVerifier;
	protected Properties sslClientProps;
	protected Properties customWebSocketHeaders;

	// --- MQTT CONNECTION ---

	protected MqttAsyncClient client;

	// --- CONSTUCTORS ---

	public MqttTransporter() {
	}

	public MqttTransporter(String... urls) {
		this.urls = Objects.requireNonNull(urls);
	}

	public MqttTransporter(String username, String password, String... urls) {
		this.username = username;
		this.password = password;
		this.urls = Objects.requireNonNull(urls);
	}

	// --- CONNECT ---

	@Override
	public void connect() {
		try {

			// Format URLs
			MqttConnectOptions opts = new MqttConnectOptions();
			for (int i = 0; i < urls.length; i++) {
				String uri = urls[i];
				if (uri.indexOf(':') == -1) {
					uri = uri + ":1883";
				}
				uri = uri.replace("mqtt://", "tcp://");
				if (uri.indexOf("://") == -1) {
					uri = "tcp://" + uri;
				}
				urls[i] = uri;
			}

			// Set properties
			opts.setCleanSession(cleanSession);
			if (username != null) {
				opts.setUserName(username);
			}
			if (password != null) {
				opts.setPassword(password.toCharArray());
			}
			opts.setAutomaticReconnect(false);
			opts.setConnectionTimeout(connectionTimeout);
			opts.setExecutorServiceTimeout(executorServiceTimeout);
			opts.setHttpsHostnameVerificationEnabled(httpsHostnameVerificationEnabled);
			opts.setKeepAliveInterval(keepAliveInterval);
			opts.setMaxInflight(maxInflight);
			opts.setMqttVersion(mqttVersion);
			opts.setServerURIs(urls);
			opts.setSocketFactory(socketFactory);
			opts.setSSLHostnameVerifier(hostnameVerifier);
			opts.setSSLProperties(sslClientProps);
			opts.setCustomWebSocketHeaders(customWebSocketHeaders);

			// Create MQTT client
			disconnect();
			String url = urls.length == 0 ? "tcp://localhost:1883" : urls[0];
			client = new MqttAsyncClient(url, nodeID, persistence);
			client.setCallback(this);
			MqttTransporter self = this;
			client.connect(opts, null, new IMqttActionListener() {

				@Override
				public final void onSuccess(IMqttToken asyncActionToken) {
					logger.info("MQTT pub-sub connection estabilished.");
					scheduler.schedule(self::connected, 100, TimeUnit.MILLISECONDS);
				}

				@Override
				public final void onFailure(IMqttToken asyncActionToken, Throwable cause) {
					reconnect(cause);
				}

			});

		} catch (Exception cause) {
			reconnect(cause);
		}
	}

	// --- DISCONNECT ---

	protected void disconnect() {
		boolean notify = false;
		if (client != null) {
			notify = true;
			try {
				if (client.isConnected()) {
					client.close();
				}
			} catch (Throwable cause) {
				logger.warn("Unexpected error occurred while closing MQTT client!", cause);
			} finally {
				client = null;
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
				msg = "Unable to connect to MQTT server (" + String.valueOf(cause) + ")!";
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
	public void connectionLost(Throwable cause) {
		reconnect(cause);
	}

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
		return new Promise(res -> {
			if (client != null && client.isConnected()) {
				client.subscribe(channel, qos).setActionCallback(new IMqttActionListener() {

					@Override
					public final void onSuccess(IMqttToken asyncActionToken) {
						res.resolve();
					}

					@Override
					public final void onFailure(IMqttToken asyncActionToken, Throwable cause) {
						res.reject(cause);
					}

				});
			} else {
				res.reject(new Throwable("Not connected!"));
			}
		});
	}

	// --- PUBLISH ---

	@Override
	public void publish(String channel, Tree message) {
		if (client != null && client.isConnected()) {
			try {
				if (debug && (debugHeartbeats || !channel.endsWith(heartbeatChannel))) {
					logger.info("Submitting message to channel \"" + channel + "\":\r\n" + message.toString());
				}

				// Metrics
				byte[] bytes = serializer.write(message);
				if (metrics != null) {
					counterTransporterPacketsSentTotal.increment();
					counterTransporterPacketsSentBytes.increment(bytes.length);
				}

				// Send
				client.publish(channel, bytes, qos, false);
			} catch (Exception cause) {
				logger.warn("Unable to send message to MQTT server!", cause);
			}
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		if (debug || debugHeartbeats) {
			String[] channels = token.getTopics();
			if (channels == null || channels.length == 0) {
				return;
			}
			String channel = channels[0];
			if (heartbeatChannel.equals(channel) && !debugHeartbeats) {
				return;
			}
			logger.info("Server received message from channel \"" + channel + "\".");
		}
	}

	// --- RECEIVE ---

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {

		// We are running in the shared executor's pool,
		// do not create new task.
		processReceivedMessage(topic, message.getPayload());
	}

	// --- GETTERS / SETTERS ---

	public String[] getUrls() {
		return urls;
	}

	public void setUrls(String... urls) {
		this.urls = Objects.requireNonNull(urls);
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

	public int getQos() {
		return qos;
	}

	public void setQos(int qos) {
		this.qos = qos;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}

	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public MemoryPersistence getPersistence() {
		return persistence;
	}

	public void setPersistence(MemoryPersistence persistence) {
		this.persistence = persistence;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public int getExecutorServiceTimeout() {
		return executorServiceTimeout;
	}

	public void setExecutorServiceTimeout(int executorServiceTimeout) {
		this.executorServiceTimeout = executorServiceTimeout;
	}

	public boolean isHttpsHostnameVerificationEnabled() {
		return httpsHostnameVerificationEnabled;
	}

	public void setHttpsHostnameVerificationEnabled(boolean httpsHostnameVerificationEnabled) {
		this.httpsHostnameVerificationEnabled = httpsHostnameVerificationEnabled;
	}

	public int getKeepAliveInterval() {
		return keepAliveInterval;
	}

	public void setKeepAliveInterval(int keepAliveInterval) {
		this.keepAliveInterval = keepAliveInterval;
	}

	public int getMaxInflight() {
		return maxInflight;
	}

	public void setMaxInflight(int maxInflight) {
		this.maxInflight = maxInflight;
	}

	public int getMqttVersion() {
		return mqttVersion;
	}

	public void setMqttVersion(int mqttVersion) {
		this.mqttVersion = mqttVersion;
	}

	public SocketFactory getSocketFactory() {
		return socketFactory;
	}

	public void setSocketFactory(SocketFactory socketFactory) {
		this.socketFactory = socketFactory;
	}

	public HostnameVerifier getHostnameVerifier() {
		return hostnameVerifier;
	}

	public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
		this.hostnameVerifier = hostnameVerifier;
	}

	public Properties getSslClientProps() {
		return sslClientProps;
	}

	public void setSslClientProps(Properties sslClientProps) {
		this.sslClientProps = sslClientProps;
	}

	public Properties getCustomWebSocketHeaders() {
		return customWebSocketHeaders;
	}

	public void setCustomWebSocketHeaders(Properties customWebSocketHeaders) {
		this.customWebSocketHeaders = customWebSocketHeaders;
	}

}