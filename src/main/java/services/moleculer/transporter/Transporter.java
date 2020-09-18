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

import static services.moleculer.util.CommonUtils.nameOf;
import static services.moleculer.util.CommonUtils.removeLocalEvents;
import static services.moleculer.util.CommonUtils.throwableToTree;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.Context;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.eventbus.Groups;
import services.moleculer.metrics.MetricConstants;
import services.moleculer.metrics.Metrics;
import services.moleculer.monitor.Monitor;
import services.moleculer.serializer.JsonSerializer;
import services.moleculer.serializer.Serializer;
import services.moleculer.service.MoleculerComponent;
import services.moleculer.service.Name;
import services.moleculer.service.ServiceInvoker;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.transporter.tcp.NodeDescriptor;
import services.moleculer.transporter.tcp.RemoteAddress;
import services.moleculer.uid.TimeBasedUidGenerator;
import services.moleculer.uid.UidGenerator;
import services.moleculer.util.FastBuildTree;

/**
 * Base superclass of all Transporter implementations.
 *
 * @see TcpTransporter
 * @see RedisTransporter
 * @see NatsTransporter
 * @see NatsStreamingTransporter
 * @see MqttTransporter
 * @see JmsTransporter
 * @see KafkaTransporter
 * @see AmqpTransporter
 */
@Name("Transporter")
public abstract class Transporter extends MoleculerComponent implements MetricConstants {

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
	public String pingChannel;
	public String pongChannel;

	// --- PROPERTIES ---

	/**
	 * Namespace of channels.
	 */
	protected String namespace = "";

	/**
	 * Prefix of channels.
	 */
	protected String prefix = "MOL";

	/**
	 * Local Node ID.
	 */
	protected String nodeID;

	/**
	 * ServiceBroker's protocol version
	 */
	protected String protocolVersion = "4";

	/**
	 * Heartbeat sending period in SECONDS.
	 */
	protected int heartbeatInterval = 5;

	/**
	 * Heartbeat timeout in SECONDS.
	 */
	protected int heartbeatTimeout = 30;

	/**
	 * How long keep information in registry about the offline nodes (SECONDS).
	 */
	protected int offlineTimeout = 180;

	/**
	 * Timeout of channel subscriptions (SECONDS).
	 */
	protected int subscriptionTimeout = 10;

	/**
	 * Use hostnames instead of IP addresses As the DHCP environment is dynamic,
	 * any later attempt to use IPs instead hostnames would most likely yield
	 * false results. Therefore, use hostnames if you are using DHCP.
	 */
	protected boolean preferHostname = true;

	// --- DEBUG COMMUNICATION ---

	/**
	 * Writes the communication of method calls and events into the log.
	 */
	protected boolean debug;

	/**
	 * Writes the communication of heartbeats/gossiping into the log.
	 */
	protected boolean debugHeartbeats;

	// --- SERIALIZER / DESERIALIZER ---

	protected Serializer serializer;

	protected boolean usingJsonSerializer = true;

	// --- INSTANCE ID ---
	
	protected final String instanceID;
	
	// --- COMPONENTS ---

	protected ExecutorService executor;
	protected ScheduledExecutorService scheduler;
	protected ServiceRegistry registry;
	protected ServiceInvoker serviceInvoker;
	protected Eventbus eventbus;
	protected Monitor monitor;
	protected UidGenerator uidGenerator;
	protected Metrics metrics;

	// --- TIMER ---

	/**
	 * Cancelable "Heart Beat" timer
	 */
	protected volatile ScheduledFuture<?> heartBeatTimer;

	/**
	 * Cancelable "Check Activities / Infos" timer
	 */
	protected volatile ScheduledFuture<?> checkTimeoutTimer;

	// --- REMOTE NODES ---

	protected final ConcurrentHashMap<String, NodeDescriptor> nodes = new ConcurrentHashMap<>(256);

	// --- SENDING INFO BLOCK ---

	protected final AtomicBoolean infoScheduled = new AtomicBoolean();
	protected final AtomicLong infoScheduledAt = new AtomicLong();
	
	// --- FOR TESTING ONLY ---
	
	protected final AtomicLong lastReceivedMessageAt = new AtomicLong();
	
	// --- CONSTUCTORS ---

	public Transporter() {
		this(new JsonSerializer());
	}

	public Transporter(Serializer serializer) {
		this.serializer = Objects.requireNonNull(serializer);
		this.instanceID = new TimeBasedUidGenerator().nextUID();
	}

