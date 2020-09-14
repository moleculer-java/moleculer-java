/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2020 Andras Berkes [andras.berkes@programmer.net]<br>
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

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.datatree.Promise;
import io.datatree.Tree;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Consumer;
import io.nats.client.ErrorListener;
import io.nats.streaming.ConnectionLostHandler;
import io.nats.streaming.Message;
import io.nats.streaming.MessageHandler;
import io.nats.streaming.Options;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;
import io.nats.streaming.SubscriptionOptions;
import services.moleculer.service.Name;

/**
 * NATS-streaming Transporter. NATS Server is a simple, high performance open
 * source messaging system for cloud native applications, IoT messaging, and
 * microservices architectures (website: https://nats.io). Tested with NATS
 * streaming server version 2.1.4. Usage:
 * 
 * <pre>
 * ServiceBroker broker = ServiceBroker.builder().nodeID("node1").transporter(new NatsStreamingTransporter("localhost"))
 * 		.build();
 * </pre>
 * 
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/io.nats/java-nats-streaming<br>
 * compile group: 'io.nats', name: 'java-nats-streaming', version: '2.2.3'
 *
 * @see TcpTransporter
 * @see RedisTransporter
 * @see NatsTransporter
 * @see MqttTransporter
 * @see JmsTransporter
 * @see KafkaTransporter
 * @see AmqpTransporter
 */
