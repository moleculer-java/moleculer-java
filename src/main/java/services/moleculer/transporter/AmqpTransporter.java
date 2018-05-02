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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.SslContextFactory;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.service.Name;

/**
 * AMQP Transporter based on RabbitMQ's AMQP client API. AMQP provides a
 * platform-agnostic method for ensuring information is safely transported
 * between applications, among organizations, within mobile infrastructures, and
 * across the Cloud.<br>
 * <br>
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/com.rabbitmq/amqp-client<br>
 * compile group: 'com.rabbitmq', name: 'amqp-client', version: '5.0.0'
 *
 * @see RedisTransporter
 * @see NatsTransporter
 * @see MqttTransporter
 * @see JmsTransporter
 * @see GoogleTransporter
 */
@Name("AMQP Transporter")
public class AmqpTransporter extends Transporter {

	// --- PROPERTIES ---

	protected String username;
	protected String password;
	protected String url = "localhost";
	protected SslContextFactory sslContextFactory;

	// --- OTHER AMQP PROPERTIES ---

	protected ConnectionFactory factory = new ConnectionFactory();
	protected boolean mandatory;
	protected boolean immediate;
	protected boolean exclusive;
	protected boolean internal;
	protected String exchangeType = "fanout";

	protected BasicProperties messageProperties;
	protected Map<String, Object> queueProperties = new HashMap<>();
	protected Map<String, Object> exchangeProperties = new HashMap<>();

	// --- AMQP CONNECTION ---

	protected Connection client;
	protected Channel channel;

	// --- STARTED FLAG ---

	protected final AtomicBoolean started = new AtomicBoolean();

	// --- CONSTUCTORS ---

	public AmqpTransporter() {
		super();
	}

	public AmqpTransporter(String url) {
		this.url = url;
	}

	public AmqpTransporter(String username, String password, SslContextFactory sslContextFactory, String url) {
		this.username = username;
		this.password = password;
		this.sslContextFactory = sslContextFactory;
		this.url = url;
	}

	// --- CONNECT ---

	@Override
	public void connect() {
		try {

			// Create client connection
			disconnect();
			String uri = url;
			if (uri.indexOf(':') == -1) {
				uri = uri + ":5672";
			}
			if (url.indexOf("://") == -1) {
				if (sslContextFactory != null) {
					uri = "amqps://" + uri;
				} else {
					uri = "amqp://" + uri;
				}
			}
			factory.setHeartbeatExecutor(scheduler);
			factory.setSharedExecutor(executor);
			factory.setSslContextFactory(sslContextFactory);

			factory.setAutomaticRecoveryEnabled(false);
			factory.setTopologyRecoveryEnabled(false);

			// NioParams params = new NioParams();
			// params.setNioExecutor(executor);
			// factory.setNioParams(params);

			factory.setUri(uri);
			if (username != null) {
				factory.setUsername(username);
			}
			if (password != null) {
				factory.setPassword(password);
			}
			started.set(true);
			client = factory.newConnection();
			channel = client.createChannel();

			logger.info("AMQP pub-sub connection estabilished.");
			connected();
		} catch (Exception cause) {
			String msg = cause.getMessage();
			if (msg == null || msg.isEmpty()) {
				msg = "Unable to connect to AMQP server!";
			} else if (!msg.endsWith("!") && !msg.endsWith(".")) {
				msg += "!";
			}
			logger.warn(msg);
			reconnect();
		}
	}

	// --- DISCONNECT ---

