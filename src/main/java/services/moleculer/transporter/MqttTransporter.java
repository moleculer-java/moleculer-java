package services.moleculer.transporter;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.ScheduledExecutorPingSender;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

@Name("MQTT Transporter")
public final class MqttTransporter extends Transporter implements MqttCallback, IMqttActionListener {

	// --- PROPERTIES ---

	private String username;
	private String password;
	private String[] urls = new String[] { "127.0.0.1" };

	// --- OTHER MQTT PROPERTIES ---

	private boolean cleanSession = MqttConnectOptions.CLEAN_SESSION_DEFAULT;
	private int connectionTimeout = MqttConnectOptions.CONNECTION_TIMEOUT_DEFAULT;
	private int keepAliveInterval = MqttConnectOptions.KEEP_ALIVE_INTERVAL_DEFAULT;
	private int maxInflight = MqttConnectOptions.MAX_INFLIGHT_DEFAULT;
	private int mqttVersion = MqttConnectOptions.MQTT_VERSION_DEFAULT;
	private SocketFactory socketFactory;
	private HostnameVerifier hostnameVerifier;
	private Properties sslProperties;

	// --- MQTT CONNECTION ---

	private MqttAsyncClient client;

	// --- CONSTUCTORS ---

	public MqttTransporter() {
		super();
	}

	public MqttTransporter(String prefix) {
		super(prefix);
	}

	public MqttTransporter(String prefix, String... urls) {
		super(prefix);
		this.urls = urls;
	}

	public MqttTransporter(String prefix, String username, String password, String... urls) {
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
		cleanSession = config.get("cleanSession", cleanSession);
		connectionTimeout = config.get("connectionTimeout", connectionTimeout);
		keepAliveInterval = config.get("keepAliveInterval", keepAliveInterval);
		maxInflight = config.get("maxInflight", maxInflight);
		mqttVersion = config.get("mqttVersion", mqttVersion);

		// Connect to MQTT server
		connect();
	}

	// --- CONNECT ---

	private final void connect() {
		try {

			// Create MQTT client options
			MqttConnectOptions options = new MqttConnectOptions();
			if (password != null) {
				options.setPassword(this.password.toCharArray());
			}
			if (username != null) {
				options.setUserName(this.username);
			}
			String[] array = new String[urls.length];
			for (int i = 0; i < urls.length; i++) {
				String url = urls[i];
				if (url.indexOf(':') == -1) {
					url = url + ":1883";
				}

				url = url.replace("mqtt://", "tcp://");

				if (!url.startsWith("tcp://")) {
					url = "tcp://" + url;
				}
				array[i] = url;
			}
			String url = array[0];
			options.setServerURIs(array);
			options.setAutomaticReconnect(false);

			options.setCleanSession(cleanSession);
			options.setConnectionTimeout(connectionTimeout);
			options.setKeepAliveInterval(keepAliveInterval);
			options.setMaxInflight(maxInflight);
			options.setMqttVersion(mqttVersion);
			options.setSocketFactory(socketFactory);
			options.setSSLHostnameVerifier(hostnameVerifier);
			options.setSSLProperties(sslProperties);

			// Create MQTT client
			disconnect();
			client = new MqttAsyncClient(url, nodeID + '-' + broker.components().uid().nextUID(),
					new MemoryPersistence(), new ScheduledExecutorPingSender(scheduler));
			client.setCallback(this);
			client.connect(options, this);

		} catch (Exception cause) {
			String msg = cause.getMessage();
			if (msg == null || msg.isEmpty()) {
				msg = "Unable to connect to MQTT server!";
			} else if (!msg.endsWith("!") && !msg.endsWith(".")) {
				msg += "!";
			}
			logger.warn(msg);
			reconnect();
		}
	}

	@Override
	public final void onSuccess(IMqttToken asyncActionToken) {
		logger.info("MQTT pub-sub client is estabilished.");
		connected();
	}

	// --- DISCONNECT ---

	@Override
	public final void connectionLost(Throwable cause) {
		logger.info("MQTT pub-sub client disconnected.");
		reconnect();
	}

	private final void disconnect() {
		if (client != null) {
			try {
				client.close();
			} catch (Throwable cause) {
				logger.warn("Unexpected error occured while closing MQTT client!", cause);
			} finally {
				client = null;
			}
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
	public final void onFailure(IMqttToken asyncActionToken, Throwable exception) {

		// Do nothing
		exception.printStackTrace();
	}

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
		if (client != null) {
			try {
				client.subscribe(channel, 0);
			} catch (Exception cause) {
				return Promise.reject(cause);
			}
		}
		return Promise.resolve();
	}

	// --- MESSAGE RECEIVED ---

	@Override
	public final void messageArrived(String topic, MqttMessage message) throws Exception {
		received(topic, message.getPayload());
	}

	// --- PUBLISH ---

	@Override
	public final void publish(String channel, Tree message) {
		if (client != null) {
			try {
				if (debug) {
					logger.info("Submitting message to channel \"" + channel + "\":\r\n" + message.toString());
				}
				client.publish(channel, serializer.write(message), 0, false);
			} catch (Exception cause) {
				logger.warn("Unable to send message to MQTT server!", cause);
				reconnect();
			}
		}
	}

	@Override
	public final void deliveryComplete(IMqttDeliveryToken token) {
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

	public final boolean isCleanSession() {
		return cleanSession;
	}

	public final void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public final int getConnectionTimeout() {
		return connectionTimeout;
	}

	public final void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public final int getKeepAliveInterval() {
		return keepAliveInterval;
	}

	public final void setKeepAliveInterval(int keepAliveInterval) {
		this.keepAliveInterval = keepAliveInterval;
	}

	public final int getMaxInflight() {
		return maxInflight;
	}

	public final void setMaxInflight(int maxInflight) {
		this.maxInflight = maxInflight;
	}

	public final int getMqttVersion() {
		return mqttVersion;
	}

	public final void setMqttVersion(int mqttVersion) {
		this.mqttVersion = mqttVersion;
	}

	public final SocketFactory getSocketFactory() {
		return socketFactory;
	}

	public final void setSocketFactory(SocketFactory socketFactory) {
		this.socketFactory = socketFactory;
	}

	public final HostnameVerifier getHostnameVerifier() {
		return hostnameVerifier;
	}

	public final void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
		this.hostnameVerifier = hostnameVerifier;
	}

	public final Properties getSslProperties() {
		return sslProperties;
	}

	public final void setSslProperties(Properties sslProperties) {
		this.sslProperties = sslProperties;
	}

}