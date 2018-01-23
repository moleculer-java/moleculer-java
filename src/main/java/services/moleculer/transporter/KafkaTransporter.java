package services.moleculer.transporter;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
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
 * Transporter t = new KafkaTransporter();<br>
 * ServiceBroker broker = ServiceBroker.builder().transporter(t).build();<br>
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
 * @see SocketClusterTransporter
 * @see GoogleTransporter
 * @see JmsTransporter
 */
@Name("Kafka Transporter")
public class KafkaTransporter extends Transporter {

	// --- PROPERTIES ---

	protected Properties properties = new Properties();
	protected String groupID;
	protected String[] urls = new String[] { "127.0.0.1:9092" };

	// --- KAFKA CLIENTS ---

	protected KafkaProducer<byte[], byte[]> producer;
	protected KafkaConsumer<byte[], byte[]> consumer;

	// --- CONSTUCTORS ---

	public KafkaTransporter() {
		super();
	}

	public KafkaTransporter(String prefix) {
		super(prefix);
	}

	public KafkaTransporter(String prefix, String... urls) {
		super(prefix);
		this.urls = urls;
	}

	public KafkaTransporter(String prefix, String groupID, Properties properties, String... urls) {
		super(prefix);
		this.groupID = groupID;
		this.properties.putAll(properties);
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
		Tree urlNode = config.get("url");
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
		
		// Read group ID
		groupID = config.get("groupID", groupID);
		if (groupID == null) {
			groupID = prefix;
		}
		properties.put("group.id", groupID);

		// Read custom properties from config, eg:
		//
		// properties {
		// "acks": "all",
		// "retries": 0,
		// "batch.size": 16384,
		// "linger.ms": 1,
		// "buffer.memory": 33554432,
		// "enable.auto.commit": true,
		// "auto.commit.interval.ms": 1000,
		// "session.timeout.ms": 30000		
		// }
		//
		Tree props = config.get("properties");
		if (props != null) {
			for (Tree prop: props) {
				properties.setProperty(prop.getName(), prop.asString());
			}
		}

		// Set key serializer / deserializer
		properties.put("key.serializer", ByteArraySerializer.class.getName());
		properties.put("key.deserializer", ByteArrayDeserializer.class.getName());

		// Set value serializer / deserializer
		properties.put("value.serializer", ByteArraySerializer.class.getName());
		properties.put("value.deserializer", ByteArrayDeserializer.class.getName());
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
			properties.put("bootstrap.servers", urlList);

			// Create producer and consumer
			producer = new KafkaProducer<>(properties);
			consumer = new KafkaConsumer<>(properties);

			// Start reader loop
			executor = Executors.newSingleThreadExecutor();
			executor.execute(this::readIncomingMessages);

			// Start subscribing channels...
			connected();

		} catch (Exception cause) {
			reconnect(cause);
		}
	}

	// --- DISCONNECT ---

	protected void disconnect() {
		subscriptions.clear();
		if (executor != null) {
			try {
				executor.shutdownNow();
			} catch (Exception ignored) {
			}
			executor = null;
		}
		if (consumer != null) {
			try {
				consumer.close(10, TimeUnit.SECONDS);
			} catch (Exception ignored) {
			}
			consumer = null;
		}
		if (producer != null) {
			try {
				producer.close(10, TimeUnit.SECONDS);
			} catch (Exception ignored) {
			}
			producer = null;
		}
	}

	// --- MESSAGE READER LOOP ---

	protected void readIncomingMessages() {
		while (true) {
			try {
				if (consumer == null) {
					return;
				}
				
				// Try to read incoming records
				ConsumerRecords<byte[], byte[]> records = consumer.poll(1000);
				if (records == null || records.isEmpty()) {
					continue;
				}
				for (ConsumerRecord<byte[], byte[]> record : records) {
					
					// Process incoming records
					received(record.topic(), record.value());
				}
			} catch (IllegalStateException closed) {
				return;
			} catch (Exception cause) {
				if (cause != null && !(cause instanceof InterruptedException)) {
					error(cause);
				}
				return;
			}
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
		disconnect();
	}

	// --- SUBSCRIBE ---

	protected HashSet<String> subscriptions = new HashSet<>();

	@Override
	public Promise subscribe(String channel) {
		if (consumer != null) {
			subscriptions.add(channel);
			try {
				consumer.subscribe(subscriptions);
			} catch (Exception cause) {
				return Promise.reject(cause);
			}
		}
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

	// --- SET CLIENT PROPERTY ---

	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

	// --- GETTERS / SETTERS ---

	public String[] getUrls() {
		return urls;
	}

	public void setUrls(String[] urls) {
		this.urls = urls;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public String getGroupID() {
		return groupID;
	}

	public void setGroupID(String groupID) {
		this.groupID = groupID;
	}

}
