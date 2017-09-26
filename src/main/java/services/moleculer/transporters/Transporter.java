package services.moleculer.transporters;

import services.moleculer.ServiceBroker;
import services.moleculer.utils.MoleculerComponent;

public abstract class Transporter implements MoleculerComponent {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	@Override
	public String name() {
		return "Transporter";
	}
	
	// --- CONSTANTS ---
	
	public static final String PACKET_UNKNOW		= "???";
	public static final String PACKET_EVENT 		= "EVENT";
	public static final String PACKET_REQUEST 		= "REQ";
	public static final String PACKET_RESPONSE		= "RES";
	public static final String PACKET_DISCOVER 		= "DISCOVER";
	public static final String PACKET_INFO 			= "INFO";
	public static final String PACKET_DISCONNECT 	= "DISCONNECT";
	public static final String PACKET_HEARTBEAT 	= "HEARTBEAT";
	
	// --- PROPERTIES ---

	protected final String prefix;
	protected ServiceBroker broker;
	
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
	 */
	@Override
	public void init(ServiceBroker broker) throws Exception {
	}

	// --- STOP TRANSPORTER ---

	/**
	 * Closes transporter.
	 */
	@Override
	public void close() {
	}
	
	// --- PUBLISH ---

	public abstract void publish(String cmd, String nodeID, Object payload);

	// --- SUBSCRIBE ---

	public abstract void subscribe(String cmd, String nodeID);

	// --- CREATE TOPIC NAME ---

	protected final String nameOf(String cmd, String nodeID) {
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