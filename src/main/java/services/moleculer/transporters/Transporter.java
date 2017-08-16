package services.moleculer.transporters;

import java.util.concurrent.atomic.AtomicBoolean;

import services.moleculer.ServiceBroker;
import services.moleculer.Transit;

public abstract class Transporter {

	// --- CONSTANTS ---

	protected static final String DEFAULT_PREFIX = "MOL";

	// --- VARIABLES ---

	protected final String prefix;

	protected final AtomicBoolean connected = new AtomicBoolean();
	
	protected Transit transit;
	
	protected MessageHandler messageHandler;
	protected AfterConnect afterConnect;
	
	protected ServiceBroker broker;
	protected String nodeID;
	
	// --- CONSTUCTORS ---

	public Transporter() {
		this(DEFAULT_PREFIX);
	}

	public Transporter(String prefix) {
		this.prefix = prefix;
	}

	// --- INIT TRANSPORTER INSTANCE ---

	public void init(Transit transit, MessageHandler messageHandler, AfterConnect afterConnect) {
		if (transit != null) {
			this.transit = transit;
			this.broker = transit.getServiceBroker();
			this.nodeID = transit.getNodeID();
			
			if (this.broker != null) {
				//this.logger = this.broker.getLogger("transporter");
			}
		}
		this.messageHandler = messageHandler;
		this.afterConnect = afterConnect;
	}

	// --- CONNECT ---

	public void connect() {
		throw new UnsupportedOperationException("Not implemented method!");
	}

	public void onConnected(boolean wasReconnect) {
		if (connected.compareAndSet(false, true)) {
			if (afterConnect != null) {
				afterConnect.onConnected(wasReconnect);
			}
		}
	}

	// --- DISCONNECT ---

	public void disconnect() {
		throw new UnsupportedOperationException("Not implemented method!");
	}
	
	public void onDisconnected() {
		connected.set(false);
	}

	// --- SUBSCRIBE ---

	public void subscribe(String cmd, String nodeID) {
		throw new UnsupportedOperationException("Not implemented method!");

	}

	// --- PUBLISH ---

	public void publish(Object packet) {
		throw new UnsupportedOperationException("Not implemented method!");
	}

	// --- GET NODE ID ---

	protected String getTopicName(String cmd, String nodeID) {
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