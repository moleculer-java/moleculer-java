/**
 * This software is licensed under MIT license.<br>
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

import static services.moleculer.ServiceBroker.MOLECULER_VERSION;
import static services.moleculer.util.CommonUtils.nameOf;
import static services.moleculer.util.CommonUtils.serializerTypeToClass;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
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
import services.moleculer.context.CallingOptions;
import services.moleculer.context.Context;
import services.moleculer.eventbus.EventBus;
import services.moleculer.monitor.Monitor;
import services.moleculer.serializer.JsonSerializer;
import services.moleculer.serializer.Serializer;
import services.moleculer.service.Name;
import services.moleculer.service.ServiceRegistry;

/**
 * Base superclass of all Transporter implementations.
 * 
 * @see RedisTransporter
 * @see NatsTransporter
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

	protected String prefix;
	protected ServiceBroker broker;
	protected String nodeID;
	protected int heartbeatInterval;
	protected int heartbeatTimeout;

	// --- SERIALIZER / DESERIALIZER ---

	protected Serializer serializer;

	// --- COMPONENTS ---

	protected ExecutorService executor;
	protected ScheduledExecutorService scheduler;
	protected ServiceRegistry registry;
	protected EventBus eventbus;
	protected Monitor monitor;

	// --- HEARTBEAT TIMES OF OTHER NODES ---

	protected final ConcurrentHashMap<String, Long> lastNodeActivities = new ConcurrentHashMap<>(128);

	// --- CONSTUCTORS ---

	public Transporter() {
		this("MOL");
	}

	public Transporter(String prefix) {
		this.prefix = prefix;
	}

	public Transporter(String prefix, Serializer serializer) {
		this.prefix = prefix;
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
		prefix = config.get(PREFIX, prefix);

		// Create serializer
		Tree serializerNode = config.get(SERIALIZER);
		if (serializerNode != null) {
			String type;
			if (serializerNode.isPrimitive()) {
				type = serializerNode.asString();
			} else {
				type = serializerNode.get(TYPE, "json");
			}

			@SuppressWarnings("unchecked")
			Class<? extends Serializer> c = (Class<? extends Serializer>) Class.forName(serializerTypeToClass(type));
			serializer = c.newInstance();
		} else {
			serializerNode = config.putMap(SERIALIZER);
		}
		if (serializer == null) {
			serializer = new JsonSerializer();
		}

		// Heartbeat interval (find in Transporter config)
		heartbeatInterval = config.get(HEARTBEAT_INTERVAL, heartbeatInterval);
		if (heartbeatInterval < 1) {

			// Find in broker config
			heartbeatInterval = config.getParent().get(HEARTBEAT_INTERVAL, 0);
			if (heartbeatInterval < 1) {
				heartbeatInterval = 5;
			}
		}
		logger.info(nameOf(this, true) + " sends heartbeat signal every " + heartbeatInterval + " seconds.");

		// Heartbeat timeout (find in Transporter config)
		heartbeatTimeout = config.get(HEARTBEAT_TIMEOUT, heartbeatTimeout);
		if (heartbeatTimeout < 1) {

			// Find in broker config
			heartbeatTimeout = config.getParent().get(HEARTBEAT_TIMEOUT, 0);
			if (heartbeatTimeout < 1) {
				heartbeatTimeout = 30;
			}
		}
		logger.info("Heartbeat timeout of " + nameOf(this, true) + " is " + heartbeatTimeout + " seconds.");

		// Start serializer
		logger.info(nameOf(this, true) + " will use " + nameOf(serializer, true) + '.');
		serializer.start(broker, serializerNode);

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

	/**
	 * Cancelable "Heart Beat" timer
	 */
	private volatile ScheduledFuture<?> heartBeatTimer;

	/**
	 * Cancelable "Check Nodes" timer
	 */
	private volatile ScheduledFuture<?> checkNodesTimer;

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

				// Start sendHeartbeatPacket timer
				if (heartBeatTimer == null) {
					heartBeatTimer = scheduler.scheduleAtFixedRate(this::sendHeartbeatPacket, heartbeatInterval,
							heartbeatInterval, TimeUnit.SECONDS);
				}

				// Start checkNodes timer
				if (checkNodesTimer == null) {
					checkNodesTimer = scheduler.scheduleAtFixedRate(this::checkNodes, heartbeatTimeout,
							heartbeatTimeout, TimeUnit.SECONDS);
				}

			}).Catch(error -> {

				logger.warn("Unable to subscribe channels!", error);
				error(error);

			});
		});
	}

	// --- SERVER DISCONNECTED ---

	protected void disconnected() {

		// Stop heartbeat timer
		if (heartBeatTimer != null) {
			heartBeatTimer.cancel(false);
			heartBeatTimer = null;
		}

		// Stop checkNodes timer
		if (checkNodesTimer != null) {
			checkNodesTimer.cancel(false);
			checkNodesTimer = null;
		}

		// Clear timestamps
		lastNodeActivities.clear();
	}

	// --- STOP TRANSPORTER ---

	/**
	 * Closes transporter.
	 */
	@Override
	public void stop() {
		sendDisconnectPacket();
	}

	// --- REQUEST PACKET ---

	public Tree createRequestPacket(Context ctx) {
		Tree message = new Tree();
		message.put(VER, ServiceBroker.MOLECULER_VERSION);
		message.put(SENDER, nodeID);
		message.put("id", ctx.id());
		message.put("action", ctx.name());

		message.putObject(PARAMS, ctx.params());
		message.put("meta", (String) null);
		
		CallingOptions opts = ctx.opts();
		if (opts != null) {
			message.put("timeout", ctx.opts().timeout());
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

			// logger.info("FROM " + channel + ": " + new String(message));

			// Parse message
			Tree data;
			try {
				data = serializer.read(message);
			} catch (Exception cause) {
				logger.warn("Unable to parse incoming message!", cause);
				return;
			}

			// Send message to proper component
			try {

				// Get "sender" property
				String sender = data.get(SENDER, "");
				if (sender == null || sender.isEmpty()) {
					logger.warn("Missing \"" + SENDER + "\" property:\r\n" + data);
					return;
				}
				if (sender.equals(nodeID)) {

					// It's our message
					return;
				}

				// Invoming event
				if (channel.equals(eventChannel)) {
					eventbus.receiveEvent(data);
					return;
				}
				
				// Incoming request
				if (channel.equals(requestChannel)) {
					registry.receiveRequest(data);
					return;
				}

				// Incoming response
				if (channel.equals(responseChannel)) {
					registry.receiveResponse(data);
					return;
				}

				// HeartBeat packet
				if (channel.endsWith(heartbeatChannel)) {

					// Store timestamp of the sender's last activity
					lastNodeActivities.put(sender, System.currentTimeMillis());
					return;
				}

				// Info packet
				if (channel.equals(infoChannel) || channel.equals(infoBroadcastChannel)) {

					// Register services in info block
					Tree services = data.get(SERVICES);
					if (services != null && services.isEnumeration()) {
						for (Tree service : services) {
							
							// Register actions
							registry.addActions(service);
							
							// Register listeners
							eventbus.addListeners(service);
						}
						logger.info("Node \"" + sender + "\" connected.");
					}
					return;
				}

				// Discover packet
				if (channel.equals(discoverChannel) || channel.equals(discoverBroadcastChannel)) {

					// Send node desriptor to the sender
					sendInfoPacket(channel(PACKET_INFO, sender));
				}

				// Disconnect packet
				if (channel.equals(disconnectChannel)) {
					lastNodeActivities.remove(sender);
					
					// Remove remote actions
					registry.removeActions(sender);
					
					// Remove remote event listeners
					eventbus.removeListeners(sender);
					
					// Ok, all actions and listeners removed
					logger.info("Node \"" + sender + "\" disconnected.");
				}

			} catch (Exception cause) {
				logger.warn("Unable to process incoming message!", cause);
			}
		});
	}

	// --- GENERIC MOLECULER PACKETS ---

	private final void sendDiscoverPacket(String channel) {
		Tree message = new Tree();
		message.put(VER, MOLECULER_VERSION);
		message.put(SENDER, nodeID);
		publish(channel, message);
	}

	private final void sendInfoPacket(String channel) {
		publish(channel, registry.generateDescriptor());
	}

	private final void sendHeartbeatPacket() {
		Tree message = new Tree();
		message.put(VER, MOLECULER_VERSION);
		message.put(SENDER, nodeID);
		message.put(CPU, monitor.getTotalCpuPercent());
		publish(heartbeatChannel, message);
	}

	private final void sendDisconnectPacket() {
		Tree message = new Tree();
		message.put(SENDER, nodeID);
		publish(disconnectChannel, message);
	}
	
	// --- "CHECK NODES" PROCESS ---

	private volatile long lastCheck;

	private final void checkNodes() {
		Iterator<Map.Entry<String, Long>> entries = lastNodeActivities.entrySet().iterator();
		long now = System.currentTimeMillis();
		if (now < lastCheck) {
			lastCheck = now;
			return;
		}
		lastCheck = now;
		Map.Entry<String, Long> entry;
		long timeoutMillis = heartbeatTimeout * 1000L;
		while (entries.hasNext()) {
			entry = entries.next();
			if (now - entry.getValue() > timeoutMillis) {

				// Get timeouted node's ID
				String nodeID = entry.getKey();

				// Remove remote actions
				registry.removeActions(nodeID);
				
				// Remove remote event listeners
				eventbus.removeListeners(nodeID);
				
				// Remove local timestamp entry
				entries.remove();
				
				// Ok, all actions and listeners removed
				logger.info("Node \"" + nodeID
						+ "\" is no longer available because it hasn't submitted heartbeat signal for "
						+ heartbeatTimeout + " seconds.");
			}
		}
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

	public final Serializer getSerializer() {
		return serializer;
	}

	public final void setSerializer(Serializer serializer) {
		this.serializer = Objects.requireNonNull(serializer);
	}

	public final int getHeartbeatInterval() {
		return heartbeatInterval;
	}

	public final void setHeartbeatInterval(int heartbeatInterval) {
		this.heartbeatInterval = heartbeatInterval;
	}

	public final int getHeartbeatTimeout() {
		return heartbeatTimeout;
	}

	public final void setHeartbeatTimeout(int heartbeatTimeout) {
		this.heartbeatTimeout = heartbeatTimeout;
	}

}