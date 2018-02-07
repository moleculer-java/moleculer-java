/**
 * MOLECULER MICROSERVICES FRAMEWORK<br>
 * <br>
 * This project is based on the idea of Moleculer Microservices
 * Framework for NodeJS (https://moleculer.services). Special thanks to
 * the Moleculer's project owner (https://github.com/icebob) for the
 * consultations.<br>
 * <br>
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
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

import static services.moleculer.ServiceBroker.PROTOCOL_VERSION;
import static services.moleculer.util.CommonUtils.nameOf;
import static services.moleculer.util.CommonUtils.serializerTypeToClass;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.context.Context;
import services.moleculer.eventbus.EventBus;
import services.moleculer.monitor.Monitor;
import services.moleculer.serializer.JsonSerializer;
import services.moleculer.serializer.Serializer;
import services.moleculer.service.Name;
import services.moleculer.service.NodeDescriptor;
import services.moleculer.service.ServiceRegistry;

/**
 * Base superclass of all Transporter implementations.
 *
 * @see RedisTransporter
 * @see NatsTransporter
 * @see MqttTransporter
 * @see AmqpTransporter
 * @see JmsTransporter
 * @see GoogleTransporter
 */
@Name("Transporter")
public abstract class Transporter implements MoleculerComponent {

	// --- CHANNEL NAMES / PACKET TYPES ---

	public static final String PACKET_EVENT = "EVENT";
	public static final String PACKET_REQUEST = "REQ";
	public static final String PACKET_RESPONSE = "RES";
	public static final String PACKET_DISCOVER = "DISCOVER";
	public static final String PACKET_INFO = "INFO";
	public static final String PACKET_DISCONNECT = "DISCONNECT";
	public static final String PACKET_HEARTBEAT = "HEARTBEAT";
	public static final String PACKET_PING = "PING";
	public static final String PACKET_PONG = "PONG";

	// --- CHANNELS OF CURRENT NODE ---

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

	protected String namespace = "";
	protected String prefix = "MOL";
	protected ServiceBroker broker;
	protected String nodeID;

	protected int heartbeatInterval = 5;
	protected int heartbeatTimeout = 30;
	protected int offlineTimeout = 180;

	/**
	 * Use hostnames instead of IP addresses As the DHCP environment is dynamic,
	 * any later attempt to use IPs instead hostnames would most likely yield
	 * false results. Therefore, use hostnames if you are using DHCP.
	 */
	protected boolean preferHostname = true;

	// --- DEBUG COMMUNICATION ---

	protected boolean debug;

	// --- SERIALIZER / DESERIALIZER ---

	protected Serializer serializer;

	// --- COMPONENTS ---

	protected ExecutorService executor;
	protected ScheduledExecutorService scheduler;
	protected ServiceRegistry registry;
	protected EventBus eventbus;
	protected Monitor monitor;

	// --- REMOTE NODES ---

	protected final ConcurrentHashMap<String, NodeDescriptor> nodes = new ConcurrentHashMap<>(256);

	// --- CONSTUCTORS ---

	public Transporter() {
		this(null);
	}

	public Transporter(Serializer serializer) {
		this.serializer = serializer;
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
		namespace = config.get("namespace", "");
		if (namespace != null && !namespace.isEmpty()) {
			prefix = prefix + '-' + namespace;
		}

		// Create serializer
		Tree serializerNode = config.get("serializer");
		if (serializerNode != null) {
			String type;
			if (serializerNode.isPrimitive()) {
				type = serializerNode.asString();
			} else {
				type = serializerNode.get("type", "json");
			}

			@SuppressWarnings("unchecked")
			Class<? extends Serializer> c = (Class<? extends Serializer>) Class.forName(serializerTypeToClass(type));
			serializer = c.newInstance();
		} else {
			serializerNode = config.putMap("serializer");
		}
		if (serializer == null) {
			serializer = new JsonSerializer();
		}

		// Heartbeat interval
		heartbeatInterval = config.get("heartbeatInterval", heartbeatInterval);
		if (heartbeatInterval > 0) {
			logger.info(nameOf(this, true) + " sends heartbeat signal every " + heartbeatInterval + " seconds.");
		}

		// Heartbeat timeout
		heartbeatTimeout = config.get("heartbeatTimeout", heartbeatTimeout);
		if (heartbeatTimeout > 0 || heartbeatInterval > 0) {
			if (heartbeatTimeout < 1) {
				heartbeatTimeout = Math.max(30, heartbeatInterval * 2);
			}
			logger.info("Heartbeat timeout of " + nameOf(this, true) + " is " + heartbeatTimeout + " seconds.");
		}

		// Offline timeout
		offlineTimeout = config.get("offlineTimeout", offlineTimeout);
		if (offlineTimeout > 0) {
			if (offlineTimeout < heartbeatTimeout) {
				offlineTimeout = Math.max(heartbeatTimeout * 2, 180);
			}
			logger.info("Configuration timeout of offline nodes is " + offlineTimeout + " seconds.");
		}

		// Debug mode
		debug = config.get("debug", debug);

		// Start serializer
		logger.info(nameOf(this, true) + " will use " + nameOf(serializer, true) + '.');
		serializer.start(broker, serializerNode);

		// Prefers hostname instead of IP
		preferHostname = broker.components().registry().isPreferHostname();

		// Get components
		executor = broker.components().executor();
		scheduler = broker.components().scheduler();
		registry = broker.components().registry();
		monitor = broker.components().monitor();
		eventbus = broker.components().eventbus();

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
		if (prefix != null && !prefix.isEmpty()) {
			name.append(prefix);
			name.append('.');
		}
		name.append(cmd);
		if (nodeID != null && !nodeID.isEmpty()) {
			name.append('.');
			name.append(nodeID);
		}
		return name.toString();
	}

