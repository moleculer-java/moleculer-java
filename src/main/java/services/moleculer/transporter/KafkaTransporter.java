package services.moleculer.transporter;

import static services.moleculer.util.CommonUtils.parseURLs;

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

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * Kafka Transporter. Kafka is used for building real-time data pipelines and
 * streaming apps. It is horizontally scalable, fault-tolerant, wicked fast, and
 * runs in production in thousands of companies. Sample of usage:<br>
 * <br>
 * KafkaTransporter trans = new KafkaTransporter();<br>
 * trans.setUrls(new String[] { "192.168.51.29:9092" });<br>
 * trans.setDebug(true);<br>
 * trans.setProducerProperty("session.timeout.ms", "30000");<br>
 * ServiceBroker broker = ServiceBroker.builder().transporter(trans).build();<br>
 * //broker.createService(new Service("test") {...});<br>
 * broker.start();<br>
 * <br>
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients<br>
 * compile group: 'org.apache.kafka', name: 'kafka-clients', version: '1.0.0'
 * <br>
 * 
 * @see RedisTransporter
 * @see NatsTransporter
 * @see MqttTransporter
 * @see AmqpTransporter
 * @see GoogleTransporter
 * @see JmsTransporter
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
	
	// --- CONSTUCTORS ---

	public KafkaTransporter() {
	}

	public KafkaTransporter(String... urls) {
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

		// Set unique "node ID" as Kafka "group ID"
		consumerProperties.setProperty("group.id", nodeID);
		
		// Read custom properties from config, eg:
		//
		// consumerProperties {
		// "acks": "all",
		// "retries": 0,
		// "batch-size": 16384,
		// "linger-ms": 1,
		// "buffer-memory": 33554432,
		// "enable-auto-commit": true,
		// "auto-commit-interval-ms": 1000,
		// "session-timeout-ms": 30000
		// }
		//
		copyProperties(config, consumerProperties, "consumerProperties");
		copyProperties(config, producerProperties, "producerProperties");
	}
	
	protected void copyProperties(Tree from, Properties to, String name) {
		Tree props = from.get(name);
		if (props != null) {
			for (Tree prop : props) {
				to.setProperty(prop.getName().replace('-', '.'), prop.asString());
			}
		}		
	}

	// --- CONNECT ---

	/**
	 * Executor of reader loop for incoming messages
	 */
	protected ExecutorService executor;

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

			// Create producer
			ByteArraySerializer byteArraySerializer = new ByteArraySerializer();
			producer = new KafkaProducer<>(producerProperties, byteArraySerializer, byteArraySerializer);

			// Start reader loop
			poller = new KafkaPoller(this);
			executor = Executors.newSingleThreadExecutor();
			executor.execute(poller);

			// Start subscribing channels...
			connected();

		} catch (Exception cause) {
			reconnect(cause);
		}
	}

	// --- DISCONNECT ---

	protected void disconnect() {
		if (poller != null) {
			poller.stop();
		}
		if (executor != null) {
			try {
				executor.shutdown();
			} catch (Exception ignored) {
			}
			executor = null;
		}
		if (poller != null) {
			poller = null;
		}
		if (producer != null) {
			try {
				producer.close(10, TimeUnit.SECONDS);
			} catch (Exception ignored) {
			}
			producer = null;
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
		
		// --- CONSTRUCTOR ---

		protected KafkaPoller(KafkaTransporter transporter) {
			this.transporter = transporter;
			
			// Create consumer
			ByteArrayDeserializer deserializer = new ByteArrayDeserializer();
			consumer = new KafkaConsumer<>(transporter.consumerProperties, deserializer, deserializer);
		}

		// --- SET OF SUBSCRIPTIONS ---

		protected HashSet<String> subscriptions = new HashSet<>();
		
		// --- READER LOOP --

		protected static boolean firstError = true;
		
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
					ConsumerRecords<byte[], byte[]> records = consumer.poll(5000);
					if (records == null || records.isEmpty()) {
						continue;
					}
					for (ConsumerRecord<byte[], byte[]> record : records) {

						// Process incoming records
						transporter.received(record.topic(), record.value());
					}
				}
			} catch (WakeupException wakeUp) {
				
				// Stopping...
				
			} catch (Exception cause) {
				if (firstError) {
					firstError = false;
					transporter.logger.error("Unable to connect to Kafka server!", cause);
				}
				if (cause != null && !(cause instanceof InterruptedException)) {
					
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
	public void stop() {
		
		// Stop timers
		super.stop();
		
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
				if (debug) {
					logger.info("Submitting message to channel \"" + channel + "\":\r\n" + message.toString());
				}
				producer.send(new ProducerRecord<byte[], byte[]>(channel, serializer.write(message)));
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

}
