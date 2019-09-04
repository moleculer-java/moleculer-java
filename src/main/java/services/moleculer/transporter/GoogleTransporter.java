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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.threeten.bp.Duration;

import com.google.api.core.ApiClock;
import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.api.gax.rpc.NotFoundException;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;
import services.moleculer.util.CommonUtils;

/**
 * Google Cloud Pub/Sub Transporter. The Google Cloud Pub/Sub service allows
 * applications to exchange messages reliably, quickly, and asynchronously.<br>
 * <br>
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/com.google.cloud/google-cloud-pubsub
 * <br>
 * compile group: 'com.google.cloud', name: 'google-cloud-pubsub', version:
 * '1.89.0'<br>
 *
 * @see TcpTransporter
 * @see RedisTransporter
 * @see NatsTransporter
 * @see MqttTransporter
 * @see JmsTransporter
 * @see KafkaTransporter
 * @see AmqpTransporter
 */
@Name("Google Cloud Pub/Sub Transporter")
public class GoogleTransporter extends Transporter {

	// --- PROPERTIES ---

	protected String projectID = ServiceOptions.getDefaultProjectId();
	protected BatchingSettings batchingSettings;
	protected TransportChannelProvider channelProvider;
	protected CredentialsProvider credentialsProvider;
	protected ExecutorProvider executorProvider;
	protected HeaderProvider headerProvider;
	protected RetrySettings retrySettings;
	protected Duration maxAckExtensionPeriod;
	protected int parallelPullCount;
	protected int ackDeadlineSeconds = 10;
	protected TransportChannelProvider transportChannelProvider;
	protected FlowControlSettings flowControlSettings;
	protected ApiClock clock;

	// --- CHANNEL NAME/PUBLISHER MAP ---

	protected final HashMap<String, Publisher> publishers = new HashMap<>(64);

	// --- CHANNEL NAME/SUBSCRIBER MAP ---

	protected final HashMap<String, Subscriber> subscribers = new HashMap<>(64);

	// --- TOPIC ADMIN CLIENT ---

	protected TopicAdminClient topicAdmin;

	// --- SUBSCRIPTION ADMIN CLIENT ---

	protected SubscriptionAdminClient subscriptionAdmin;

	// --- SHARED EXECUTOR ---

	protected ExecutorProvider defaultExecutorProvider;

	// --- CONNECTED FLAG ---

	protected final AtomicBoolean connected = new AtomicBoolean();

	// --- CONSTRUCTOR ---

	public GoogleTransporter() {

		// Use default / user-defined credentials provider
		this((Credentials) null);
	}

	public GoogleTransporter(String credetialsURL) throws Exception {

		// Load credentials JSON from URL or file system
		this(CommonUtils.readTree(credetialsURL));
	}

	public GoogleTransporter(Tree credetials) throws Exception {

		// Load credentials from a "Tree" structure
		this(GoogleCredentials.fromStream(new ByteArrayInputStream(credetials.toBinary())));
	}

	public GoogleTransporter(Credentials credentials) {

		// Slow down heartbeat timer
		heartbeatInterval = 15;
		heartbeatTimeout = 35;

		// Increase subscription timeout
		subscriptionTimeout = 30;

		// Use custom credentials provider
		if (credentials != null) {
			credentialsProvider = new CredentialsProvider() {

				@Override
				public Credentials getCredentials() throws IOException {
					return credentials;
				}

			};
		}
	}