	public abstract void connect();

	// --- SERVER CONNECTED ---

	/**
	 * Cancelable "Heart Beat" timer
	 */
	protected volatile ScheduledFuture<?> heartBeatTimer;

	/**
	 * Cancelable "Check Activities / Infos" timer
	 */
	protected volatile ScheduledFuture<?> checkTimeoutTimer;

	protected void connected() {
		executor.execute(() -> {

			// Subscribe channels
			Promise.all( // Waiting for all subscriptions...
					subscribe(eventChannel), // EVENT
					subscribe(requestChannel), // REQ
					subscribe(responseChannel), // RES
					subscribe(discoverBroadcastChannel), // DISCOVER
					subscribe(discoverChannel), // DISCOVER
					subscribe(infoBroadcastChannel), // INFO
					subscribe(infoChannel), // INFO
					subscribe(disconnectChannel), // DISCONNECT
					subscribe(heartbeatChannel), // HEARTBEAT
					subscribe(pingBroadcastChannel), // PING
					subscribe(pingChannel), // PING
					subscribe(pongChannel) // PONG
			).then(in -> {

				// Redis transporter is ready for use
				logger.info("All channels subscribed.");

				// Do the discovery process
				sendDiscoverPacket(discoverBroadcastChannel);
				sendInfoPacket(infoBroadcastChannel);

				// Start sendHeartbeat timer
				if (heartBeatTimer == null && heartbeatInterval > 0) {
					heartBeatTimer = scheduler.scheduleAtFixedRate(this::sendHeartbeatPacket, heartbeatInterval,
							heartbeatInterval, TimeUnit.SECONDS);
				}

				// Start timeout checker's timer
				if (checkTimeoutTimer == null && (heartbeatTimeout > 0 || offlineTimeout > 0)) {
					checkTimeoutTimer = scheduler.scheduleAtFixedRate(this::checkTimeouts, heartbeatTimeout,
							heartbeatTimeout, TimeUnit.SECONDS);
				}

			}).Catch(error -> {

				logger.warn("Unable to subscribe channels!", error);
				error(error);

			});
		});
	}

	// --- STOP TRANSPORTER ---

	/**
	 * Closes transporter.
	 */
	@Override
	public void stop() {

		// Send "disconnected" packet
		sendDisconnectPacket();

		// Stop heartbeat timer
		if (heartBeatTimer != null) {
			heartBeatTimer.cancel(false);
			heartBeatTimer = null;
		}

		// Stop timeout checker's timer
		if (checkTimeoutTimer != null) {
			checkTimeoutTimer.cancel(false);
			checkTimeoutTimer = null;
		}

		// Clear all stored data
		nodes.clear();
	}

	// --- REQUEST PACKET ---

	public Tree createRequestPacket(Context ctx) {

		// TODO Add more properties
		Tree message = new Tree();
		message.put("ver", ServiceBroker.PROTOCOL_VERSION);
		message.put("sender", nodeID);
		message.put("id", ctx.id);
		message.put("action", ctx.name);

		message.putObject("params", ctx.params);
		message.put("meta", (String) null);

		if (ctx.opts != null) {
			message.put("socketTimeout", ctx.opts.timeout);
		}

		message.put("level", 1);
		message.put("metrics", false);
		message.put("parentID", (String) null);
		message.put("requestID", (String) null);
		return message;
	}

	// --- PUBLISH ---

	public void publish(String cmd, String nodeID, Tree message) {
		publish(channel(cmd, nodeID), message);
	}

	public abstract void publish(String channel, Tree message);

	// --- SUBSCRIBE ---

	public Promise subscribe(String cmd, String nodeID) {
		return subscribe(channel(cmd, nodeID));
	}

	public abstract Promise subscribe(String channel);

	// --- PROCESS INCOMING MESSAGE ---

