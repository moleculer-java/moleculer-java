package services.moleculer.transporters;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.services.Name;
import services.moleculer.services.ServiceRegistry;
import services.moleculer.utils.Serializer;

@Name("Transporter")
public abstract class Transporter implements MoleculerComponent {

	// --- CONSTANTS ---

	public static final String PACKET_EVENT = "EVENT";
	public static final String PACKET_REQUEST = "REQ";
	public static final String PACKET_RESPONSE = "RES";
	public static final String PACKET_DISCOVER = "DISCOVER";
	public static final String PACKET_INFO = "INFO";
	public static final String PACKET_DISCONNECT = "DISCONNECT";
	public static final String PACKET_HEARTBEAT = "HEARTBEAT";
	public static final String PACKET_PING = "PING";
	public static final String PACKET_PONG = "PONG";
		
	// --- CHANNELS ---
	
	public String eventChannel;
	public String requestChannel;
	public String responseChannel;
	public String discoverBroadcastChannel;
	public String discoverChannel;
	public String infoBroadcastChannel;
	public String infoChannel;
	public String disconnectChannel;
	public String heartbeatChannel;
	public String pingBroadcastChannel;
	public String pingChannel;
	public String pongChannel;
	
	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- PROPERTIES ---

	protected String prefix;
	protected ServiceBroker broker;
	protected String nodeID;
	protected String format;

	// --- COMPONENTS ---

	protected Executor executor;
	protected ServiceRegistry serviceRegistry;

	// --- CONSTUCTORS ---

	public Transporter() {
		this("MOL");
	}

	public Transporter(String prefix) {
		this.prefix = prefix;
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

		// Process config
		prefix = config.get("prefix", prefix);

		// Get components
		executor = broker.components().executor();
		serviceRegistry = broker.components().serviceRegistry();

		// Get properties from broker
		this.broker = broker;
		this.nodeID = broker.nodeID();
		
		// Set channel names
		eventChannel = channel(PACKET_EVENT, nodeID);
		requestChannel = channel(PACKET_REQUEST, nodeID);
		responseChannel = channel(PACKET_RESPONSE, nodeID);
		discoverBroadcastChannel = channel(PACKET_DISCOVER, null);
		discoverChannel = channel(PACKET_DISCOVER, nodeID);
		infoBroadcastChannel = channel(PACKET_INFO, null);
		infoChannel = channel(PACKET_INFO, nodeID);
		disconnectChannel = channel(PACKET_DISCONNECT, null);
		heartbeatChannel = channel(PACKET_HEARTBEAT, null);
		pingBroadcastChannel = channel(PACKET_PING, null);
		pingChannel = channel(PACKET_PING, nodeID);
		pongChannel = channel(PACKET_PONG, nodeID);
	}

	protected String channel(String cmd, String nodeID) {
		StringBuilder name = new StringBuilder(64);
		name.append(prefix);
		name.append('.');
		name.append(cmd);
		if (nodeID != null && !nodeID.isEmpty()) {
			name.append('.');
			name.append(nodeID);
		}
		return name.toString();
	}
	
	// --- SERVER CONNECTED ---

	protected void connected() {
		executor.execute(() -> {

			// Subscribe channels
			subscribe(eventChannel);
			subscribe(requestChannel);
			subscribe(responseChannel);
			subscribe(discoverBroadcastChannel);
			subscribe(discoverChannel);
			subscribe(infoBroadcastChannel);
			subscribe(infoChannel);
			subscribe(disconnectChannel);
			subscribe(heartbeatChannel);
			subscribe(pingBroadcastChannel);
			subscribe(pingChannel);
			subscribe(pongChannel);

		});

		// TODO
		// - Start heartbeat timer
		// - Start checkNodes timer
	}

	// --- SERVER DISCONNECTED ---

	protected void disconnected() {

		// TODO on disconnected (move to superclass):
		// Stop heartbeat timer
		// Stop checkNodes timer
		// Call `this.tx.disconnect()`
	}

	// --- STOP TRANSPORTER ---

	/**
	 * Closes transporter.
	 */
	@Override
	public void stop() {
		// If isConnected() call `sendDisconnectPacket()`
	}

	// --- PUBLISH ---

	public void publish(String cmd, String nodeID, Tree message) {
		publish(channel(cmd, nodeID), message);
	}
	
	public abstract void publish(String channel, Tree message);

	// --- SUBSCRIBE ---

	public void subscribe(String cmd, String nodeID) {
		subscribe(channel(cmd, nodeID));
	}
	
	public abstract void subscribe(String channel);

	// --- PROCESS INCOMING MESSAGE ---

	protected void received(String channel, byte[] message, Object connectionID) {
		executor.execute(() -> {
			
			// Parse message
			Tree data;
			try {
				data = Serializer.deserialize(message, format);
			} catch (Exception cause) {
				logger.warn("Unable to parse incoming message!", cause);
				if (connectionID != null) {
					failed(connectionID);
				}
				return;
			}
			
			// Send message to proper component
			try {
				System.out.println(channel + " -> " + data);
				
				// Messages of ServiceRegistry
				if (channel.equals(eventChannel) || channel.equals(requestChannel) || channel.equals(responseChannel)) {
					serviceRegistry.receive(data);
					return;
				}

			} catch (Exception cause) {
				logger.warn("Unable to process incoming message!", cause);
			}
		});
	}

	// --- SUBSCRIPTION FINISHED ---

	protected void subscribed(String channel) {
		executor.execute(() -> {
			try {
				logger.info(channel + " channel subscribed.");
				
				// Send INFO to all nodes
				if (channel.equals(discoverBroadcastChannel)) {
					Tree message = new Tree();
					message.put("ver", "2");
					message.put("sender", nodeID);
					publish(discoverBroadcastChannel, message);
				}
				
			} catch (Exception cause) {
				logger.warn("Unable to process subscription!", cause);
			}
		});
	}

	// --- OPTIONAL DISCONNECTION ON ERROR ---

	protected void failed(Object connectionID) {
	}

}