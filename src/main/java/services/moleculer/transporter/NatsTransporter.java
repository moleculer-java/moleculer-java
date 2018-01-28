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

import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import io.datatree.Tree;
import io.nats.client.Connection;
import io.nats.client.ConnectionEvent;
import io.nats.client.DisconnectedCallback;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import io.nats.client.Nats;
import io.nats.client.Options;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * NATS Transporter. NATS Server is a simple, high performance open source
 * messaging system for cloud native applications, IoT messaging, and
 * microservices architectures (website: https://nats.io).<br>
 * <br>
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/io.nats/jnats<br>
 * compile group: 'io.nats', name: 'jnats', version: '1.0'
 * 
 * @see RedisTransporter
 * @see MqttTransporter
 * @see AmqpTransporter
 * @see JmsTransporter
 * @see GoogleTransporter
 */
@Name("NATS Transporter")
public class NatsTransporter extends Transporter implements MessageHandler, DisconnectedCallback {

	// --- PROPERTIES ---

	protected String username;
	protected String password;
	protected boolean secure;
	protected String[] urls = { "localhost" };

	// --- OTHER NATS PROPERTIES ---

	protected SSLContext sslContext;
	protected boolean dontRandomize;
	protected int maxPingsOut = Nats.DEFAULT_MAX_PINGS_OUT;
	protected long pingInterval = Nats.DEFAULT_PING_INTERVAL;
	protected int timeout = Nats.DEFAULT_TIMEOUT;
	protected boolean tlsDebug;
	protected boolean verbose;

	// --- NATS CONNECTION ---

	protected Connection client;

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

	public NatsTransporter(String prefix, String username, String password, boolean secure, String... urls) {
		super(prefix);
		this.username = username;
		this.password = password;
		this.secure = secure;
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
	public void start(ServiceBroker broker, Tree config) throws Exception {

		// Process basic properties (eg. "prefix")
		super.start(broker, config);

		// Process config
		urls = parseURLs(config, urls);
		username = config.get("username", username);
		password = config.get("password", password);
		secure = config.get("secure", secure);
		dontRandomize = config.get("dontRandomize", dontRandomize);
		maxPingsOut = config.get("maxPingsOut", maxPingsOut);
		pingInterval = config.get("pingInterval", pingInterval);
		timeout = config.get("socketTimeout", timeout);
		tlsDebug = config.get("tlsDebug", tlsDebug);
		verbose = config.get("verbose", verbose);
	}

	// --- CONNECT ---

	@Override
	public void connect() {
		try {

			// Create NATS client options
			Options.Builder builder = new Options.Builder();
			if (secure) {
				builder.secure();
			}
			if (username != null && password != null && !username.isEmpty() && !password.isEmpty()) {
				builder.userInfo(username, password);
			}
			if (sslContext != null) {
				builder.sslContext(sslContext);
			}
			if (dontRandomize) {
				builder.dontRandomize();
			}
			builder.maxPingsOut(maxPingsOut);
			builder.pingInterval(pingInterval);
			builder.timeout(timeout);
			if (tlsDebug) {
				builder.tlsDebug();
			}
			if (verbose) {
				builder.verbose();
			}
			builder.disconnectedCb(this);
			builder.noReconnect();
			Options options = builder.build();

			// Connect to NATS server
			disconnect();
			StringBuilder urlList = new StringBuilder(128);
			for (String url : urls) {
				if (urlList.length() > 0) {
					urlList.append(',');
				}
				if (url.indexOf(':') == -1) {
					url = url + ":4222";
				}
				if (url.indexOf("://") == -1) {
					url = "nats://" + url;
				}
				urlList.append(url);
			}
			client = Nats.connect(urlList.toString(), options);
			logger.info("NATS pub-sub connection estabilished.");
			connected();
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

	@Override
	public void onDisconnect(ConnectionEvent event) {
		logger.info("NATS pub-sub connection disconnected.");
		reconnect();
	}

	protected void disconnect() {
		if (client != null) {
			try {
				client.close();
			} catch (Throwable cause) {
				logger.warn("Unexpected error occured while closing NATS client!", cause);
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
		disconnect();
	}

	// --- SUBSCRIBE ---

	@Override
	public Promise subscribe(String channel) {
		if (client != null) {
			try {
				client.subscribe(channel, this);
			} catch (Exception cause) {
				return Promise.reject(cause);
			}
		}
		return Promise.resolve();
	}

	// --- MESSAGE RECEIVED ---

	@Override
	public void onMessage(Message msg) {
		received(msg.getSubject(), msg.getData());
	}

	// --- PUBLISH ---

	@Override
	public void publish(String channel, Tree message) {
		if (client != null) {
			try {
				if (debug) {
					logger.info("Submitting message to channel \"" + channel + "\":\r\n" + message.toString());
				}
				client.publish(channel, serializer.write(message));
			} catch (Exception cause) {
				logger.warn("Unable to send message to NATS server!", cause);
				reconnect();
			}
		}
	}

	// --- GETTERS / SETTERS ---

	public String[] getUrls() {
		return urls;
	}

	public void setUrls(String[] urls) {
		this.urls = urls;
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

	public SSLContext getSslContext() {
		return sslContext;
	}

	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public boolean isSecure() {
		return secure;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public boolean isDontRandomize() {
		return dontRandomize;
	}

	public void setDontRandomize(boolean dontRandomize) {
		this.dontRandomize = dontRandomize;
	}

	public int getMaxPingsOut() {
		return maxPingsOut;
	}

	public void setMaxPingsOut(int maxPingsOut) {
		this.maxPingsOut = maxPingsOut;
	}

	public long getPingInterval() {
		return pingInterval;
	}

	public void setPingInterval(long pingInterval) {
		this.pingInterval = pingInterval;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public boolean isTlsDebug() {
		return tlsDebug;
	}

	public void setTlsDebug(boolean tlsDebug) {
		this.tlsDebug = tlsDebug;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

}