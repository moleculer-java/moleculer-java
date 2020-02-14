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

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;

import io.datatree.Promise;
import io.datatree.Tree;
import io.nats.client.AuthHandler;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.Statistics;
import services.moleculer.service.Name;

/**
 * NATS Transporter. NATS Server is a simple, high performance open source
 * messaging system for cloud native applications, IoT messaging, and
 * microservices architectures (website: https://nats.io). Tested with NATS
 * server version 1.3.0. Usage:
 * <pre>
 * ServiceBroker broker = ServiceBroker.builder().nodeID("node1")
 * .transporter(new NatsTransporter("localhost")).build();
 * </pre>
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/io.nats/jnats<br>
 * compile group: 'io.nats', name: 'jnats', version: '2.6.6'
 *
 * @see TcpTransporter
 * @see RedisTransporter
 * @see NatsStreamingTransporter
 * @see MqttTransporter
 * @see JmsTransporter
 * @see KafkaTransporter
 * @see AmqpTransporter
 */
@Name("NATS Transporter")
public class NatsTransporter extends Transporter implements MessageHandler, ConnectionListener {

	// --- PROPERTIES ---

	/**
	 * Username for basic authentication.
	 */
	protected String username;

	/**
	 * Password for basic authentication.
	 */
	protected String password;

	/**
	 * Use the default SSL Context, if it exists.
	 */
	protected boolean secure;

	/**
	 * Nats server url(s).
	 */
	protected String[] urls = { "localhost" };

	// --- OTHER NATS PROPERTIES ---

	/**
	 * Optional SSL Context.
	 */
	protected SSLContext sslContext;

	/**
	 * Turn off server pool randomization.
	 */
	protected boolean noRandomize;

	/**
	 * Set the maximum number of pings the client can have in flight.
	 */
	protected int maxPingsOut = Options.DEFAULT_MAX_PINGS_OUT;

	/**
	 * Set the interval between attempts to pings the server in MILLISECONDS.
	 */
	protected long pingInterval = Options.DEFAULT_PING_INTERVAL.toMillis();

	/**
	 * Set the timeout for connection attempts in MILLISECONDS.
	 */
	protected long connectionTimeout = Options.DEFAULT_CONNECTION_TIMEOUT.toMillis();

	/**
	 * Turn on verbose mode with the server.
	 */
	protected boolean verbose;

	/**
	 * Sets the initial size for buffers in the connection.
	 */
	protected int bufferSize = Options.DEFAULT_BUFFER_SIZE;

	/**
	 * Optional AuthHandler.
	 */
	protected AuthHandler authHandler;

	/**
	 * Turn off echo.
	 */
	protected boolean noEcho = true;

	/**
	 * Enable UTF8 channels.
	 */
	protected boolean utf8Support;

	/**
	 * Turn on pedantic mode for the server.
	 */
	protected boolean pedantic;

	/**
	 * Turn on advanced stats, primarily for test/benchmarks.
	 */
	protected boolean advancedStats;

	/**
	 * Set the SSL context to one that accepts any server certificate and has no
	 * client certificate.
	 */
	protected boolean opentls;

	/**
	 * Turn on the old request style that uses a new inbox and subscriber for
	 * each request.
	 */
	protected boolean oldRequestStyle;

	// --- NATS CONNECTION ---

	protected Connection client;

	// --- MESSAGE DISPATCHER ---

	protected Dispatcher dispatcher;

	// --- STARTED FLAG ---

	protected final AtomicBoolean started = new AtomicBoolean();

	// --- CONSTUCTORS ---

	public NatsTransporter() {
	}

	public NatsTransporter(String... urls) {
		this.urls = urls;
	}

	public NatsTransporter(String username, String password, boolean secure, String... urls) {
		this.username = username;
		this.password = password;
		this.secure = secure;
		this.urls = urls;
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
				builder.userInfo(username.toCharArray(), password.toCharArray());
			}
			if (sslContext != null) {
				builder.sslContext(sslContext);
			}
			if (noRandomize) {
				builder.noRandomize();
			}
			builder.maxPingsOut(maxPingsOut);
			builder.pingInterval(Duration.ofMillis(pingInterval));
			builder.connectionTimeout(Duration.ofMillis(connectionTimeout));
			if (verbose) {
				builder.verbose();
			}
			builder.bufferSize(bufferSize);
			builder.authHandler(authHandler);
			if (noEcho) {
				builder.noEcho();
			}
			if (opentls) {
				builder.opentls();
			}
			if (pedantic) {
				builder.pedantic();
			}
			if (advancedStats) {
				builder.turnOnAdvancedStats();
			}
			if (utf8Support) {
				builder.supportUTF8Subjects();
			}
			if (oldRequestStyle) {
				builder.oldRequestStyle();
			}
			builder.connectionListener(this);
			builder.noReconnect();

			// Set server URLs
			for (String url : urls) {
				if (url.indexOf(':') == -1) {
					url = url + ":4222";
				}
				if (url.indexOf("://") == -1) {
					url = "nats://" + url;
				}
				builder.server(url);
			}