	// --- START TRANSPORTER ---

	/**
	 * Initializes transporter instance.
	 *
	 * @param broker
	 *            parent ServiceBroker
	 */
	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Set serializer's debug mode
		if (debug) {
			serializer.setDebug(true);
		}
		
		// Process config
		ServiceBrokerConfig cfg = broker.getConfig();
		namespace = cfg.getNamespace();
		if (namespace != null && !namespace.isEmpty()) {
			prefix = prefix + '-' + namespace;
		}
		nodeID = broker.getNodeID();

		// Set the protocol version
		protocolVersion = broker.getProtocolVersion();

		// Log serializer info
		serializer.started(broker);
		logger.info(nameOf(this, true) + " will use " + nameOf(serializer, true) + '.');

		// Get components
		executor = cfg.getExecutor();
		scheduler = cfg.getScheduler();
		registry = cfg.getServiceRegistry();
		monitor = cfg.getMonitor();
		eventbus = cfg.getEventbus();
		uidGenerator = cfg.getUidGenerator();
		serviceInvoker = cfg.getServiceInvoker();
		if (cfg.isMetricsEnabled()) {
			metrics = cfg.getMetrics();
			if (metrics != null) {
				metrics.set(MOLECULER_TRANSIT_CONNECTED, MOLECULER_TRANSIT_CONNECTED_DESC, 0);
				metrics.increment(MOLECULER_TRANSPORTER_PACKETS_SENT_TOTAL, MOLECULER_TRANSPORTER_PACKETS_SENT_TOTAL_DESC, 0);
				metrics.increment(MOLECULER_TRANSPORTER_PACKETS_SENT_BYTES, MOLECULER_TRANSPORTER_PACKETS_SENT_BYTES_DESC, 0);				
				metrics.increment(MOLECULER_TRANSPORTER_PACKETS_RECEIVED_TOTAL, MOLECULER_TRANSPORTER_PACKETS_RECEIVED_TOTAL_DESC, 0);
				metrics.increment(MOLECULER_TRANSPORTER_PACKETS_RECEIVED_BYTES, MOLECULER_TRANSPORTER_PACKETS_RECEIVED_BYTES_DESC, 0);				
			}
		}

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

	protected void connected() {
		connected(true);
	}

	protected void connected(boolean waitFor) {
		Promise promise = new Promise();
		executor.execute(() -> {

			// Subscribe all required channels
			Promise.all(subscribe(eventChannel), // EVENT
					subscribe(requestChannel), // REQ
					subscribe(responseChannel), // RES
					subscribe(discoverChannel), // DISCOVER
					subscribe(infoChannel), // INFO
					subscribe(pingChannel), // PING
					subscribe(pongChannel), // PONG
					subscribe(disconnectChannel), // DISCONNECT
					subscribe(discoverBroadcastChannel), // DISCOVER
					subscribe(infoBroadcastChannel), // INFO
					subscribe(heartbeatChannel) // HEARTBEAT					
			).then(in -> {
				promise.complete();

				// Redis transporter is ready for use
				logger.info("All channels subscribed successfully.");
				
				// Do the discovery process
				scheduler.schedule(() -> {
					sendDiscoverPacket(discoverBroadcastChannel);
				}, 500, TimeUnit.MILLISECONDS);
				broadcastInfoPacket();

				// Start sendHeartbeat timer
				if (heartBeatTimer == null && heartbeatInterval > 0) {
					heartBeatTimer = scheduler.scheduleAtFixedRate(this::sendHeartbeatPacket, heartbeatInterval,
							heartbeatInterval, TimeUnit.SECONDS);
				}

				// Start timeout checker's timer
				if (checkTimeoutTimer == null && (heartbeatTimeout > 0 || offlineTimeout > 0)) {
					int checkPeriod = Math.max(5, heartbeatTimeout / 3);
					checkTimeoutTimer = scheduler.scheduleAtFixedRate(this::checkTimeouts, checkPeriod, checkPeriod,
							TimeUnit.SECONDS);
				}

				// Notify internal listeners
				broadcastTransporterConnected();
				
			}).catchError(error -> {

				logger.warn("Unable to subscribe channels!", error);
				error(error);
				promise.complete(error);

			});
		});
		if (waitFor) {
			try {
				promise.waitFor(subscriptionTimeout, TimeUnit.SECONDS);
			} catch (Exception timeout) {
				error(timeout);
			}
		}
	}

