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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.SslContextFactory;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
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
	protected boolean durable;
	protected boolean exclusive;
	protected boolean autoDelete = true;

	protected BasicProperties messageProperties;
	protected Map<String, Object> channelProperties = new HashMap<>();

	// --- AMQP CONNECTION ---

	protected Connection client;

	// --- CHANNEL NAME/CHANNEL MAP ---

	protected final HashMap<String, Channel> channels = new HashMap<>(64);

	// --- CONSTUCTORS ---

	public AmqpTransporter() {
		super();
	}

	public AmqpTransporter(String prefix) {
		super(prefix);
	}

	public AmqpTransporter(String prefix, String url) {
		super(prefix);
		this.url = url;
	}

	public AmqpTransporter(String prefix, String username, String password, SslContextFactory sslContextFactory,
			String url) {
		super(prefix);
		this.username = username;
		this.password = password;
		this.sslContextFactory = sslContextFactory;
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
		mandatory = config.get("mandatory", mandatory);
		immediate = config.get("immediate", immediate);
		durable = config.get("durable", durable);
		exclusive = config.get("exclusive", exclusive);
		autoDelete = config.get("autoDelete", autoDelete);
		Tree props = config.get("channelProperties");
		if (props != null) {
			for (Tree prop : props) {
				channelProperties.put(prop.getName(), prop.asObject());
			}
		}
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
			client = factory.newConnection();

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
		synchronized (channels) {
			for (Channel channel : channels.values()) {
				try {
					channel.close();
				} catch (Throwable ignored) {
				}
			}
			channels.clear();
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
	public void stop() {
		
		// Stop timers
		super.stop();
		
		// Disconnect
		disconnect();
	}

	// --- SUBSCRIBE ---

	@Override
	public Promise subscribe(String channel) {
		if (client != null) {
			try {
				synchronized (channels) {
					if (channels.containsKey(channel)) {
						return Promise.resolve();
					}
					createOrGetChannel(channel).basicConsume(channel, new Consumer() {

						// --- MESSAGE RECEIVED ---

						@Override
						public final void handleDelivery(String consumerTag, Envelope envelope,
								AMQP.BasicProperties properties, byte[] body) throws IOException {
							received(channel, body);
						}

						// --- CONNECTION LOST ---

						@Override
						public final void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
							synchronized (factory) {
								if (client != null) {
									logger.info("AMQP pub-sub connection aborted.");
									reconnect();
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
				}
			} catch (Exception cause) {
				return Promise.reject(cause);
			}
		}
		return Promise.resolve();
	}

	protected Channel createOrGetChannel(String channel) throws Exception {
		Channel c;
		synchronized (channels) {
			c = channels.get(channel);
			if (c != null) {
				return c;
			}
			c = client.createChannel();
			c.queueDeclare(channel, durable, exclusive, autoDelete, channelProperties);
			channels.put(channel, c);
		}
		return c;
	}

	// --- PUBLISH ---

	@Override
	public void publish(String channel, Tree message) {
		if (client != null) {
			if (debug) {
				logger.info("Submitting message to channel \"" + channel + "\":\r\n" + message.toString());
			}
			try {
				createOrGetChannel(channel).basicPublish("", channel, mandatory, immediate, messageProperties,
						serializer.write(message));
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
		this.factory = factory;
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

	public boolean isDurable() {
		return durable;
	}

	public void setDurable(boolean durable) {
		this.durable = durable;
	}

	public boolean isExclusive() {
		return exclusive;
	}

	public void setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
	}

	public boolean isAutoDelete() {
		return autoDelete;
	}

	public void setAutoDelete(boolean autoDelete) {
		this.autoDelete = autoDelete;
	}

	public Map<String, Object> getChannelProperties() {
		return channelProperties;
	}

	public void setChannelProperties(Map<String, Object> arguments) {
		this.channelProperties = arguments;
	}

	public SslContextFactory getSslContextFactory() {
		return sslContextFactory;
	}

	public void setSslContextFactory(SslContextFactory sslContextFactory) {
		this.sslContextFactory = sslContextFactory;
	}

}