	protected void received(String channel, byte[] message) {
		executor.execute(() -> {

			// Parse message
			Tree data;
			try {
				data = serializer.read(message);
			} catch (Exception cause) {
				logger.warn("Unable to parse incoming message!", cause);
				return;
			}

			// Debug
			if (debug) {
				logger.info("Message received from channel \"" + channel + "\":\r\n" + data);
			}

			// Send message to proper component
			try {

				// Get "sender" property
				String sender = data.get("sender", "");
				if (sender == null || sender.isEmpty()) {
					logger.warn("Missing \"sender\" property:\r\n" + data);
					return;
				}
				if (sender.equals(nodeID)) {

					// It's our message
					return;
				}

				// Incoming response
				if (channel.equals(responseChannel)) {
					registry.receiveResponse(data);
					return;
				}

				// Incoming event
				if (channel.equals(eventChannel)) {
					eventbus.receiveEvent(data);
					return;
				}

				// Incoming request
				if (channel.equals(requestChannel)) {
					registry.receiveRequest(data);
					return;
				}

				// HeartBeat packet
				if (channel.endsWith(heartbeatChannel)) {

					// Get node container
					NodeDescriptor node = nodes.get(sender);
					if (node == null) {

						// Unknown node -> send discover packet
						sendDiscoverPacket(channel(PACKET_DISCOVER, sender));
						return;
					}

					// Update CPU info
					node.setCpuUsage(data.get("cpu", 0));
					return;
				}

				// Info packet
				if (channel.equals(infoChannel) || channel.equals(infoBroadcastChannel)) {

					// Register services and listeners
					updateNodeInfo(sender, data);
					return;
				}

				// Discover packet
				if (channel.equals(discoverChannel) || channel.equals(discoverBroadcastChannel)) {

					// Send node desriptor to the sender
					sendInfoPacket(channel(PACKET_INFO, sender));
				}

				// Disconnect packet
				if (channel.equals(disconnectChannel)) {

					// Get node container
					synchronized (this) {
						NodeDescriptor node = nodes.get(sender);
						if (node != null && node.switchToOffline()) {

							// Remove remote actions and listeners
							registry.removeActions(sender);
							eventbus.removeListeners(sender);

							// Notify listeners (not unexpected disconnection)
							logger.info("Node \"" + sender + "\" disconnected.");
							broadcastNodeDisconnected(node.info, false);
						}
					}
				}

			} catch (Exception cause) {
				logger.warn("Unable to process incoming message!", cause);
			}
		});
	}

	protected synchronized void updateNodeInfo(String sender, Tree info) throws Exception {

		boolean connected = false;
		boolean reconnected = false;
		boolean updated = false;

		NodeDescriptor node = nodes.get(sender);
		if (node == null || node.getSequence() == 0) {

			// New, unknown node
			connected = true;
			node = new NodeDescriptor(info, preferHostname, false);
			nodes.put(sender, node);

		} else {

			// Try to update current node
			NodeDescriptor newNode = node.switchToOnline(info);
			if (newNode == null) {

				// New info block is older
				return;
			} else {

				// Store new node info
				nodes.put(sender, newNode);
				if (updated) {
					Tree s1 = newNode.info.get("services");
					if (s1 != null) {
						Tree s2 = node.info.get("services");
						if (s2 != null && s1.equals(s2)) {

							// Service blocks are equal
							return;
						}
					}
				}
				if (node.isOffline()) {
					reconnected = true;
				} else {
					updated = true;
				}
				node = newNode;
			}
		}

		// Register actions and listeners
		if (connected || reconnected || updated) {
			if (updated) {

				// Remove actions and listeners
				registry.removeActions(sender);
				eventbus.removeListeners(sender);

			}
			Tree services = info.get("services");
			if (services != null && services.size() > 0) {
				for (Tree service : services) {

					// Register actions and listeners
					registry.addActions(service);
					eventbus.addListeners(service);
				}
			}
		}

		// Notify local listeners
		if (updated) {

			// Node updated
			logger.info("Node \"" + sender + "\" updated.");
			broadcastNodeUpdated(info);

		} else if (connected || reconnected) {

			// Node connected or reconnected
			if (connected) {
				logger.info("Node \"" + sender + "\" connected.");
			} else {
				logger.info("Node \"" + sender + "\" reconnected.");
			}
			broadcastNodeConnected(info, reconnected);
		}
	}

	protected void broadcastNodeConnected(Tree info, boolean reconnected) {
		if (info != null) {
			Tree message = new Tree();
			message.putObject("node", info);
			message.put("reconnected", reconnected);
			eventbus.broadcast("$node.connected", message, null, true);
		}
	}

	protected void broadcastNodeUpdated(Tree info) {
		if (info != null) {
			Tree message = new Tree();
			message.putObject("node", info);
			eventbus.broadcast("$node.updated", message, null, true);
		}
	}

