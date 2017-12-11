/**
 * This software is licensed under MIT license.<br>
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

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.jms.BytesMessage;
import javax.jms.DeliveryMode;
import javax.jms.JMSContext;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * JMS Transporter. The Java Message Service API is a Java Message Oriented
 * Middleware API for sending messages between two or more clients. It is an
 * implementation to handle the Producer-consumer problem. JMS is a part of the
 * Java Enterprise Edition. Sample of usage with Active MQ:<br>
 * <br>
 * Transporter t = new JmsTransporter(new ActiveMQConnectionFactory());<br>
 * ServiceBroker broker = ServiceBroker.builder().transporter(t).build();<br>
 * <br>
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/javax.jms/javax.jms-api<br>
 * compile group: 'javax.jms', name: 'javax.jms-api', version: '2.0.1'<br>
 * <br>
 * + dependencies of the JMS driver.
 * 
 * @see RedisTransporter
 * @see NatsTransporter
 * @see MqttTransporter
 * @see AmqpTransporter
 * @see SocketClusterTransporter
 * @see GoogleCloudTransporter
 */
@Name("JMS Transporter")
public final class JmsTransporter extends Transporter {

	// --- PROPERTIES ---

	private String username;
	private String password;

	// --- OTHER JMS PROPERTIES ---

	private boolean transacted;
	private int acknowledgeMode = JMSContext.AUTO_ACKNOWLEDGE;
	private int deliveryMode = DeliveryMode.NON_PERSISTENT;
	private int priority = 5;
	private int ttl = 1;

	// --- CONNECTION FACTORY NAME IN JNDI ---

	private String connectionFactoryJndiName = "moleculerConnectionFactory";

	// --- JMS CONNECTION FACTORY ---

	private TopicConnectionFactory factory;

	// --- JMS CONNECTION ---

	private TopicConnection client;

	// --- JMS SESSION ---

	private TopicSession session;

	// --- CHANNEL NAME/PUBLISHER MAP ---

	private final HashMap<String, TopicPublisher> publishers = new HashMap<>(64);

	// --- CHANNEL NAME/SUBSCRIBER MAP ---

	private final HashMap<String, TopicSubscriber> subscribers = new HashMap<>(64);

	// --- CONSTUCTORS ---

	public JmsTransporter() {
		super();
	}

	public JmsTransporter(TopicConnectionFactory connectionFactory) {
		super();
		this.factory = connectionFactory;
	}

	public JmsTransporter(String prefix, String connectionFactoryJndiName) {
		super(prefix);
		this.connectionFactoryJndiName = connectionFactoryJndiName;
	}

	public JmsTransporter(String prefix, TopicConnectionFactory connectionFactory) {
		super(prefix);
		this.factory = connectionFactory;
	}

	public JmsTransporter(String prefix, String username, String password, String connectionFactoryJndiName) {
		super(prefix);
		this.username = username;
		this.password = password;
		this.connectionFactoryJndiName = connectionFactoryJndiName;
	}

	public JmsTransporter(String prefix, String username, String password, TopicConnectionFactory connectionFactory) {
		super(prefix);
		this.username = username;
		this.password = password;
		this.factory = connectionFactory;
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
		username = config.get("username", username);
		password = config.get("password", password);
		transacted = config.get("transacted", transacted);
		acknowledgeMode = config.get("acknowledgeMode", acknowledgeMode);
		deliveryMode = config.get("deliveryMode", deliveryMode);
		priority = config.get("priority", priority);
		ttl = config.get("ttl", ttl);
		connectionFactoryJndiName = config.get("connectionFactoryJndiName", connectionFactoryJndiName);

		// Connect to JMS server
		connect();
	}

	// --- CONNECT ---

	private final void connect() {
		try {

			// Create JMS client and session
			disconnect();
			if (factory == null) {
				try {
					Context ctx = new InitialContext();
					factory = (TopicConnectionFactory) ctx.lookup(connectionFactoryJndiName);
				} catch (Exception cause) {
					logger.error("TopicConnectionFactory is undefined and \"" + connectionFactoryJndiName
							+ "\" JNDI value isn't available!");
					reconnect(cause);
					return;
				}
			}
			if ((username == null || username.isEmpty()) && (password == null || password.isEmpty())) {
				client = factory.createTopicConnection();
			} else {
				client = factory.createTopicConnection(username, password);
			}
			client.setClientID(nodeID);
			client.start();
			session = client.createTopicSession(transacted, acknowledgeMode);
			connected();
		} catch (Exception cause) {
			reconnect(cause);
		}
	}

	// --- DISCONNECT ---