	// --- START ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Create shared executor provider
		defaultExecutorProvider = new ExecutorProvider() {

			@Override
			public final boolean shouldAutoClose() {
				return false;
			}

			@Override
			public final ScheduledExecutorService getExecutor() {
				return scheduler;
			}

		};
	}

	// --- CONNECT ---

	@Override
	public void connect() {
		try {
			disconnect();

			// Create topic admin
			TopicAdminSettings.Builder topicBuilder = TopicAdminSettings.newBuilder();
			if (credentialsProvider != null) {
				topicBuilder.setCredentialsProvider(credentialsProvider);
			}
			if (executorProvider != null) {
				topicBuilder.setExecutorProvider(executorProvider);
			} else {
				topicBuilder.setExecutorProvider(defaultExecutorProvider);
			}
			if (headerProvider != null) {
				topicBuilder.setHeaderProvider(headerProvider);
			}
			if (transportChannelProvider != null) {
				topicBuilder.setTransportChannelProvider(transportChannelProvider);
			}
			if (clock != null) {
				topicBuilder.setClock(clock);
			}
			topicAdmin = TopicAdminClient.create(topicBuilder.build());

			// Create subscription admin
			SubscriptionAdminSettings.Builder subscriptionBuilder = SubscriptionAdminSettings.newBuilder();
			if (credentialsProvider != null) {
				subscriptionBuilder.setCredentialsProvider(credentialsProvider);
			}
			if (executorProvider != null) {
				subscriptionBuilder.setExecutorProvider(executorProvider);
			} else {
				subscriptionBuilder.setExecutorProvider(defaultExecutorProvider);
			}
			if (headerProvider != null) {
				subscriptionBuilder.setHeaderProvider(headerProvider);
			}
			if (transportChannelProvider != null) {
				subscriptionBuilder.setTransportChannelProvider(transportChannelProvider);
			}
			if (clock != null) {
				subscriptionBuilder.setClock(clock);
			}
			subscriptionAdmin = SubscriptionAdminClient.create(subscriptionBuilder.build());

			connected();

			connected.set(true);
		} catch (Exception cause) {
			reconnect(cause);
		}
	}

	// --- DISCONNECT ---

	protected void disconnect() {
		connected.set(false);
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
		if (topicAdmin != null) {
			try {
				topicAdmin.close();
			} catch (Exception ingored) {
			}
			topicAdmin = null;
		}
		if (subscriptionAdmin != null) {
			try {
				subscriptionAdmin.close();
			} catch (Exception ingored) {
			}
			subscriptionAdmin = null;
		}
	}

	// --- RECONNECT ---

	protected void reconnect(Throwable cause) {
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
		try {

			// Create topic
			ProjectTopicName topicName = ProjectTopicName.of(projectID, channel);
			Topic topic = null;
			try {
				topic = topicAdmin.getTopic(topicName);
			} catch (NotFoundException notFound) {
			}
			if (topic == null) {
				topic = topicAdmin.createTopic(topicName);
				logger.info("Topic \"" + topic.getName() + "\" created successfully.");
			}

			// Create subscription
			String nodeSubscription;
			if (channel.endsWith('.' + nodeID)) {
				nodeSubscription = channel;
			} else {
				nodeSubscription = channel + '-' + nodeID;
			}
			ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectID, nodeSubscription);
			Subscription subscription = null;
			try {
				subscription = subscriptionAdmin.getSubscription(subscriptionName);
			} catch (NotFoundException notFound) {
			}
			if (subscription == null) {
				subscription = subscriptionAdmin.createSubscription(subscriptionName, topicName,
						PushConfig.getDefaultInstance(), ackDeadlineSeconds);
				logger.info("Subscription \"" + subscription.getName() + "\" created successfully.");
			}

			// Create subscriber
			synchronized (subscribers) {
				if (!subscribers.containsKey(nodeSubscription)) {
					Subscriber.Builder builder = Subscriber.newBuilder(subscriptionName, (message, consumer) -> {

						// Message received
						try {

							// We are running in a netty executor's pool,
							// do not create new task.
							processReceivedMessage(channel, message.getData().toByteArray());
						} finally {
							consumer.ack();
						}

					});
					if (channelProvider != null) {
						builder.setChannelProvider(channelProvider);
					}
					if (credentialsProvider != null) {
						builder.setCredentialsProvider(credentialsProvider);
					}
					if (executorProvider != null) {
						builder.setExecutorProvider(executorProvider);
						builder.setSystemExecutorProvider(executorProvider);
					} else {
						builder.setExecutorProvider(defaultExecutorProvider);
						builder.setSystemExecutorProvider(defaultExecutorProvider);
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
					if (flowControlSettings != null) {
						builder.setFlowControlSettings(flowControlSettings);
					}
					Subscriber subscriber = builder.build();
					subscriber.startAsync();
					subscribers.put(nodeSubscription, subscriber);
					logger.info(
							"Subscriber created for subscription \"" + subscriber.getSubscriptionNameString() + "\".");
				}
			}

		} catch (Exception cause) {
			return Promise.reject(cause);
		}
		return Promise.resolve();
	}

	// --- PUBLISH ---

	@Override
	public void publish(String channel, Tree message) {
		if (connected.get()) {
			try {
				if (debug && (debugHeartbeats || !channel.endsWith(heartbeatChannel))) {
					logger.info("Submitting message to channel \"" + channel + "\":\r\n" + message.toString());
				}
				byte[] bytes = serializer.write(message);
				PubsubMessage msg = PubsubMessage.newBuilder().setData(ByteString.copyFrom(bytes)).build();
				getOrCreatePublisher(channel).publish(msg);
			} catch (Exception cause) {
				logger.warn("Unable to send message to Google Cloud service!", cause);
			}
		}
	}

	protected Publisher getOrCreatePublisher(String channel) throws Exception {
		Publisher publisher;
		synchronized (publishers) {
			publisher = publishers.get(channel);
			if (publisher != null) {
				return publisher;
			}
			ProjectTopicName topicName = ProjectTopicName.of(projectID, channel);
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
				builder.setExecutorProvider(defaultExecutorProvider);
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

	// --- GETTERS / SETTERS ---

	public String getProjectID() {
		return projectID;
	}

	public void setProjectID(String projectID) {
		if (projectID != null) {
			this.projectID = projectID;
		}
	}

	public BatchingSettings getBatchingSettings() {
		return batchingSettings;
	}

	public void setBatchingSettings(BatchingSettings batchingSettings) {
		this.batchingSettings = batchingSettings;
	}

	public TransportChannelProvider getChannelProvider() {
		return channelProvider;
	}

	public void setChannelProvider(TransportChannelProvider channelProvider) {
		this.channelProvider = channelProvider;
	}

	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	public ExecutorProvider getExecutorProvider() {
		return executorProvider;
	}

	public void setExecutorProvider(ExecutorProvider executorProvider) {
		this.executorProvider = executorProvider;
	}

	public HeaderProvider getHeaderProvider() {
		return headerProvider;
	}

	public void setHeaderProvider(HeaderProvider headerProvider) {
		this.headerProvider = headerProvider;
	}

	public RetrySettings getRetrySettings() {
		return retrySettings;
	}

	public void setRetrySettings(RetrySettings retrySettings) {
		this.retrySettings = retrySettings;
	}

	public Duration getMaxAckExtensionPeriod() {
		return maxAckExtensionPeriod;
	}

	public void setMaxAckExtensionPeriod(Duration maxAckExtensionPeriod) {
		this.maxAckExtensionPeriod = maxAckExtensionPeriod;
	}

	public int getParallelPullCount() {
		return parallelPullCount;
	}

	public void setParallelPullCount(int parallelPullCount) {
		this.parallelPullCount = parallelPullCount;
	}

	public int getAckDeadlineSeconds() {
		return ackDeadlineSeconds;
	}

	public void setAckDeadlineSeconds(int ackDeadlineSeconds) {
		this.ackDeadlineSeconds = ackDeadlineSeconds;
	}

	public TransportChannelProvider getTransportChannelProvider() {
		return transportChannelProvider;
	}

	public void setTransportChannelProvider(TransportChannelProvider transportChannelProvider) {
		this.transportChannelProvider = transportChannelProvider;
	}

	public FlowControlSettings getFlowControlSettings() {
		return flowControlSettings;
	}

	public void setFlowControlSettings(FlowControlSettings flowControlSettings) {
		this.flowControlSettings = flowControlSettings;
	}

	public ApiClock getClock() {
		return clock;
	}

	public void setClock(ApiClock clock) {
		this.clock = clock;
	}

}