	protected void broadcastNodeDisconnected(Tree info, boolean unexpected) {
		if (info != null) {
			Tree message = new Tree();
			message.putObject("node", info);
			message.put("unexpected", unexpected);
			eventbus.broadcast("$node.disconnected", message, null, true);
		}
	}

	// --- GENERIC MOLECULER PACKETS ---

	protected void sendDiscoverPacket(String channel) {
		Tree message = new Tree();
		message.put("ver", PROTOCOL_VERSION);
		message.put("sender", nodeID);
		publish(channel, message);
	}

	protected void sendInfoPacket(String channel) {
		publish(channel, registry.getDescriptor().info);
	}

	protected void sendHeartbeatPacket() {
		int cpu = monitor.getTotalCpuPercent();
		registry.getDescriptor().setCpuUsage(cpu);

		Tree message = new Tree();
		message.put("ver", PROTOCOL_VERSION);
		message.put("sender", nodeID);
		message.put("cpu", cpu);
		publish(heartbeatChannel, message);
	}

	protected void sendDisconnectPacket() {
		Tree message = new Tree();
		message.put("sender", nodeID);
		publish(disconnectChannel, message);
	}

	// --- TIMEOUT PROCESS ---

	protected synchronized void checkTimeouts() {

		// Check heartbeat timeout
		long now = System.currentTimeMillis();
		long offlineTimeoutMillis = offlineTimeout * 1000L;
		long heartbeatTimeoutMillis = heartbeatTimeout * 1000L;
		NodeDescriptor node;

		Iterator<NodeDescriptor> i = nodes.values().iterator();
		while (i.hasNext()) {
			node = i.next();
			if (now - node.getOfflineSince(Long.MAX_VALUE) > offlineTimeoutMillis) {

				// Remove node from Map
				i.remove();
				logger.info("Node \"" + nodeID + "\" is no longer registered because it was inactive for "
						+ offlineTimeout + " seconds.");
			} else if (now - node.getLastHeartbeatTime(Long.MAX_VALUE) > heartbeatTimeoutMillis
					&& node.switchToOffline()) {

				// Remove services and listeners
				registry.removeActions(node.nodeID);
				eventbus.removeListeners(node.nodeID);

				// Notify listeners
				logger.info("Node \"" + node.nodeID
						+ "\" is no longer available because it hasn't submitted heartbeat signal for "
						+ heartbeatTimeout + " seconds.");
				broadcastNodeDisconnected(node.info, true);
			}
		}
	}

	// --- GET CPU USAGE OF A REMOTE NODE ---

	public int getCpuUsage(String nodeID) {
		NodeDescriptor node = nodes.get(nodeID);
		return node == null ? 0 : node.getCpuUsage().value;
	}

	// --- IS NODE ONLINE? ---

	public boolean isOnline(String nodeID) {
		if (this.nodeID.equals(nodeID)) {
			return true;
		}
		NodeDescriptor node = nodes.get(nodeID);
		return node != null && node.isOnline();
	}

	// --- GET NODE IDS OF ALL NODES ---

	public Set<String> getAllNodeIDs() {
		HashSet<String> nodeIDs = new HashSet<>(nodes.keySet());
		nodeIDs.add(nodeID);
		return nodeIDs;
	}

	// --- GET NODE DESCRIPTOR ---

	public NodeDescriptor getNodeDescriptor(String nodeID) {
		if (this.nodeID.equals(nodeID)) {
			return registry.getDescriptor();
		}
		NodeDescriptor node = nodes.get(nodeID);
		return node == null ? null : node;
	}

	// --- OPTIONAL ERROR HANDLER ---

	/**
	 * Any I/O error occured. Implementation-specific error handling goes here
	 * (reconnection, etc.).
	 * 
	 * @param error
	 *            I/O error
	 */
	protected void error(Throwable error) {
	}

	// --- GETTERS / SETTERS ---

	public Serializer getSerializer() {
		return serializer;
	}

	public void setSerializer(Serializer serializer) {
		this.serializer = Objects.requireNonNull(serializer);
	}

	public int getHeartbeatInterval() {
		return heartbeatInterval;
	}

	public void setHeartbeatInterval(int heartbeatInterval) {
		this.heartbeatInterval = heartbeatInterval;
	}

	public int getHeartbeatTimeout() {
		return heartbeatTimeout;
	}

	public void setHeartbeatTimeout(int heartbeatTimeout) {
		this.heartbeatTimeout = heartbeatTimeout;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public int getOfflineTimeout() {
		return offlineTimeout;
	}

	public void setOfflineTimeout(int offlineTimeout) {
		this.offlineTimeout = offlineTimeout;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = Objects.requireNonNull(namespace);
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

}