	protected void disconnect() {
		if (channel != null) {
			try {
				channel.close();
			} catch (Throwable cause) {
				logger.warn("Unexpected error occured while closing AMQP channel!", cause);
			} finally {
				channel = null;
			}
		}
		if (client != null) {
			try {
				client.close();
			} catch (Throwable cause) {
				logger.warn("Unexpected error occured while closing AMQP client!", cause);
			} finally {
				client = null;
			}
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
		logger.warn("Unexpected communication error occured!", cause);
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
		disconnect();
	}

	// --- SUBSCRIBE ---

	@Override
	public Promise subscribe(String channel) {
		if (client != null) {
			try {
				String postfix = '.' + nodeID;
				String queueName;
				if (channel.endsWith(postfix)) {

					// Create queue
					queueName = channel;
					this.channel.queueDeclareNoWait(queueName, true, exclusive, true, queueProperties);

				} else {

					// Create queue and exchange
					queueName = channel + '.' + nodeID;
					this.channel.queueDeclareNoWait(queueName, true, exclusive, true, queueProperties);
					this.channel.exchangeDeclareNoWait(channel, exchangeType, true, false, internal,
							exchangeProperties);
					this.channel.queueBind(queueName, channel, "");
				}
				this.channel.basicConsume(queueName, true, new Consumer() {

					// --- MESSAGE RECEIVED ---

					@Override
					public final void handleDelivery(String consumerTag, Envelope envelope,
							AMQP.BasicProperties properties, byte[] body) throws IOException {
						if (debug) {
							logger.info(nodeID + " received message from queue \"" + queueName + "\".");
						}
						received(channel, body);
					}

					// --- CONNECTION LOST ---

					@Override
					public final void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
						synchronized (factory) {
							if (client != null) {
								logger.info("AMQP pub-sub connection aborted.");
								if (started.get()) {
									reconnect();
								}
							}
						}
					}

					// --- UNUSED METHODS ---

					@Override
					public final void handleConsumeOk(String consumerTag) {
						if (debug) {
							logger.info("ConsumeOk packet received (consumerTag: " + consumerTag + ").");
						}
					}

					@Override
					public final void handleCancelOk(String consumerTag) {
						if (debug) {
							logger.info("CancelOk packet received (consumerTag: " + consumerTag + ").");
						}
					}

					@Override
					public final void handleCancel(String consumerTag) throws IOException {
						if (debug) {
							logger.info("Cancel packet received (consumerTag: " + consumerTag + ").");
						}
					}

					@Override
					public final void handleRecoverOk(String consumerTag) {
						if (debug) {
							logger.info("RecoverOk packet received (consumerTag: " + consumerTag + ").");
						}
					}

				});

			} catch (Exception cause) {
				return Promise.reject(cause);
			}
		}
		return Promise.resolve();
	}

	// --- PUBLISH ---

	@Override
	public void publish(String channel, Tree message) {
		if (client != null) {
			try {
				int pos = channel.indexOf('.');
				if (channel.indexOf('.', pos + 1) > -1) {

					// Send to queue directly
					if (debug) {
						logger.info("Submitting message to queue \"" + channel + "\":\r\n" + message.toString());
					}
					this.channel.basicPublish("", channel, mandatory, immediate, messageProperties,
							serializer.write(message));

				} else {

					// Send to exchange
					if (debug) {
						logger.info("Submitting message to exchange \"" + channel + "\":\r\n" + message.toString());
					}
					this.channel.basicPublish(channel, "", mandatory, immediate, messageProperties,
							serializer.write(message));

				}
			} catch (Exception cause) {
				logger.warn("Unable to send message to AMQP server!", cause);
			}
		}
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

	public ConnectionFactory getFactory() {
		return factory;
	}

	public void setFactory(ConnectionFactory factory) {
		if (factory != null) {
			this.factory = factory;
		}
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public boolean isImmediate() {
		return immediate;
	}

	public void setImmediate(boolean immediate) {
		this.immediate = immediate;
	}

	public BasicProperties getMessageProperties() {
		return messageProperties;
	}

	public void setMessageProperties(BasicProperties messageProperties) {
		this.messageProperties = messageProperties;
	}

	public SslContextFactory getSslContextFactory() {
		return sslContextFactory;
	}

	public void setSslContextFactory(SslContextFactory sslContextFactory) {
		this.sslContextFactory = sslContextFactory;
	}

	public String getExchangeType() {
		return exchangeType;
	}

	public void setExchangeType(String exchangeType) {
		this.exchangeType = exchangeType;
	}

	public boolean isExclusive() {
		return exclusive;
	}

	public void setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
	}

	public Map<String, Object> getQueueProperties() {
		return queueProperties;
	}

	public void setQueueProperties(Map<String, Object> queueProperties) {
		this.queueProperties = queueProperties;
	}

	public Map<String, Object> getExchangeProperties() {
		return exchangeProperties;
	}

	public void setExchangeProperties(Map<String, Object> exchangeProperties) {
		this.exchangeProperties = exchangeProperties;
	}

	public boolean isInternal() {
		return internal;
	}

	public void setInternal(boolean internal) {
		this.internal = internal;
	}

}