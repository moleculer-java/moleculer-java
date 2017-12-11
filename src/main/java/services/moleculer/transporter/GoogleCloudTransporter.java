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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.threeten.bp.Duration;

import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.TopicName;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * Google Cloud Pub/Sub Transporter. The Google Cloud Pub/Sub service allows
 * applications to exchange messages reliably, quickly, and asynchronously.<br>
 * <br>
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/com.google.cloud/google-cloud-pubsub
 * <br>
 * compile group: 'com.google.cloud', name: 'google-cloud-pubsub', version:
 * '0.30.0-beta'<br>
 * <br>
 * DRAFT / UNFINISHED VERSION
 * 
 * @see RedisTransporter
 * @see NatsTransporter
 * @see MqttTransporter
 * @see AmqpTransporter
 * @see JmsTransporter
 * @see SocketClusterTransporter
 */
@Name("Google Cloud Pub/Sub Transporter")
public final class GoogleCloudTransporter extends Transporter {

	// --- PROPERTIES ---

	private String projectID = ServiceOptions.getDefaultProjectId();
	private BatchingSettings batchingSettings;
	private TransportChannelProvider channelProvider;
	private CredentialsProvider credentialsProvider;
	private ExecutorProvider executorProvider;
	private HeaderProvider headerProvider;
	private RetrySettings retrySettings;
	private Duration maxAckExtensionPeriod;
	private int parallelPullCount;

	// --- CHANNEL NAME/PUBLISHER MAP ---

	private final HashMap<String, Publisher> publishers = new HashMap<>(64);

	// --- CHANNEL NAME/SUBSCRIBER MAP ---

	private final HashMap<String, Subscriber> subscribers = new HashMap<>(64);

	// --- CONSTUCTORS ---

	public GoogleCloudTransporter() {
		super();
	}

	public GoogleCloudTransporter(String prefix) {
		super(prefix);
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

		// Connect to Google Cloud server
		connect();
	}

	// --- CONNECT ---

	private final void connect() {
		try {

			// Create Google Cloud client
			disconnect();
			if (System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
				throw new SecurityException("Environment property \"GOOGLE_APPLICATION_CREDENTIALS\" is missing!");
			}
			connected();

		} catch (Exception cause) {
			reconnect(cause);
		}
	}

	// --- DISCONNECT ---

	private final void disconnect() {
		boolean connected;
		synchronized (publishers) {
			connected = !publishers.isEmpty();
		}
		if (!connected) {
			synchronized (subscribers) {
				connected = !subscribers.isEmpty();
			}
		}
		if (connected) {
			try {
				synchronized (publishers) {
					for (Publisher publisher : publishers.values()) {
						try {
							publisher.shutdown();
						} catch (Exception ingored) {
						}
					}
					publishers.clear();
				}
				synchronized (subscribers) {
					for (Subscriber subscriber : subscribers.values()) {
						try {
							subscriber.stopAsync();
						} catch (Exception ingored) {
						}
					}
					subscribers.clear();
				}
			} catch (Throwable cause) {
				logger.warn("Unexpected error occured while closing Google Cloud client!", cause);
			} finally {
				disconnected();
			}
		}
	}

	// --- RECONNECT ---