	// --- STOP TRANSPORTER ---

	/**
	 * Closes transporter.
	 */
	@Override
	public void stopped() {

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

		// Send "disconnected" packet
		sendDisconnectPacket();

		// Clear all stored data
		nodes.clear();
	}

	// --- GENERIC MOLECULER PACKETS ---

	protected void sendInfoPacket(String channel) {
		Tree msg = removeLocalEvents(registry.getDescriptor());		
		msg.put("ver", protocolVersion);
		msg.put("sender", nodeID);
		msg.put("seq", registry.getTimestamp());
		msg.put("instanceID", instanceID);
		publish(channel, msg);
	}

	protected void sendDiscoverPacket(String channel) {
		FastBuildTree msg = new FastBuildTree(3);
		msg.putUnsafe("ver", protocolVersion);
		msg.putUnsafe("sender", nodeID);
		publish(channel, msg);
	}

	protected void sendHeartbeatPacket() {
		FastBuildTree msg = new FastBuildTree(4);
		msg.putUnsafe("ver", protocolVersion);
		msg.putUnsafe("sender", nodeID);
		msg.putUnsafe("cpu", monitor.getTotalCpuPercent());
		publish(heartbeatChannel, msg);
	}

	protected void sendDisconnectPacket() {
		FastBuildTree msg = new FastBuildTree(3);
		msg.putUnsafe("ver", protocolVersion);
		msg.putUnsafe("sender", nodeID);
		publish(disconnectChannel, msg);
	}

	protected void sendPongPacket(String channel, Tree data) {
		data.put("sender", this.nodeID);
		data.put("arrived", System.currentTimeMillis());
		publish(channel, data);
	}

	// --- PING PACKET ---

	public Tree createPingPacket(String id) {
		FastBuildTree msg = new FastBuildTree(5);
		msg.putUnsafe("ver", protocolVersion);
		msg.putUnsafe("sender", nodeID);
		msg.putUnsafe("id", id);
		msg.putUnsafe("time", System.currentTimeMillis());
		return msg;
	}

	// --- EVENT PACKET ---

	public void sendEventPacket(String nodeID, Context ctx, Groups groups, boolean broadcast) {
		FastBuildTree msg = new FastBuildTree(13);

		// Add basic properties (version, sender's nodeID, etc.)
		msg.putUnsafe("ver", protocolVersion);
		msg.putUnsafe("sender", this.nodeID);
		msg.putUnsafe("id", ctx.id);
		msg.putUnsafe("event", ctx.name);

		// Call level
		msg.putUnsafe("level", ctx.level);

		// Broadcast or emit?
		msg.putUnsafe("broadcast", broadcast);

		// Add groups
		if (groups != null) {
			String[] array = groups.groups();
			if (array != null && array.length > 0) {
				msg.putUnsafe("groups", array);
			}
		}

		// Request ID
		if (ctx.requestID != null) {
			msg.putUnsafe("requestID", ctx.requestID);
		}

		// Add params and meta
		if (ctx.params != null) {
			msg.putUnsafe("data", ctx.params.asObject());
			Tree meta = ctx.params.getMeta(false);
			if (meta != null) {
				msg.putUnsafe("meta", meta.asObject());
			}
		}

		// Streaming content
		if (ctx.stream != null) {

			// Streaming in progress
			msg.putUnsafe("stream", true);

			// First sequence
			msg.putUnsafe("seq", 0);
		}

		// Send message
		publish(PACKET_EVENT, nodeID, msg);
	}

	// --- REQUEST PACKET ---

	public void sendRequestPacket(String nodeID, Context ctx) {
		FastBuildTree msg = new FastBuildTree(13);

		// Add basic properties (version, sender's nodeID, etc.)
		msg.putUnsafe("ver", protocolVersion);
		msg.putUnsafe("sender", this.nodeID);
		msg.putUnsafe("id", ctx.id);
		msg.putUnsafe("action", ctx.name);

		// Timeout
		if (ctx.opts != null && ctx.opts.timeout > 0) {
			msg.putUnsafe("timeout", ctx.opts.timeout);
		}

		// Call level
		msg.putUnsafe("level", ctx.level);

		// Parent ID
		if (ctx.parentID != null) {
			msg.putUnsafe("parentID", ctx.parentID);
		}

		// Request ID
		if (ctx.requestID != null) {
			msg.putUnsafe("requestID", ctx.requestID);
		}

		// Add params and meta
		if (ctx.params != null) {
			msg.putUnsafe("params", ctx.params.asObject());
			Tree meta = ctx.params.getMeta(false);
			if (meta != null) {
				msg.putUnsafe("meta", meta.asObject());
			}
		}

		// Streaming content
		if (ctx.stream != null) {

			// Streaming in progress
			msg.putUnsafe("stream", true);

			// First sequence
			msg.putUnsafe("seq", 0);
		}

		// Send message
		publish(Transporter.PACKET_REQUEST, nodeID, msg);
	}

