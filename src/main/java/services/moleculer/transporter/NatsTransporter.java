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

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.nats.Connection;
import org.nats.MsgHandler;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * Not implemented (yet).
 */
@Name("NATS Transporter")
public final class NatsTransporter extends Transporter {

	// --- PROPERTIES ---

	private String[] urls = new String[] { "localhost" };
	private String username;
	private String password;

	// --- ADVANCED PROPERTIES ---

	private Properties properties = new Properties();

	// --- NATS RECEIVER ---

	private MessageReceiver receiver;

	// --- NATS CONNECTION ---

	private Connection connection;

	// --- CONSTUCTORS ---

	public NatsTransporter() {
		super();
	}

	public NatsTransporter(String prefix) {
		super(prefix);
	}

	public NatsTransporter(String prefix, String... urls) {
		super(prefix);
		this.urls = urls;
	}

	public NatsTransporter(String prefix, String username, String password, String... urls) {
		super(prefix);
		this.username = username;
		this.password = password;
		this.urls = urls;
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
		Tree urlNode = config.get(URL);
		if (urlNode != null) {
			List<String> urlList;
			if (urlNode.isPrimitive()) {
				urlList = new LinkedList<>();
				String url = urlNode.asString().trim();
				if (!url.isEmpty()) {
					urlList.add(url);
				}
			} else {
				urlList = urlNode.asList(String.class);
			}
			if (!urlList.isEmpty()) {
				urls = new String[urlList.size()];
				urlList.toArray(urls);
			}
		}
		username = config.get("username", username);
		password = config.get(PASSWORD, password);

		// Connect to NATS server
		connect();
	}

	// --- CONNECT ---

	private final void connect() {
		try {

			// Create receiver
			if (receiver == null) {
				receiver = new MessageReceiver(this);
			}

			// Set base properties
			properties.put("reconnect", Boolean.FALSE);
			if (urls != null && urls.length > 0) {
				for (int i = 0; i < urls.length; i++) {
					String url = urls[i];
					if (url.indexOf(':') == -1) {
						url = url + ":4222";
					}
					if (!url.startsWith("nats://")) {
						if (username != null && password != null) {
							url = "nats://" + username + ':' + password + '@' + url;
						} else {
							url = "nats://" + url;
						}
					}
					urls[i] = url;
				}
				if (urls.length == 1) {
					properties.put("uri", urls[0]);
					properties.remove("uris");
				} else {
					StringBuilder uris = new StringBuilder(64);
					for (String url : urls) {
						if (uris.length() > 0) {
							uris.append(',');
						}
						uris.append(url);
					}
					properties.put("uris", uris.toString());
					properties.remove("uri");
				}
			}

			// Create connection
			disconnect();
			connection = Connection.connect(properties, new MsgHandler() {
				public final void execute(Object msg) {

					// Connected
					logger.info("NATS pub-sub connection is estabilished.");
					connected();

				}
			}, new MsgHandler() {
				public final void execute(Object msg) {

					// Connection lost
					reconnect();
				}
			});

		} catch (Exception cause) {
			String msg = cause.getMessage();
			if (msg == null || msg.isEmpty()) {
				msg = "Unable to connect to NATS server!";
			} else if (!msg.endsWith("!") && !msg.endsWith(".")) {
				msg += "!";
			}
			logger.warn(msg);
			reconnect();
		}
	}

	// --- DISCONNECT ---

	private final void disconnect() {
		try {
			if (connection != null) {
				connection.close(false);
				logger.info("NATS pub-sub connection aborted.");
			}
		} catch (Throwable cause) {
			logger.warn("Unexpected error occured while closing NATS connection!", cause);
		} finally {
			connection = null;
		}
	}

	// --- RECONNECT ---

	private final void reconnect() {
		disconnect();
		logger.info("Trying to reconnect...");
		scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
	}

	// --- ANY I/O ERROR ---

	@Override
	protected final void error(Throwable cause) {
		logger.warn("Unexpected communication error occured!", cause);
		reconnect();
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
		if (connection != null) {
			try {
				connection.subscribe(channel, receiver);
			} catch (Exception cause) {
				reconnect();
				return Promise.reject(cause);
			}
		}
		return Promise.resolve();
	}

	// --- MESSAGE RECEIVER ---

	private static final class MessageReceiver extends MsgHandler {

		private final NatsTransporter transporter;

		private MessageReceiver(NatsTransporter transporter) {
			this.transporter = transporter;
		}

		public void execute(byte[] msg, String reply, String subject) {
			// System.out.println("RECEIVE " + subject + " " + new String(msg));
			transporter.received(subject, msg);
		}

	}

	// --- PUBLISH ---

	@Override
	public final void publish(String channel, Tree message) {
		if (connection != null) {
			try {
				// System.out.println("SEND " + channel + " " + message.toString(false));
				connection.publish(channel, serializer.write(message));
			} catch (Exception cause) {
				logger.warn("Unable to send message to NATS server!", cause);
				reconnect();
			}
		}
	}

	// --- GETTERS / SETTERS ---

	public final String[] getUrls() {
		return urls;
	}

	public final void setUrls(String[] urls) {
		this.urls = urls;
	}

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

	public final Properties getProperties() {
		return properties;
	}

	public final void setProperties(Properties properties) {
		this.properties = properties;
	}

}