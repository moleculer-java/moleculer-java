package services.moleculer.transporters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.services.Name;

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

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- PROPERTIES ---

	protected final String prefix;

	protected ServiceBroker broker;
	protected String nodeID;
	protected String format;

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
		this.broker = broker;
		this.nodeID = broker.nodeID();
	}

	// --- SERVER CONNECTED ---

	protected void connected() throws Exception {

		// Subscribe to broadcast events
		subscribe(PACKET_EVENT, null);

		// Subscribe to requests
		subscribe(PACKET_REQUEST, nodeID);

		// Subscribe to node responses of requests
		subscribe(PACKET_RESPONSE, nodeID);

		// Discover handler
		subscribe(PACKET_DISCOVER, null);

		// Broadcasted INFO (if a new node connected)
		subscribe(PACKET_INFO, null);

		// Response INFO to DISCOVER packet
		subscribe(PACKET_INFO, nodeID);

		// Disconnect handler
		subscribe(PACKET_DISCONNECT, null);

		// Heart-beat handler
		subscribe(PACKET_HEARTBEAT, null);

		// TODO
		// - Start heartbeat timer
		// - Start checkNodes timer
	}

	// --- SERVER DISCONNECTED ---

	protected void disconnected() throws Exception {

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

	public abstract void publish(String cmd, String nodeID, Tree message);

	// --- SUBSCRIBE ---

	public abstract void subscribe(String cmd, String nodeID);

	// --- IS CONNECTED ---

	public abstract boolean isConnected();

	// --- CREATE TOPIC NAME ---

	protected final String nameOfChannel(String cmd, String nodeID) {
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

}