			// Connect to NATS server
			disconnect();
			started.set(true);
			Options options = builder.build();
			client = Nats.connect(options);
			dispatcher = client.createDispatcher(this);
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

	protected void disconnect() {
		boolean notify = false;
		if (dispatcher != null && client != null) {
			notify = true;
			try {
				client.closeDispatcher(dispatcher);
			} catch (Throwable cause) {

				// Do nothing
			}
			dispatcher = null;
		}
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
		if (dispatcher != null) {
			try {
				dispatcher.subscribe(channel);
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
				client.publish(channel, serializer.write(message));
			} catch (Exception cause) {
				logger.warn("Unable to send message to NATS server!", cause);
				reconnect();
			}
		}
	}

	// --- GETTERS / SETTERS ---

	/**
	 * @return Statistics
	 */
	public Statistics getStatistics() {
		return client.getStatistics();
	}

	/**
	 * @return the connectionTimeout
	 */
	public long getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * @param connectionTimeout
	 *            the connectionTimeout to set
	 */
	public void setConnectionTimeout(long connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * @return the bufferSize
	 */
	public int getBufferSize() {
		return bufferSize;
	}

	/**
	 * @param bufferSize
	 *            the bufferSize to set
	 */
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	/**
	 * @return the authHandler
	 */
	public AuthHandler getAuthHandler() {
		return authHandler;
	}

	/**
	 * @param authHandler
	 *            the authHandler to set
	 */
	public void setAuthHandler(AuthHandler authHandler) {
		this.authHandler = authHandler;
	}

	/**
	 * @return the noEcho
	 */
	public boolean isNoEcho() {
		return noEcho;
	}

	/**
	 * @param noEcho
	 *            the noEcho to set
	 */
	public void setNoEcho(boolean noEcho) {
		this.noEcho = noEcho;
	}

	/**
	 * @return the utf8Support
	 */
	public boolean isUtf8Support() {
		return utf8Support;
	}

	/**
	 * @param utf8Support
	 *            the utf8Support to set
	 */
	public void setUtf8Support(boolean utf8Support) {
		this.utf8Support = utf8Support;
	}

	/**
	 * @return the pedantic
	 */
	public boolean isPedantic() {
		return pedantic;
	}

	/**
	 * @param pedantic
	 *            the pedantic to set
	 */
	public void setPedantic(boolean pedantic) {
		this.pedantic = pedantic;
	}

	/**
	 * @return the advancedStats
	 */
	public boolean isAdvancedStats() {
		return advancedStats;
	}

	/**
	 * @param advancedStats
	 *            the advancedStats to set
	 */
	public void setAdvancedStats(boolean advancedStats) {
		this.advancedStats = advancedStats;
	}

	/**
	 * @return the opentls
	 */
	public boolean isOpentls() {
		return opentls;
	}

	/**
	 * @param opentls
	 *            the opentls to set
	 */
	public void setOpentls(boolean opentls) {
		this.opentls = opentls;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username
	 *            the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the secure
	 */
	public boolean isSecure() {
		return secure;
	}

	/**
	 * @param secure
	 *            the secure to set
	 */
	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	/**
	 * @return the urls
	 */
	public String[] getUrls() {
		return urls;
	}

	/**
	 * @param urls
	 *            the urls to set
	 */
	public void setUrls(String[] urls) {
		this.urls = urls;
	}

	/**
	 * @return the sslContext
	 */
	public SSLContext getSslContext() {
		return sslContext;
	}

	/**
	 * @param sslContext
	 *            the sslContext to set
	 */
	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	/**
	 * @return the noRandomize
	 */
	public boolean isNoRandomize() {
		return noRandomize;
	}

	/**
	 * @param noRandomize
	 *            the noRandomize to set
	 */
	public void setNoRandomize(boolean noRandomize) {
		this.noRandomize = noRandomize;
	}

	/**
	 * @return the maxPingsOut
	 */
	public int getMaxPingsOut() {
		return maxPingsOut;
	}

	/**
	 * @param maxPingsOut
	 *            the maxPingsOut to set
	 */
	public void setMaxPingsOut(int maxPingsOut) {
		this.maxPingsOut = maxPingsOut;
	}

	/**
	 * @return the pingInterval
	 */
	public long getPingInterval() {
		return pingInterval;
	}

	/**
	 * @param pingInterval
	 *            the pingInterval to set
	 */
	public void setPingInterval(long pingInterval) {
		this.pingInterval = pingInterval;
	}

	/**
	 * @return the verbose
	 */
	public boolean isVerbose() {
		return verbose;
	}

	/**
	 * @param verbose
	 *            the verbose to set
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	/**
	 * @return the oldRequestStyle
	 */
	public boolean isOldRequestStyle() {
		return oldRequestStyle;
	}

	/**
	 * @param oldRequestStyle
	 *            the oldRequestStyle to set
	 */
	public void setOldRequestStyle(boolean oldRequestStyle) {
		this.oldRequestStyle = oldRequestStyle;
	}

}