@Name("NATS-Streaming Transporter")
public class NatsStreamingTransporter extends Transporter
		implements ConnectionListener, ErrorListener, ConnectionLostHandler, MessageHandler {

	// --- SERVER URL ---

	/**
	 * Nats server url.
	 */
	protected String url = "localhost";

	// --- OTHER NATS-STREAMING PROPERTIES ---

	protected Duration connectTimeout;
	protected Duration ackTimeout;
	protected String discoverPrefix;
	protected Integer maxPubAcksInFlight;
	protected String clientId;
	protected String clusterId;
	private Duration pingInterval;
	private Integer pingsMaxOut;
	private Boolean traceConnection;
	private SubscriptionOptions subscriptionOptions;

	// --- NATS CLIENT ---

	/**
	 * Nats connection.
	 */
	protected StreamingConnection client;

	// --- STARTED FLAG ---

	protected final AtomicBoolean started = new AtomicBoolean();

	// --- CONSTUCTORS ---

	public NatsStreamingTransporter() {
	}

	public NatsStreamingTransporter(String url) {
		this.url = url;
	}

	// --- CONNECT ---

	@Override
	public void connect() {
		try {

			// Create NATS client options
			Options.Builder builder = new Options.Builder();

			if (clientId != null && !clientId.isEmpty()) {
				builder.clientId(clientId);
			} else {
				builder.clientId(nodeID);
			}
			if (clusterId != null && !clusterId.isEmpty()) {
				builder.clusterId(clusterId);
			} else {
				builder.clusterId("test-cluster");
			}
			if (connectTimeout != null) {
				builder.connectWait(connectTimeout);
			}
			if (ackTimeout != null) {
				builder.pubAckWait(ackTimeout);
			}
			if (discoverPrefix != null) {
				builder.discoverPrefix(discoverPrefix);
			}
			if (maxPubAcksInFlight != null) {
				builder.maxPubAcksInFlight(maxPubAcksInFlight);
			}
			if (pingInterval != null) {
				builder.pingInterval(pingInterval);
			}
			if (pingsMaxOut != null) {
				builder.maxPingsOut(pingsMaxOut);
			}
			if (traceConnection != null) {
				builder.traceConnection();
			}

			// Set server URLs
			String natsUrl = url;
			if (natsUrl.indexOf(':') == -1) {
				natsUrl = natsUrl + ":4222";
			}
			if (natsUrl.indexOf("://") == -1) {
				natsUrl = "nats://" + natsUrl;
			}
			builder.natsUrl(natsUrl);

			// Set event listeners
			builder.connectionListener(this);
			builder.errorListener(this);
			builder.connectionLostHandler(this);

			// Connect to NATS server
			disconnect();
			started.set(true);

			StreamingConnectionFactory factory = new StreamingConnectionFactory(builder.build());
			client = factory.createConnection();

			logger.info("NATS pub-sub connection estabilished.");
			connected();
		} catch (Exception cause) {
			String msg = cause.getMessage();
			if (msg == null || msg.isEmpty()) {
				msg = "Unable to connect to NATS server!";
			} else if (!msg.endsWith("!") && !msg.endsWith(".")) {
				msg += "!";
			}
			logger.warn(msg, cause);
		}
	}

	// --- DISCONNECT ---

	protected void disconnect() {
		boolean notify = false;
		if (client != null) {
			notify = true;
			try {
				client.close();
			} catch (Throwable cause) {
				logger.warn("Unexpected error occurred while closing NATS client!", cause);
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

	protected void reconnect() {
		disconnect();
		logger.info("Trying to reconnect...");
		scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
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

		// Mark as stopped
		started.set(false);

		// Stop timers
		super.stopped();

		// Disconnect
		try {
			Thread.sleep(100);
		} catch (InterruptedException interrupt) {
		}
		disconnect();
	}

	// --- SUBSCRIBE ---

	@Override
	public Promise subscribe(String channel) {
		if (client != null) {
			try {
				if (subscriptionOptions == null) {
					client.subscribe(channel, this);
				} else {
					client.subscribe(channel, this, subscriptionOptions);
				}
			} catch (Exception cause) {
				return Promise.reject(cause);
			}
		}
		return Promise.resolve();
	}

	// --- MESSAGE RECEIVED ---

	@Override
	public void onMessage(Message msg) {

		// Send messages into the shared executor
		received(msg.getSubject(), msg.getData());
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
				client.publish(channel, bytes);
			} catch (Exception cause) {
				logger.warn("Unable to send message to NATS server!", cause);
				reconnect();
			}
		}
	}

	// --- EVENT LISTENERS ---

	@Override
	public void connectionEvent(Connection conn, Events type) {
		switch (type) {
		case CONNECTED:

			// The connection is permanently closed, either by manual action or
			// failed reconnects
			if (debug) {
				logger.info("NATS connection opened.");
			}
			break;

		case CLOSED:

			// The connection lost its connection, but may try to reconnect if
			// configured to
			logger.info("NATS connection closed.");
			if (started.get()) {
				reconnect();
			}
			break;

		case DISCONNECTED:

			// The connection was connected, lost its connection and
			// successfully reconnected
			if (debug) {
				logger.info("NATS pub-sub connection disconnected.");
			}
			break;

		case RECONNECTED:

			// The connection was reconnected and the server has been notified
			// of all subscriptions
			if (debug) {
				logger.info("NATS connection reconnected.");
			}
			break;

		case RESUBSCRIBED:

			// The connection was told about new servers from, from the current
			// server
			if (debug) {
				logger.info("NATS subscriptions re-established.");
			}
			break;

		case DISCOVERED_SERVERS:

			// Servers discovered
			if (debug) {
				logger.info("NATS servers discovered.");
			}
			break;

		default:
			break;
		}
	}

	@Override
	public void errorOccurred(Connection conn, String error) {
		logger.error("NATS error occured: " + error);
	}

	@Override
	public void exceptionOccurred(Connection conn, Exception exp) {
		logger.error("NATS exception occured!", exp);
	}

	@Override
	public void slowConsumerDetected(Connection conn, Consumer consumer) {
		if (debug) {
			logger.warn("Slow NATS consumer detected: " + consumer);
		}
	}

	@Override
	public void connectionLost(StreamingConnection conn, Exception ex) {
		if (debug) {
			logger.warn("NATS connection lost!", ex);
		}
	}

	// --- GETTERS / SETTERS ---

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url
	 *            the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the connectTimeout
	 */
	public Duration getConnectTimeout() {
		return connectTimeout;
	}

	/**
	 * @param connectTimeout
	 *            the connectTimeout to set
	 */
	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * @return the ackTimeout
	 */
	public Duration getAckTimeout() {
		return ackTimeout;
	}

	/**
	 * @param ackTimeout
	 *            the ackTimeout to set
	 */
	public void setAckTimeout(Duration ackTimeout) {
		this.ackTimeout = ackTimeout;
	}

	/**
	 * @return the discoverPrefix
	 */
	public String getDiscoverPrefix() {
		return discoverPrefix;
	}

	/**
	 * @param discoverPrefix
	 *            the discoverPrefix to set
	 */
	public void setDiscoverPrefix(String discoverPrefix) {
		this.discoverPrefix = discoverPrefix;
	}

	/**
	 * @return the maxPubAcksInFlight
	 */
	public Integer getMaxPubAcksInFlight() {
		return maxPubAcksInFlight;
	}

	/**
	 * @param maxPubAcksInFlight
	 *            the maxPubAcksInFlight to set
	 */
	public void setMaxPubAcksInFlight(Integer maxPubAcksInFlight) {
		this.maxPubAcksInFlight = maxPubAcksInFlight;
	}

	/**
	 * @return the clientId
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * @param clientId
	 *            the clientId to set
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * @return the clusterId
	 */
	public String getClusterId() {
		return clusterId;
	}

	/**
	 * @param clusterId
	 *            the clusterId to set
	 */
	public void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}

	/**
	 * @return the pingInterval
	 */
	public Duration getPingInterval() {
		return pingInterval;
	}

	/**
	 * @param pingInterval
	 *            the pingInterval to set
	 */
	public void setPingInterval(Duration pingInterval) {
		this.pingInterval = pingInterval;
	}

	/**
	 * @return the pingsMaxOut
	 */
	public Integer getPingsMaxOut() {
		return pingsMaxOut;
	}

	/**
	 * @param pingsMaxOut
	 *            the pingsMaxOut to set
	 */
	public void setPingsMaxOut(Integer pingsMaxOut) {
		this.pingsMaxOut = pingsMaxOut;
	}

	/**
	 * @return the traceConnection
	 */
	public Boolean getTraceConnection() {
		return traceConnection;
	}

	/**
	 * @param traceConnection
	 *            the traceConnection to set
	 */
	public void setTraceConnection(Boolean traceConnection) {
		this.traceConnection = traceConnection;
	}

	/**
	 * @return the subscriptionOptions
	 */
	public SubscriptionOptions getSubscriptionOptions() {
		return subscriptionOptions;
	}

	/**
	 * @param subscriptionOptions
	 *            the subscriptionOptions to set
	 */
	public void setSubscriptionOptions(SubscriptionOptions subscriptionOptions) {
		this.subscriptionOptions = subscriptionOptions;
	}

}