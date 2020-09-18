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
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.service.Name;

/**
 * Kafka Transporter. Kafka is used for building real-time data pipelines and
 * streaming apps. It is horizontally scalable, fault-tolerant, wicked fast, and
 * runs in production in thousands of companies. Sample of usage:<br>
 * 
 * <pre>
 * KafkaTransporter kafka = new KafkaTransporter();
 * kafka.setUrls(new String[] { "192.168.51.29:9092" });
 * kafka.setDebug(true);
 * kafka.setProducerProperty("session.timeout.ms", "30000");
 * ServiceBroker broker = ServiceBroker.builder().transporter(kafka).build();
 * // broker.createService(new Service("test") {...});
 * broker.start();
 * </pre>
 * 
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients<br>
 * compile group: 'org.apache.kafka', name: 'kafka-clients', version: '2.4.0'
 * <br>
 *
 * @see TcpTransporter
 * @see RedisTransporter
 * @see NatsTransporter
 * @see NatsStreamingTransporter
 * @see MqttTransporter
 * @see JmsTransporter
 * @see AmqpTransporter
 */
@Name("Kafka Transporter")
public class KafkaTransporter extends Transporter {

	// --- PROPERTIES ---

	protected Properties producerProperties = new Properties();
	protected Properties consumerProperties = new Properties();

	protected String[] urls = { "localhost:9092" };

	// --- KAFKA PRODUCER / MESSAGE SENDER ---

	protected KafkaProducer<byte[], byte[]> producer;

	// --- KAFKA CONSUMER / MESSAGE RECEIVER ---

	protected KafkaPoller poller;

	// --- EXECUTOR ---

	/**
	 * Executor of reader loop for incoming messages
	 */
	protected ExecutorService executor;
	
	protected boolean shutDownThreadPools;

	// --- CONSTUCTORS ---

	public KafkaTransporter() {
	}

	public KafkaTransporter(String... urls) {
		this.urls = urls;
	}

	// --- CONNECT ---

	@Override
	public void connect() {
		try {

			// Create Kafka clients
			disconnect();
			StringBuilder urlList = new StringBuilder(128);
			for (String url : urls) {
				if (urlList.length() > 0) {
					urlList.append(',');
				}

				// Add default port
				if (url.indexOf(':') == -1) {
					url = url + ":9092";
				}

				// Remove protocol prefix ("tcp://127.0.0.0")
				int i = url.indexOf("://");
				if (i > -1 && i < url.length() - 4) {
					url = url.substring(i + 3);
				}
				urlList.append(url);
			}
			producerProperties.put("bootstrap.servers", urlList.toString());
			consumerProperties.put("bootstrap.servers", urlList.toString());

			// Set unique "node ID" as Kafka "group ID"
			consumerProperties.setProperty("group.id", nodeID);

			// Create producer
			ByteArraySerializer byteArraySerializer = new ByteArraySerializer();
			producer = new KafkaProducer<>(producerProperties, byteArraySerializer, byteArraySerializer);

			// Start reader loop
			poller = new KafkaPoller(this);
			if (executor == null) {
				executor = Executors.newSingleThreadExecutor();
				shutDownThreadPools = true;
			}
			executor.execute(poller);

			// Start subscribing channels...
			connected();

		} catch (Exception cause) {
			reconnect(cause);
		}
	}

	// --- DISCONNECT ---

	protected void disconnect() {
		boolean notify = false;
		if (poller != null) {
			poller.stop();
		}
		if (executor != null && shutDownThreadPools) {
			notify = true;
			try {
				executor.shutdown();
			} catch (Exception ignored) {
			}
			executor = null;
		}
		if (poller != null) {
			notify = true;
			poller = null;
		}
		if (producer != null) {
			notify = true;
			try {
				producer.close(Duration.ofSeconds(10));
			} catch (Exception ignored) {
			}
			producer = null;
		}
		
		// Notify internal listeners
		if (notify) {
			broadcastTransporterDisconnected();
		}
	}

	// --- INPROCESS READER ---

	protected static class KafkaPoller implements Runnable {

		// --- STATUS CODES ---

		protected static final int UNSUBSCRIBED = 0;
		protected static final int SUBSCRIBED = 1;
		protected static final int STOPPING = 2;

		// --- KAFKA CONSUMER / MESSAGE RECEIVER ---

		protected KafkaConsumer<byte[], byte[]> consumer;

		// --- PARENT TRANSPORTER ---

