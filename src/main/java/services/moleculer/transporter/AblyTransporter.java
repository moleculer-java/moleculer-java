/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.ably.lib.realtime.AblyRealtime;
import io.ably.lib.realtime.Channel;
import io.ably.lib.realtime.CompletionListener;
import io.ably.lib.realtime.ConnectionEvent;
import io.ably.lib.realtime.ConnectionStateListener;
import io.ably.lib.types.ClientOptions;
import io.ably.lib.types.ErrorInfo;
import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.service.Name;

/**
 * The Ably Realtime service (https://www.ably.io) organizes the message traffic
 * within applications into named channels. Channels are the "unit" of message
 * distribution; clients attach to any number of channels to subscribe to
 * messages, and every message published to a channel is broadcasted to all
 * subscribers. This scalable and resilient messaging pattern is commonly called
 * pub/sub. Important: The "apiKey" role must have "Publish" and "Subscribe"
 * privileges. Usage:
 * 
 * <pre>
 * ServiceBroker broker = ServiceBroker.builder()
 *                                     .nodeID("node1")
 *                                     .transporter(new AblyTransporter("apiKey"))
 *                                     .build();
 * </pre>
 * 
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/io.ably/ably-java<br>
 * compile group: 'io.ably', name: 'ably-java', version: '1.1.8'
 *
 * @see AmqpTransporter
 * @see TcpTransporter
 * @see RedisTransporter
 * @see NatsTransporter
 * @see MqttTransporter
 * @see JmsTransporter
 * @see GoogleTransporter
 * @see KafkaTransporter
 */
@Name("Ably Realtime Transporter")
public class AblyTransporter extends Transporter {

	// --- CLIENT OPTIONS ---

	protected ClientOptions options = new ClientOptions();

	// --- CHANNELS ---

	protected ConcurrentHashMap<String, Channel> publishedChannels = new ConcurrentHashMap<>();
	protected ConcurrentHashMap<String, Channel> subscribedChannels = new ConcurrentHashMap<>();

	// --- ABLY CLIENT ---

	protected AblyRealtime client;

	// --- STARTED FLAG ---

	protected final AtomicBoolean started = new AtomicBoolean();

	// --- CONSTUCTORS ---

	public AblyTransporter() {
		this(null);
	}

	public AblyTransporter(String apiKey) {
		options.key = apiKey;
		options.autoConnect = false;
		options.echoMessages = false;
	}

	// --- CONNECT ---

	@Override
	public void connect() {
		try {

			// Create client connection
			disconnect();
			client = new AblyRealtime(options);
			client.connection.on(new ConnectionStateListener() {

				@Override
				public final void onConnectionStateChanged(ConnectionStateChange change) {
					if (change.event == ConnectionEvent.connected) {
						logger.info("Ably realtime connection estabilished.");
						connected();
						return;
					}
					if (change.event == ConnectionEvent.failed) {
						logger.warn("Unable to connect to Ably server (event name: " + change.event.name() + ")!");
						reconnect();
						return;
					}
				}

			});
			client.connect();
			started.set(true);
		} catch (Exception cause) {
			String msg = cause.getMessage();
			if (msg == null || msg.isEmpty()) {
				msg = "Unable to connect to Ably server!";
			} else if (!msg.endsWith("!") && !msg.endsWith(".")) {
				msg += "!";
			}
			logger.warn(msg);
			reconnect();
		}
	}

	// --- DISCONNECT ---

	protected void disconnect() {
		if (publishedChannels != null) {
			for (Channel abblyChannel : publishedChannels.values()) {
				try {
					abblyChannel.detach();
				} catch (Exception ignore) {

					// Ignored
				}
			}
			publishedChannels.clear();
		}
		if (subscribedChannels != null) {
			for (Channel abblyChannel : subscribedChannels.values()) {
				try {
					abblyChannel.unsubscribe();
				} catch (Exception ignore) {

					// Ignored
				}
			}
			subscribedChannels.clear();
		}
		if (client != null) {
			client.connection.on(ConnectionEvent.disconnected, new ConnectionStateListener() {

				@Override
				public final void onConnectionStateChanged(ConnectionStateChange change) {
					if (debug && change.event == ConnectionEvent.disconnected) {
						logger.info("Ably realtime connection disconnected.");
					}
				}

			});
			client.close();

			// Notify internal listeners
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
		disconnect();
	}

	// --- SUBSCRIBE ---

	@Override
	public Promise subscribe(String channel) {
		if (client != null) {
			try {
				Channel abblyChannel = client.channels.get(channel);
				subscribedChannels.put(channel, abblyChannel);
				abblyChannel.subscribe(channel, msg -> {

					// We are running in the shared executor's pool,
					// do not create new task.
					processReceivedMessage(channel, (byte[]) msg.data);

				});
			} catch (Exception cause) {
				error(cause);
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
				Channel abblyChannel = publishedChannels.get(channel);
				if (abblyChannel == null) {
					abblyChannel = client.channels.get(channel);
					publishedChannels.put(channel, abblyChannel);
				}
				abblyChannel.publish(channel, serializer.write(message), new CompletionListener() {

					@Override
					public final void onSuccess() {
						if (debug) {
							logger.debug("Message transfered successfully.");
						}
					}

					@Override
					public final void onError(ErrorInfo reason) {
						error(new IOException(reason.toString()));
					}

				});
			} catch (Exception cause) {
				logger.warn("Unable to send message to Ably server!", cause);
			}
		}
	}

	// --- GETTERS / SETTERS ---

	public ClientOptions getOptions() {
		return options;
	}

	public void setOptions(ClientOptions options) {
		this.options = Objects.requireNonNull(options);
	}

}