	// --- DATA PACKET (STREAMING) ---

	public void sendDataPacket(String cmd, String nodeID, Context ctx, byte[] bytes, long sequence) {
		FastBuildTree msg = new FastBuildTree(8);

		// Add required properties (version, sender's nodeID, request ID)
		msg.putUnsafe("ver", protocolVersion);
		msg.putUnsafe("sender", this.nodeID);
		msg.putUnsafe("id", ctx.id);

		// Streaming in progress
		msg.putUnsafe("stream", true);

		// Stream packet counter (1...N)
		msg.putUnsafe("seq", sequence);

		// Add "params" block
		FastBuildTree params = new FastBuildTree(3);
		if (PACKET_RESPONSE.equals(cmd)) {
			msg.putUnsafe("success", true);
			msg.putUnsafe("data", params);
		} else {
			msg.putUnsafe("params", params);
		}
		params.putUnsafe("type", "Buffer");

		// Convert signed byte array to unsigned short array
		if (usingJsonSerializer) {

			// Using NodeJS compatible byte-array encoding
			short[] data = new short[bytes.length];
			for (int i = 0; i < bytes.length; i++) {
				data[i] = (short) (bytes[i] & 0xFF);
			}
			params.putUnsafe("data", data);
		} else {

			// Other formats, eg. MessagePack
			params.putUnsafe("data", bytes);
		}

		// Send message
		publish(cmd, nodeID, msg);
	}

	// --- ERROR PACKET (STREAMING) ---

	public void sendErrorPacket(String cmd, String nodeID, Context ctx, Throwable cause, long sequence) {
		FastBuildTree msg = throwableToTree(ctx.id, this.nodeID, protocolVersion, cause);

		// Stream packet counter (1...N)
		msg.putUnsafe("seq", sequence);

		// End of streaming
		msg.putUnsafe("stream", false);

		// Send message
		publish(cmd, nodeID, msg);
	}

	// --- CLOSE PACKET (STREAMING) ---