	private final void disconnect() {
		if (client != null) {
			try {
				client.stop();
			} catch (Exception ignored) {
			}
		}
		synchronized (publishers) {
			for (TopicPublisher publisher : publishers.values()) {
				try {
					publisher.close();
				} catch (Exception ignored) {
				}
			}
			publishers.clear();
		}
		synchronized (subscribers) {
			for (TopicSubscriber subscriber : subscribers.values()) {
				try {
					subscriber.close();
				} catch (Exception ignored) {
				}
			}
			subscribers.clear();
		}
		if (session != null) {
			try {
				session.close();
			} catch (Exception ignored) {
			}
			session = null;
		}
		if (client != null) {
			try {
				client.close();
			} catch (Exception ignored) {
			}
			client = null;
		}
	}

	// --- RECONNECT ---

	private final void reconnect(Throwable cause) {
		if (cause != null) {
			String msg = cause.getMessage();
			if (msg == null || msg.isEmpty()) {
				msg = "Unable to connect to JMS server!";
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
	protected final void error(Throwable cause) {
		reconnect(cause);
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
		try {

			// Create publisher
			createOrGetPublisher(channel);

			// Create subscriber
			synchronized (subscribers) {
				if (!subscribers.containsKey(channel)) {
					TopicSubscriber subscriber = session.createSubscriber(session.createTopic(channel));
					subscribers.put(channel, subscriber);
					subscriber.setMessageListener((message) -> {
						try {
							BytesMessage msg = (BytesMessage) message;
							byte[] bytes = new byte[(int) msg.getBodyLength()];
							msg.readBytes(bytes);
							received(channel, bytes);
						} catch (Exception cause) {
							logger.error("Unable to deserialize message!", cause);
						}
					});
				}
			}
		} catch (Exception cause) {
			return Promise.reject(cause);
		}
		return Promise.resolve();
	}

	private final TopicPublisher createOrGetPublisher(String channel) throws Exception {
		TopicPublisher publisher;
		synchronized (publishers) {
			publisher = publishers.get(channel);
			if (publisher != null) {
				return publisher;
			}
			Topic topic = session.createTopic(channel);
			publisher = session.createPublisher(topic);
			publisher.setDeliveryMode(deliveryMode);
			publishers.put(channel, publisher);
		}
		return publisher;
	}

	// --- PUBLISH ---

	@Override
	public final void publish(String channel, Tree message) {
		if (client != null) {
			try {
				if (debug) {
					logger.info("Submitting message to channel \"" + channel + "\":\r\n" + message.toString());
				}
				TopicPublisher publisher = createOrGetPublisher(channel);
				BytesMessage msg = session.createBytesMessage();
				msg.writeBytes(serializer.write(message));
				if (transacted) {
					synchronized (this) {
						try {
							publisher.send(msg, deliveryMode, priority, ttl);
							session.commit();
						} catch (Exception cause) {
							try {
								session.rollback();
							} catch (Exception ignored) {
							}
							throw cause;
						}
					}
				} else {
					publisher.send(msg, deliveryMode, priority, ttl);
				}
			} catch (Exception cause) {
				logger.warn("Unable to send message to JMS server!", cause);
			}
		}
	}

	// --- GETTERS / SETTERS ---

	public final String getUsername() {
		return username;
	}

	public final void setUsername(String username) {
		this.username = username;
	}

	public final String getPassword() {
		return password;
	}

	public final void setPassword(String password) {
		this.password = password;
	}

	public final int getAcknowledgeMode() {
		return acknowledgeMode;
	}

	public final void setAcknowledgeMode(int acknowledgeMode) {
		this.acknowledgeMode = acknowledgeMode;
	}

	public final int getDeliveryMode() {
		return deliveryMode;
	}

	public final void setDeliveryMode(int deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	public final TopicConnectionFactory getTopicConnectionFactory() {
		return factory;
	}

	public final void setTopicConnectionFactory(TopicConnectionFactory factory) {
		this.factory = factory;
	}

	public final boolean isTransacted() {
		return transacted;
	}

	public final void setTransacted(boolean transacted) {
		this.transacted = transacted;
	}

	public final int getPriority() {
		return priority;
	}

	public final void setPriority(int priority) {
		this.priority = priority;
	}

	public final int getTtl() {
		return ttl;
	}

	public final void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public final String getConnectionFactoryJndiName() {
		return connectionFactoryJndiName;
	}

	public final void setConnectionFactoryJndiName(String connectionFactoryJndiName) {
		this.connectionFactoryJndiName = connectionFactoryJndiName;
	}

}