		protected final KafkaTransporter transporter;

		// --- STATUS ---

		protected final AtomicInteger status = new AtomicInteger(UNSUBSCRIBED);

		// --- ERROR MARKER ---

		protected static boolean firstError = true;

		// --- SET OF SUBSCRIPTIONS ---

		protected HashSet<String> subscriptions = new HashSet<>();

		// --- CONSTRUCTOR ---

		protected KafkaPoller(KafkaTransporter transporter) {
			this.transporter = transporter;

			// Create consumer
			ByteArrayDeserializer deserializer = new ByteArrayDeserializer();
			consumer = new KafkaConsumer<>(transporter.consumerProperties, deserializer, deserializer);
		}

		// --- READER LOOP -- ---

		public void run() {
			try {

				// Loop
				int current;
				while ((current = status.get()) != STOPPING) {

					// Try to read incoming records
					if (current == UNSUBSCRIBED) {
						Thread.sleep(1000);
						continue;
					}
					ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofSeconds(5));
					if (records == null || records.isEmpty()) {
						continue;
					}
					for (ConsumerRecord<byte[], byte[]> record : records) {

						// Process incoming records
						transporter.received(record.topic(), record.value());
					}
				}
			} catch (InterruptedException interrupt) {

				// Interrupted...

			} catch (WakeupException wakeUp) {

				// Stopping...

			} catch (Exception cause) {
				if (firstError) {
					firstError = false;
					transporter.logger.error("Unable to connect to Kafka server!", cause);
				}
				if (cause != null) {

					// Reconnect
					transporter.error(cause);
				}
			} finally {
				if (consumer != null) {
					try {
						consumer.close();
					} catch (Exception cause) {
						transporter.logger.warn("Unable to close Kafka consumer!", cause);
					}
				}
				consumer = null;
			}
		}

		// --- SUBSCRIBE ---

		protected void subscribe(String channel) {
			subscriptions.add(channel);
			consumer.subscribe(subscriptions);
			status.set(SUBSCRIBED);
		}

		// --- STOP ---

		protected void stop() {
			status.set(STOPPING);
			consumer.wakeup();
		}

	}

	// --- RECONNECT ---

	protected void reconnect(Throwable cause) {
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
		poller.subscribe(channel);
		return Promise.resolve();
	}

	// --- PUBLISH ---

	@Override
	public void publish(String channel, Tree message) {
		if (producer != null) {
			try {
				if (debug && (debugHeartbeats || !channel.endsWith(heartbeatChannel))) {
					logger.info("Submitting message to channel \"" + channel + "\":\r\n" + message.toString());
				}
				
				// Metrics
				byte[] bytes = serializer.write(message);
				if (metrics != null) {
					metrics.increment(MOLECULER_TRANSPORTER_PACKETS_SENT_TOTAL, MOLECULER_TRANSPORTER_PACKETS_SENT_TOTAL_DESC);
					metrics.increment(MOLECULER_TRANSPORTER_PACKETS_SENT_BYTES, MOLECULER_TRANSPORTER_PACKETS_SENT_BYTES_DESC,
							bytes.length);
				}
				
				// Send
				producer.send(new ProducerRecord<byte[], byte[]>(channel, bytes));
			} catch (Exception cause) {
				logger.warn("Unable to send message to Kafka server!", cause);
			}
		}
	}

	// --- SET CLIENT PROPERTIES ---

	public void setProducerProperty(String key, String value) {
		producerProperties.setProperty(key, value);
	}

	public void setConsumerProperty(String key, String value) {
		consumerProperties.setProperty(key, value);
	}

	// --- GETTERS / SETTERS ---

	public String[] getUrls() {
		return urls;
	}

	public void setUrls(String[] urls) {
		this.urls = urls;
	}

	public Properties getProducerProperties() {
		return producerProperties;
	}

	public void setProducerProperties(Properties producerProperties) {
		this.producerProperties = producerProperties;
	}

	public Properties getConsumerProperties() {
		return consumerProperties;
	}

	public void setConsumerProperties(Properties consumerProperties) {
		this.consumerProperties = consumerProperties;
	}

	public ExecutorService getExecutor() {
		return executor;
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	public boolean isShutDownThreadPools() {
		return shutDownThreadPools;
	}

	public void setShutDownThreadPools(boolean shutDownThreadPools) {
		this.shutDownThreadPools = shutDownThreadPools;
	}

}