	public void sendClosePacket(String cmd, String nodeID, Context ctx, long sequence) {
		FastBuildTree msg = new FastBuildTree(8);

		// Add required properties (version, sender's nodeID, request ID)
		msg.putUnsafe("ver", protocolVersion);
		msg.putUnsafe("sender", this.nodeID);
		msg.putUnsafe("id", ctx.id);
		msg.putUnsafe("success", true);
		msg.putUnsafe("meta", Collections.EMPTY_MAP);

		// Stream packet counter (1...N)
		msg.putUnsafe("seq", sequence);

		// End of streaming
		msg.putUnsafe("stream", false);

		// Send message
		publish(cmd, nodeID, msg);
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

	/**
	 * Process incoming message later (in a new Runnable or JoinForkTask).
	 * 
	 * @param channel
	 *            incoming channel
	 * @param message
	 *            incoming message
	 */
	protected void received(String channel, byte[] message) {
		executor.execute(() -> {
			processReceivedMessage(channel, message);
		});
	}

	/**
	 * Process incoming message directly (without new Task).
	 * 
	 * @param channel
	 *            incoming channel
	 * @param message
	 *            incoming message
	 */
	protected void processReceivedMessage(String channel, byte[] message) {
		try {
			
			// Metrics
			if (metrics != null) {
				metrics.increment(MOLECULER_TRANSPORTER_PACKETS_RECEIVED_TOTAL, MOLECULER_TRANSPORTER_PACKETS_RECEIVED_TOTAL_DESC);
				metrics.increment(MOLECULER_TRANSPORTER_PACKETS_RECEIVED_BYTES, MOLECULER_TRANSPORTER_PACKETS_RECEIVED_BYTES_DESC, message.length);
			}
			
			// Process parsed (JSON) request
			processReceivedMessage(channel, serializer.read(message));
		} catch (Exception cause) {
			logger.warn("Unable to parse incoming message!", cause);
			return;
		}
	}

	/**
	 * Process incoming message directly (without new Task).
	 * 
	 * @param channel
	 *            incoming channel
	 * @param message
	 *            incoming message
	 */
	protected void processReceivedMessage(String channel, Tree message) {

		// Debug & test
		if (debug && (debugHeartbeats || !channel.endsWith(heartbeatChannel))) {
			logger.info("Message received from channel \"" + channel + "\":\r\n" + message);
			lastReceivedMessageAt.set(System.currentTimeMillis());
		}

		// Send message to proper component
		try {

			// Get "sender" property
			String sender = message.get("sender", "");
			if (sender == null || sender.isEmpty()) {
				logger.warn("Missing \"sender\" property:\r\n" + message);
				return;
			}
			if (sender.equals(nodeID)) {

				// It's our message
				return;
			}

			// Incoming response
			if (channel.equals(responseChannel)) {
				registry.receiveResponse(message);
				return;
			}

			// Incoming event
			if (channel.equals(eventChannel)) {
				eventbus.receiveEvent(message);
				return;
			}

			// Incoming request
			if (channel.equals(requestChannel)) {
				registry.receiveRequest(message);
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
				int cpu = message.get("cpu", 0);

				// Update CPU info
				boolean offline;
				node.writeLock.lock();
				try {
					node.updateCpu(cpu);
					offline = node.offlineSince > 0;
				} finally {
					node.writeLock.unlock();
				}
				if (offline) {

					// Offline node -> send discover packet
					sendDiscoverPacket(channel(PACKET_DISCOVER, sender));
				}
				return;
			}

			// Info packet
			if (channel.equals(infoChannel) || channel.equals(infoBroadcastChannel)) {

				// Register services and listeners
				message.put("seq", System.currentTimeMillis());
				message.put("port", 1);
				updateNodeInfo(sender, message);
				return;
			}

			// Discover packet
			if (channel.equals(discoverChannel) || channel.equals(discoverBroadcastChannel)) {

				// Send node desriptor to the sender
				sendInfoPacket(channel(PACKET_INFO, sender));
				return;
			}

			// Ping packet
			if (channel.equals(pingChannel)) {
				sendPongPacket(channel(PACKET_PONG, sender), message);
				return;
			}

			// Pong packet
			if (channel.equals(pongChannel)) {
				registry.receivePong(message);
				return;
			}

			// Disconnect packet
			if (channel.equals(disconnectChannel)) {

				// Switch to offline
				NodeDescriptor node = nodes.get(sender);
				if (node == null) {
					return;
				}
				boolean disconnected = false;
				node.writeLock.lock();
				try {
					if (node.markAsOffline()) {

						// Remove remote actions and listeners
						registry.removeActions(sender);
						eventbus.removeListeners(sender);
						disconnected = true;

					}
				} finally {
					node.writeLock.unlock();
				}
				if (node != null && disconnected) {

					// Notify listeners (not unexpected disconnection)
					logger.info("Node \"" + sender + "\" disconnected.");
					broadcastNodeDisconnected(node.info, false);
				}
				return;
			}

		} catch (Exception cause) {
			logger.warn("Unable to process incoming message!", cause);
		}
	}

	protected void updateNodeInfo(String sender, Tree info) throws Exception {
		boolean connected = false;
		boolean reconnected = false;
		boolean updated = false;
		NodeDescriptor node = nodes.get(sender);
		if (node == null) {

			// New, unknown node
			connected = true;
			node = new NodeDescriptor(sender, preferHostname, false, info);
			nodes.put(sender, node);

		} else {
			node.writeLock.lock();
			try {

				// Node is registered
				if (node.seq == 0) {

					// Node connected (it was offline)
					connected = true;
					node.markAsOnline(info);

				} else {

					// Try to update current node
					Tree prevInfo = node.info;
					boolean wasOnline = node.offlineSince == 0;
					if (node.markAsOnline(info)) {

						// Store new node info
						if (prevInfo != null && wasOnline) {
							Tree s1 = prevInfo.get("services");
							if (s1 != null) {
								Tree s2 = node.info.get("services");
								if (s2 != null && s1.equals(s2)) {

									// Service blocks are equal
									return;
								}
							}
						}
						if (wasOnline) {
							updated = true;
						} else {
							reconnected = true;
						}

					} else {

						// New info block is older than ours
						return;
					}
				}

			} finally {
				node.writeLock.unlock();
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
			if (services != null && !services.isEmpty()) {
				for (Tree service : services) {
					registry.addActions(sender, service);
					eventbus.addListeners(sender, service);
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

	// --- INTERNAL MOLECULER EVENTS ---

	protected void broadcastTransporterConnected() {
		eventbus.broadcast(new Context(serviceInvoker, eventbus, uidGenerator, uidGenerator.nextUID(),
				"$transporter.connected", null, 1, null, null, null, null, nodeID), null, true);
		
		// Metrics
		if (metrics != null) {
			metrics.set(MOLECULER_TRANSIT_CONNECTED, MOLECULER_TRANSIT_CONNECTED_DESC, 1); 
		}		
	}

	protected void broadcastTransporterDisconnected() {
		eventbus.broadcast(new Context(serviceInvoker, eventbus, uidGenerator, uidGenerator.nextUID(),
				"$transporter.disconnected", null, 1, null, null, null, null, nodeID), null, true);
		
		// Metrics
		if (metrics != null) {
			metrics.set(MOLECULER_TRANSIT_CONNECTED, MOLECULER_TRANSIT_CONNECTED_DESC, 0); 
		}		
	}

	protected void broadcastNodeConnected(Tree info, boolean reconnected) {
		if (info != null) {
			Tree msg = new Tree();
			msg.putObject("node", info);
			msg.put("reconnected", reconnected);
			eventbus.broadcast(new Context(serviceInvoker, eventbus, uidGenerator, uidGenerator.nextUID(),
					"$node.connected", msg, 1, null, null, null, null, nodeID), null, true);
		}
	}

	protected void broadcastNodeUpdated(Tree info) {
		if (info != null) {
			Tree msg = new Tree();
			msg.putObject("node", info);
			eventbus.broadcast(new Context(serviceInvoker, eventbus, uidGenerator, uidGenerator.nextUID(),
					"$node.updated", msg, 1, null, null, null, null, nodeID), null, true);
		}
	}

	protected void broadcastNodeDisconnected(Tree info, boolean unexpected) {
		if (info != null) {
			Tree msg = new Tree();
			msg.putObject("node", info);
			msg.put("unexpected", unexpected);
			eventbus.broadcast(new Context(serviceInvoker, eventbus, uidGenerator, uidGenerator.nextUID(),
					"$node.disconnected", msg, 1, null, null, null, null, nodeID), null, true);
		}
	}

	// --- SEND BROADCAST INFO PACKET ---

	public void broadcastInfoPacket() {
		infoScheduledAt.set(System.currentTimeMillis());
		if (infoScheduled.compareAndSet(false, true)) {
			scheduleInfoPacket();
		}
	}
	
	protected void scheduleInfoPacket() {
		scheduler.schedule(() -> {
			if (System.currentTimeMillis() - infoScheduledAt.get() >= 1000L) {
				infoScheduled.set(false);
				sendInfoPacket(infoBroadcastChannel);
			} else {
				scheduleInfoPacket();
			}
		}, 1, TimeUnit.SECONDS);		
	}

	// --- TIMEOUT PROCESS ---

	protected void checkTimeouts() {

		// Compute timeout limits
		long now = System.currentTimeMillis();
		Iterator<NodeDescriptor> i;
		NodeDescriptor node;

		// Check offline timeout
		if (offlineTimeout > 0) {
			i = nodes.values().iterator();
			long offlineTimeoutMillis = offlineTimeout * 1000L;
			while (i.hasNext()) {
				node = i.next();
				if (node.local) {
					continue;
				}
				node.readLock.lock();
				try {
					if (node.offlineSince > 0 && now - node.offlineSince > offlineTimeoutMillis) {

						// Remove node info from registry
						i.remove();
						logger.info("Node \"" + node.nodeID + "\" is no longer registered because it was inactive for "
								+ offlineTimeout + " seconds.");

					}
				} finally {
					node.readLock.unlock();
				}
			}
		}

		// Check heartbeat timeout
		if (heartbeatTimeout > 0) {
			long heartbeatTimeoutMillis = heartbeatTimeout * 1000L;
			i = nodes.values().iterator();
			LinkedList<NodeDescriptor> disconnectedNodes = new LinkedList<>();
			while (i.hasNext()) {
				node = i.next();
				if (node.local) {
					continue;
				}
				node.writeLock.lock();
				try {
					if (node.cpuWhen > 0 && now - node.cpuWhen > heartbeatTimeoutMillis && node.markAsOffline()) {

						// Remove services and listeners
						registry.removeActions(node.nodeID);
						eventbus.removeListeners(node.nodeID);
						disconnectedNodes.add(node);
					}
				} finally {
					node.writeLock.unlock();
				}
			}
			i = disconnectedNodes.iterator();
			while (i.hasNext()) {
				node = i.next();

				// Notify listeners
				logger.info("Heartbeat is not received from \"" + node.nodeID + "\" node.");
				logger.info("Node \"" + node.nodeID + "\" disconnected unexpectedly.");
				broadcastNodeDisconnected(node.info, true);
			}
		}
	}

	// --- GET CPU USAGE OF A REMOTE NODE ---

	public int getCpuUsage(String nodeID) {
		if (this.nodeID.equals(nodeID)) {
			return monitor.getTotalCpuPercent();
		}
		NodeDescriptor node = nodes.get(nodeID);
		return node == null ? 0 : node.cpu;
	}

	// --- GET LAST HEARTBEAT TIME OF A REMOTE NODE ---

	public long getLastHeartbeatTime(String nodeID) {
		if (this.nodeID.equals(nodeID)) {
			return System.currentTimeMillis();
		}
		NodeDescriptor node = nodes.get(nodeID);
		return node == null ? 0 : node.cpuWhen;
	}

	// --- IS NODE ONLINE? ---

	public boolean isOnline(String nodeID) {
		if (this.nodeID.equals(nodeID)) {
			return true;
		}
		NodeDescriptor node = nodes.get(nodeID);
		if (node == null) {
			return false;
		}
		node.readLock.lock();
		try {
			return node.offlineSince == 0 && node.seq > 0;
		} finally {
			node.readLock.unlock();
		}
	}

	// --- GET NODE IDS OF ALL NODES ---

	public Set<String> getAllNodeIDs() {
		HashSet<String> ids = new HashSet<>(nodes.keySet());
		ids.add(nodeID);
		return ids;
	}

	// --- GET DESCRIPTOR OF A NODE ---

	public Tree getDescriptor(String nodeID) {
		if (this.nodeID.equals(nodeID)) {
			return registry.getDescriptor();
		}
		NodeDescriptor node = nodes.get(nodeID);
		if (node == null) {
			return null;
		}
		Tree info = null;
		node.readLock.lock();
		try {
			if (node.info != null) {
				info = node.info.clone();
			}
		} finally {
			node.readLock.unlock();
		}
		return info;
	}

	// --- GET SOCKET ADDRESS OF A NODE ---

	public RemoteAddress getAddress(String nodeID) {
		NodeDescriptor node = nodes.get(nodeID);
		if (node == null) {
			return null;
		}
		RemoteAddress address;
		node.readLock.lock();
		try {
			address = new RemoteAddress(node.host, node.port);
		} finally {
			node.readLock.unlock();
		}
		return address;
	}

	// --- OPTIONAL ERROR HANDLER ---

	/**
	 * Any I/O error occurred. Implementation-specific error handling goes here
	 * (reconnection, etc.).
	 *
	 * @param error
	 *            I/O error
	 */
	protected void error(Throwable error) {

		// Do nothing by default
	}

	// --- GETTERS / SETTERS ---

	public Serializer getSerializer() {
		return serializer;
	}

	public void setSerializer(Serializer serializer) {
		this.serializer = Objects.requireNonNull(serializer);
		this.usingJsonSerializer = serializer.getFormat().toLowerCase().contains("json");
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

	public boolean isPreferHostname() {
		return preferHostname;
	}

	public void setPreferHostname(boolean preferHostname) {
		this.preferHostname = preferHostname;
	}

	public int getSubscriptionTimeout() {
		return subscriptionTimeout;
	}

	public void setSubscriptionTimeout(int subscriptionTimeout) {
		this.subscriptionTimeout = subscriptionTimeout;
	}

	public boolean isDebugHeartbeats() {
		return debugHeartbeats;
	}

	public void setDebugHeartbeats(boolean debugHeartbeats) {
		this.debugHeartbeats = debugHeartbeats;
	}

	public String getInstanceID() {
		return instanceID;
	}

}