	private final void reconnect(Throwable cause) {
		if (cause != null) {
			String msg = cause.getMessage();
			if (msg == null || msg.isEmpty()) {
				msg = "Unable to connect to Google Cloud service!";
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
			getOrCreatePublisher(channel);

			// Create subscriber
			synchronized (subscribers) {
				if (!subscribers.containsKey(channel)) {
					Subscriber.Builder builder = Subscriber.newBuilder(SubscriptionName.of(projectID, channel),
							(message, consumer) -> {

								// Message received
								byte[] bytes = message.getData().toByteArray();
								received(channel, bytes);
								consumer.ack();

							});
					builder.setChannelProvider(channelProvider);
					builder.setCredentialsProvider(credentialsProvider);
					if (executorProvider != null) {
						builder.setExecutorProvider(executorProvider);
					} else {
						builder.setExecutorProvider(new ExecutorProvider() {

							@Override
							public final boolean shouldAutoClose() {
								return false;
							}

							@Override
							public final ScheduledExecutorService getExecutor() {
								return scheduler;
							}

						});
					}
					if (headerProvider != null) {
						builder.setHeaderProvider(headerProvider);
					}
					if (maxAckExtensionPeriod != null) {
						builder.setMaxAckExtensionPeriod(maxAckExtensionPeriod);
					}
					if (parallelPullCount > 0) {
						builder.setParallelPullCount(parallelPullCount);
					}
					if (executorProvider != null) {
						builder.setSystemExecutorProvider(executorProvider);
					} else {
						builder.setSystemExecutorProvider(new ExecutorProvider() {

							@Override
							public final boolean shouldAutoClose() {
								return false;
							}

							@Override
							public final ScheduledExecutorService getExecutor() {
								return scheduler;
							}

						});
					}
					Subscriber subscriber = builder.build();
					subscriber.startAsync();
					subscribers.put(channel, subscriber);
				}
			}

		} catch (Exception cause) {
			return Promise.reject(cause);
		}
		return Promise.resolve();
	}

	private final Publisher getOrCreatePublisher(String channel) throws Exception {
		Publisher publisher;
		synchronized (publishers) {
			publisher = publishers.get(channel);
			if (publisher != null) {
				return publisher;
			}
			TopicName topicName = TopicName.of(projectID, channel);
			Publisher.Builder builder = Publisher.newBuilder(topicName);
			if (batchingSettings != null) {
				builder.setBatchingSettings(batchingSettings);
			}
			if (channelProvider != null) {
				builder.setChannelProvider(channelProvider);
			}
			if (credentialsProvider != null) {
				builder.setCredentialsProvider(credentialsProvider);
			}
			if (executorProvider != null) {
				builder.setExecutorProvider(executorProvider);
			} else {
				builder.setExecutorProvider(new ExecutorProvider() {

					@Override
					public final boolean shouldAutoClose() {
						return false;
					}

					@Override
					public final ScheduledExecutorService getExecutor() {
						return scheduler;
					}

				});
			}
			if (headerProvider != null) {
				builder.setHeaderProvider(headerProvider);
			}
			if (retrySettings != null) {
				builder.setRetrySettings(retrySettings);
			}
			publisher = builder.build();
			publishers.put(channel, publisher);
		}
		return publisher;
	}

	// --- PUBLISH ---

	@Override
	public final void publish(String channel, Tree message) {
		try {
			if (debug) {
				logger.info("Submitting message to channel \"" + channel + "\":\r\n" + message.toString());
			}
			byte[] bytes = serializer.write(message);
			PubsubMessage msg = PubsubMessage.newBuilder().setData(ByteString.copyFrom(bytes)).build();
			getOrCreatePublisher(channel).publish(msg);
		} catch (Exception cause) {
			logger.warn("Unable to send message to Google Cloud service!", cause);
		}
	}

	// --- GETTERS / SETTERS ---

	public final String getProjectID() {
		return projectID;
	}

	public final void setProjectID(String projectID) {
		this.projectID = projectID;
	}

	public final BatchingSettings getBatchingSettings() {
		return batchingSettings;
	}

	public final void setBatchingSettings(BatchingSettings batchingSettings) {
		this.batchingSettings = batchingSettings;
	}

	public final TransportChannelProvider getChannelProvider() {
		return channelProvider;
	}

	public final void setChannelProvider(TransportChannelProvider channelProvider) {
		this.channelProvider = channelProvider;
	}

	public final CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	public final void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	public final ExecutorProvider getExecutorProvider() {
		return executorProvider;
	}

	public final void setExecutorProvider(ExecutorProvider executorProvider) {
		this.executorProvider = executorProvider;
	}

	public final HeaderProvider getHeaderProvider() {
		return headerProvider;
	}

	public final void setHeaderProvider(HeaderProvider headerProvider) {
		this.headerProvider = headerProvider;
	}

	public final RetrySettings getRetrySettings() {
		return retrySettings;
	}

	public final void setRetrySettings(RetrySettings retrySettings) {
		this.retrySettings = retrySettings;
	}

	public final Duration getMaxAckExtensionPeriod() {
		return maxAckExtensionPeriod;
	}

	public final void setMaxAckExtensionPeriod(Duration maxAckExtensionPeriod) {
		this.maxAckExtensionPeriod = maxAckExtensionPeriod;
	}

	public final int getParallelPullCount() {
		return parallelPullCount;
	}

	public final void setParallelPullCount(int parallelPullCount) {
		this.parallelPullCount = parallelPullCount;
	}

}