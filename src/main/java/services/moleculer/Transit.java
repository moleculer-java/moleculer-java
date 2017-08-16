package services.moleculer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.datatree.Tree;
import services.moleculer.transporters.AfterConnect;
import services.moleculer.transporters.MessageHandler;
import services.moleculer.transporters.Transporter;

public class Transit {
	
	protected final AtomicBoolean connected = new AtomicBoolean();
	
	private ServiceBroker broker;
	private Transporter tx;
	//private Logger logger;
	
	public Map<String, Node> nodes = new HashMap<String, Node>();
	
	private MessageHandler messageHandler;
	private AfterConnect afterConnect;
	
	// private Map<String, Object> pendingRequests = new HashMap<String, Object>();
	
	public Transit(ServiceBroker broker, Transporter tx) {
		this.broker = broker;
		this.tx = tx;
		if (this.broker != null) {
			//this.logger = this.broker.getLogger("transit");
		}
		
		this.messageHandler = new MessageHandler() {
			
			@Override
			public void onMessage(Object data) {
				// TODO
				
				/*
				 * 1. Deserialize data
				 * 2. Check sender !== this.nodeID
				 * 3. if PACKET_REQUEST call `this.requestHandler`
				 * 4. if PACKET_RESPONSE call `this.responseHandler`
				 * 5. if PACKET_EVENT call `this.broker.emitLocal`
				 * 6. if PACKET_INFO || PACKET_DISCOVER call `this.processNodeInfo`
				 * 7. if PACKET_DISCONNECT call `this.nodeDisconnected`
				 * 8. if PACKET_HEARTBEAT call `this.nodeHeartbeat`
				 * 8. else throw Invalid packet!
				 */
				
			}
		};
		
		this.afterConnect = new AfterConnect() {
			
			@Override
			public void onConnected(boolean wasReconnect) {
				// TODO
				
				// Call `makeSubscriptions`
				// Call `discoverNodes`
				// Call `sendNodeInfo`
				// Set `this.connected = true`
			}
		};
		
		if (this.tx != null) {
			this.tx.init(this, messageHandler, afterConnect);
		}
	}
	
	/**
	 * Connect with transporter. If failed, try again after 5 sec.
	 */
	public void connect() {
		// Call `this.tx.connect()`
		// If failed, try again after 5 sec.
		// If success
		// 		Start heartbeat timer
		//		Start checkNodes timer 
	}
	
	/**
	 * Disconnect with transporter
	 */
	public void disconnect() {
		// Stop heartbeat timer
		// Stop checkNodes timer
		// Call `sendDisconnectPacket()`
		// Call `this.tx.disconnect()`
	}
	
	/**
	 * Send DISCONNECT to remote nodes
	 */
	private void sendDisconnectPacket() {
		// TODO: return this.publish(new P.PacketDisconnect(this));
	}
	
	/**
	 * Subscribe to topics for transportation
	 */
	private void makeSubscriptions() {
		/* TODO:
			// Subscribe to broadcast events
			this.subscribe(P.PACKET_EVENT),

			// Subscribe to requests
			this.subscribe(P.PACKET_REQUEST, this.nodeID),

			// Subscribe to node responses of requests
			this.subscribe(P.PACKET_RESPONSE, this.nodeID),

			// Discover handler
			this.subscribe(P.PACKET_DISCOVER),

			// NodeInfo handler
			this.subscribe(P.PACKET_INFO), // Broadcasted INFO. If a new node connected
			this.subscribe(P.PACKET_INFO, this.nodeID), // Response INFO to DISCOVER packet

			// Disconnect handler
			this.subscribe(P.PACKET_DISCONNECT),

			// Heart-beat handler
			this.subscribe(P.PACKET_HEARTBEAT),		 
		 */
	}
	
	/**
	 * Emit an event to remote nodes
	 * 
	 * @param eventName
	 * @param data
	 */
	protected void emit(String eventName, Object data) {
		// TODO: this.publish(new P.PacketEvent(this, eventName, data));
	}
	
	/**
	 * Send a request to a remote service. It returns a Promise
	 * what will be resolved when the response received.
	 * 
	 * @param ctx
	 */
	protected void request(Context ctx) {
		// TODO: Send this request to the target node (ctx.nodeID) in a PACKET_REQUEST
	}
	
	private void requestHandler(Object payload) {
		// TODO: process the incoming request. Call broker.call with this `context`. With result call the `sendResponse`
	}
	
	private void responseHandler(Object packet) {
		// TODO: process the incoming request.
	}
	
	/**
	 * Send back the response of request
	 * 
	 * @param nodeID
	 * @param id
	 * @param data
	 * @param err
	 */
	private void sendResponse(String nodeID, String id, Object data, Throwable err) {
		// TODO: return this.publish(new P.PacketResponse(this, nodeID, id, data, err));
	}
	
	/**
	 * Get Node information to DISCOVER & INFO packages
	 */
	public Tree getNodeInfo() {
		return null;
	}
	
	/**
	 * Discover other nodes. It will be called after success connect.
	 */
	private void discoverNodes() {
		// TODO: return this.publish(new P.PacketDiscover(this));
	}
	
	/**
	 * Send node info package to other nodes. It will be called with timer
	 * 
	 * @param nodeID
	 */
	private void sendNodeInfo(String nodeID) {
		/* TODO:
		 * const info = this.getNodeInfo();
		 * return this.publish(new P.PacketInfo(this, nodeID, info));
		 */
	}
	
	/**
	 * Send a node heart-beat. It will be called with timer
	 */
	private void sendHeartbeat() {
		/* TODO:
		 * 
		 * const uptime = process.uptime();
		 * return this.publish(new P.PacketHeartbeat(this, uptime));
		 */
	}
	
	/**
	 * Process remote node info (list of actions)
	 * 
	 * @param nodeID
	 * @param payload
	 */
	private void processNodeInfo(String nodeID, Tree payload) {
		
	}

	/**
	 * Check the given nodeID is available
	 * 
	 * @param nodeID
	 */
	public boolean isNodeAvailable(String nodeID) {
		Node node = this.nodes.get(nodeID);
		if (node != null)
			return node.available;
		
		return false;		
	}	
	
	/**
	 * Save a heart-beat time from a remote node
	 */
	private void nodeHeartbeat(String nodeID, Tree payload) {
		// Update `lastHeartbeatTime`, `uptime` and `available` in `nodes`. 
	}	
	
	/**
	 * Node disconnected event handler. 
	 * Remove node and remove remote actions of node
	 * 
	 * @param nodeID
	 * @param isUnexpected
	 */
	private void nodeDisconnected(String nodeID, boolean isUnexpected) {
		// Set `available = false` in `nodes`
	}	
	
	/**
	 * Check all registered remote nodes is live.
	 */
	private void checkRemoteNodes() {
		// Foreach nodes, if node.lastHeartbeattime > this.broker.options.heartbeatTimeout call this.nodeDisconnected
	}	
	
	public ServiceBroker getServiceBroker() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getNodeID() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object serialize(Object data, String type) {
		// TODO 
		return null;
	}
	
	public Packet deserialize(Object data, String type) {
		// TODO 
		return null;
	}	
}
