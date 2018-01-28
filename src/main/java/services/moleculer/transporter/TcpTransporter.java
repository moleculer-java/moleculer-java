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

import static services.moleculer.util.CommonUtils.parseURLs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.monitor.Monitor;
import services.moleculer.service.Name;
import services.moleculer.transporter.tcp.SendBuffer;
import services.moleculer.transporter.tcp.TcpReader;
import services.moleculer.transporter.tcp.TcpWriter;
import services.moleculer.transporter.tcp.UDPBroadcaster;

/**
 * TCP Transporter with optional UDP discovery ("zero configuration") module.
 * TCP Transporter uses fault tolerant and peer-to-peer GOSSIP protocol to
 * discover location and service information about the other nodes participating
 * in a Moleculer Cluster.
 */
@Name("TCP Transporter")
public class TcpTransporter extends Transporter {

	// --- PACKET IDS ---

	protected static final byte PACKET_EVENT_ID = 1;
	protected static final byte PACKET_REQUEST_ID = 2;
	protected static final byte PACKET_RESPONSE_ID = 3;
	protected static final byte PACKET_PING_ID = 4;
	protected static final byte PACKET_PONG_ID = 5;
	protected static final byte PACKET_GOSSIP_ID = 6;

	// --- PROPERTIES ---

	/**
	 * TCP port (used by the Transporter and Gossiper services)
	 */
	protected int port = 7328;

	/**
	 * Gossiping period time, in SECONDS
	 */
	protected int gossipPeriod = 1;

	/**
	 * Max number of keep-alive connections (0 = unlimited)
	 */
	protected int maxKeepAliveConnections;

	/**
	 * Keep-alive timeout in SECONDS (0 = no timeout)
	 */
	protected int keepAliveTimeout = 60;

	/**
	 * Max enable packet size (BYTES)
	 */
	protected int maxPacketSize = 1024 * 1024 * 64;

	/**
	 * List of URLs ("tcp://host:port/nodeID" or "host:port/nodeID" or
	 * "host/nodeID"), when UDP discovery is disabled
	 */
	protected String[] urls = new String[0];

	/**
	 * UDP multicast host of automatic discovery service ("zero config" mode)
	 */
	protected String multicastHost = "230.0.0.0";

	/**
	 * UDP multicast port of automatic discovery service
	 */
	protected int multicastPort = 4445;

	/**
	 * UDP multicast period in SECONDS
	 */
	protected int multicastPeriod = 60;

	// --- COMPONENTS ---

	/**
	 * CPU monitor
	 */
	protected Monitor monitor;

	// --- CONSTUCTORS ---

	/**
	 * Start TCP Transporter in "zero config" mode, with automatic UDP service
	 * discovery.
	 */
	public TcpTransporter() {
		super();
	}

	/**
	 * Start TCP Transporter in "zero config" mode, with automatic UDP service
	 * discovery, with the specified prefix.
	 */
	public TcpTransporter(String prefix) {
		super(prefix);
	}

	/**
	 * Start TCP Transporter in full TCP mode, without UDP discovery.
	 */
	public TcpTransporter(String prefix, String... urls) {
		super(prefix);
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

		// Disable heartbeat messages
		heartbeatInterval = 0;
		heartbeatTimeout = 0;
		if (urls != null && urls.length > 0) {
			offlineTimeout = 0;
		}

		// TCP server's port
		port = config.get("port", port);

		// Process config
		urls = parseURLs(config, urls);

		// Set components
		monitor = broker.components().monitor();

		// Gossiper's gossiping period in seconds
		gossipPeriod = config.get("gossipPeriod", gossipPeriod);

		// TCP socket properties
		maxKeepAliveConnections = config.get("maxKeepAliveConnections", maxKeepAliveConnections);
		keepAliveTimeout = config.get("keepAliveTimeout", keepAliveTimeout);

		// Maxiumum enabled size of a packet, in bytes
		maxPacketSize = config.get("maxPacketSize", maxPacketSize);

		// UDP discovery ("zero config" mode)
		multicastHost = config.get("multicastHost", multicastHost);
		multicastPort = config.get("multicastPort", multicastPort);
		multicastPeriod = config.get("multicastPeriod", multicastPeriod);
	}

	// --- CONNECT ---

	/**
	 * Cancelable timer of gossiper
	 */
	protected volatile ScheduledFuture<?> timer;

	/**
	 * Socket reader
	 */
	protected TcpReader reader;

	/**
	 * Socket writer
	 */
	protected TcpWriter writer;

	/**
	 * UDP broadcaster
	 */
	protected UDPBroadcaster broadcaster;

	@Override
	public void connect() {
		try {

			// Create reader and writer
			disconnect();
			reader = new TcpReader(this);
			writer = new TcpWriter(this, scheduler);

			// Start reader and writer
			reader.connect();
			writer.connect();

			// Full TCP or "zero config" UDP mode?
			if (urls == null || urls.length == 0) {

				// TCP + UDP mode ("zero config")
				if (multicastPeriod < 10) {
					multicastPeriod = 10;
				}
				broadcaster = new UDPBroadcaster(nodeID, this, scheduler, monitor);
				broadcaster.connect();

			} else {

				// Full TCP mode
				long now = System.currentTimeMillis();
				for (String url : urls) {
					int i = url.indexOf("://");
					if (i > -1 && i < url.length() - 4) {
						url = url.substring(i + 3);
					}
					url = url.replace('/', ':');
					String[] parts = url.split(":");
					if (parts.length < 2) {
						logger.warn("Invalid URL format (" + url + ")!");
						continue;
					}
					String targetNodeID = parts[2];
					if (targetNodeID.equals(nodeID)) {
						continue;
					}
					int port = this.port;
					if (parts.length == 3) {
						port = Integer.parseInt(parts[1]);
					}

					// Add as offline node (without services block)
					Tree info = new Tree();
					info.put("sender", targetNodeID);
					info.put("when", 0);
					info.put("offlineSince", now);
					info.putObject("hostName", parts[0]);
					info.put("port", port);
					nodeInfos.put(targetNodeID, info);
				}
			}

			// Start gossiper
			if (gossipPeriod < 1) {
				gossipPeriod = 1;
			}
			timer = scheduler.scheduleWithFixedDelay(this::doGossiping, gossipPeriod, gossipPeriod, TimeUnit.SECONDS);

			// Ok, transporter started
			logger.info("TCP transporter server started on port #" + port + ".");

		} catch (Exception cause) {
			String msg = cause.getMessage();
			if (msg == null || msg.isEmpty()) {
				msg = "Unable to start TCP transporter!";
			} else if (!msg.endsWith("!") && !msg.endsWith(".")) {
				msg += "!";
			}
			logger.warn(msg);
			reconnect();
		}
	}

	// --- DISCONNECT ---

	protected void disconnect() {

		// Stop broadcaster
		if (broadcaster != null) {
			broadcaster.disconnect();
			broadcaster = null;
		}

		// Stop timer of the gossiper
		if (timer != null) {
			timer.cancel(false);
			timer = null;
		}

		// Close socket reader
		if (reader != null) {
			reader.disconnect();
			reader = null;
		}

		// Close socket writer
		if (writer != null) {
			writer.disconnect();
			writer = null;
		}
	}

	// --- RECONNECT ---

	protected void reconnect() {
		disconnect();
		logger.info("Trying to reconnect...");
		scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
	}

	// --- STOP TRANSPORTER ---

	/**
	 * Closes transporter.
	 */
	@Override
	public void stop() {

		// Stop timers
		super.stop();

		// Disconnect
		disconnect();
	}

	// --- MESSAGE RECEIVED ---

	protected final AtomicBoolean processingGossipPacket = new AtomicBoolean();

	public void received(byte packetID, byte[] packet) {
		executor.execute(() -> {

			// Parse message
			Tree data;
			try {
				data = serializer.read(packet);
			} catch (Exception cause) {
				logger.warn("Unable to parse incoming message!", cause);
				return;
			}

			// Debug
			if (debug) {
				logger.info("Message received from channel #" + packetID + ":\r\n" + data.toString());
			}

			// Send message to proper component
			try {

				switch (packetID) {
				case PACKET_EVENT_ID:

					// Incoming event
					eventbus.receiveEvent(data);
					return;

				case PACKET_REQUEST_ID:

					// Incoming request
					registry.receiveRequest(data);
					return;

				case PACKET_RESPONSE_ID:

					// Incoming response
					registry.receiveResponse(data);
					return;

				case PACKET_PING_ID:

					// Not implemented
					return;

				case PACKET_PONG_ID:

					// Not implemented
					return;

				case PACKET_GOSSIP_ID:

					// Process (or ignore) incoming gossip packet
					if (processingGossipPacket.compareAndSet(false, true)) {
						try {
							String sender;
							for (Tree info : data.get("nodes")) {
								sender = info.get("sender", (String) null);
								updateLocalInfos(sender, info);
							}
						} finally {
							processingGossipPacket.set(false);
						}
					}
					return;

				default:
					logger.warn("Unsupported message ID (" + packetID + ")!");
				}

			} catch (Exception cause) {
				logger.warn("Unable to process incoming message!", cause);
			}
		});
	}

	// --- CONNECTION ERROR ---

	public void unableToSend(SendBuffer buffer, Throwable error) {
		if (buffer != null) {
			executor.execute(() -> {
				try {

					// Debug
					if (debug) {
						logger.info("Unable to send message to " + buffer.host + ":" + buffer.port + ".");
					}

					// Mark endpoint as offline
					Tree info = nodeInfos.get(buffer.nodeID);
					long now = System.currentTimeMillis();
					if (info == null) {
						info = new Tree();
						info.put("sender", buffer.nodeID);
						info.put("when", now);
						info.put("offlineSince", now);
						info.putObject("hostName", buffer.host);
						info.putObject("ipList", Collections.singletonList(buffer.host));
						info.put("port", buffer.port);
						updateLocalInfos(buffer.nodeID, info);
					} else {
						Tree copy = info.clone();
						copy.put("when", now);
						Tree offline = copy.get("offlineSince");
						if (offline == null) {
							copy.put("offlineSince", now);
						}
						updateLocalInfos(buffer.nodeID, copy);
					}

					// Remove header
					byte[] packet = buffer.getCurrentPacket();
					if (packet != null && packet.length > 6) {
						byte[] copy = new byte[packet.length - 6];
						System.arraycopy(packet, 6, copy, 0, copy.length);

						// Deserialize packet
						Tree message = serializer.read(copy);

						// Get request's unique ID
						String id = message.get("id", (String) null);
						if (id == null || id.isEmpty()) {

							// Not a request
							return;
						}

						// Create response message
						Tree response = new Tree();
						response.put("id", id);
						response.put("ver", ServiceBroker.PROTOCOL_VERSION);
						response.put("success", false);
						response.put("data", (String) null);
						if (error != null) {

							// Add message
							Tree errorMap = response.putMap("error");
							errorMap.put("message", error.getMessage());

							// Add trace
							StringWriter sw = new StringWriter(128);
							PrintWriter pw = new PrintWriter(sw);
							error.printStackTrace(pw);
							errorMap.put("trace", sw.toString());
						}

						// Send error response back to the source
						registry.receiveResponse(response);
					}
				} catch (Exception cause) {
					logger.warn("Unable to handle error!", cause);
				}
			});
		}
	}

	// --- SUBSCRIBE (UNUSED) ---

	@Override
	public Promise subscribe(String channel) {
		throw new UnsupportedOperationException();
	}

	// --- PUBLISH ---

	@Override
	public void publish(String channel, Tree message) {
		if (writer != null) {
			byte[] packet = null;
			try {

				// Parse channel
				int s = channel.indexOf('.');
				if (s == -1) {
					logger.warn("Invalid channel syntax (" + channel + ")!");
					return;
				}
				int e = channel.indexOf('.', s + 1);
				if (e == -1) {
					logger.warn("Broadcast messaging isn't supported (" + channel + ")!");
					return;

				}
				String command = channel.substring(s + 1, e);
				String nodeID = channel.substring(e + 1);

				// Switch by packet type
				byte packetID;
				switch (command) {
				case PACKET_EVENT:
					packetID = PACKET_EVENT_ID;
					break;
				case PACKET_REQUEST:
					packetID = PACKET_REQUEST_ID;
					break;
				case PACKET_RESPONSE:
					packetID = PACKET_RESPONSE_ID;
					break;
				case PACKET_PING:
					packetID = PACKET_PING_ID;
					break;
				case PACKET_PONG:
					packetID = PACKET_PONG_ID;
					break;
				default:
					logger.warn("Unsupported command (" + command + ")!");
					return;
				}

				// Create data packet to send
				packet = serialize(packetID, message);

				// Check size
				if (maxPacketSize > 0 && packet.length > maxPacketSize) {
					throw new Exception("Outgoing packet is larger than the \"maxPacketSize\" limit (" + packet.length
							+ " > " + maxPacketSize + ")!");
				}

				// Send packet to endpoint
				Tree info = nodeInfos.get(nodeID);
				if (info == null) {
					logger.warn("Unknown node ID (" + nodeID + ")!");
					return;
				}
				writer.send(nodeID, info, packet);

			} catch (Exception cause) {
				logger.warn("Unable to send message!", cause);
			}
		}
	}

	protected byte[] serialize(byte packetID, Tree message) throws Exception {
		byte[] data = serializer.write(message);
		byte[] packet = new byte[data.length + 6];

		// 6. byte is the packet type (from 1 to 6)
		packet[5] = packetID;

		// 2.-5. bytes are the length of the packet
		packet[4] = (byte) packet.length;
		packet[3] = (byte) (packet.length >>> 8);
		packet[2] = (byte) (packet.length >>> 16);
		packet[1] = (byte) (packet.length >>> 24);

		// First byte = Header's CRC (XOR)
		packet[0] = (byte) (packet[1] ^ packet[2] ^ packet[3] ^ packet[4] ^ packet[5]);

		// Add data block
		System.arraycopy(data, 0, packet, 6, data.length);
		return packet;
	}

	// --- GOSSIPING ---

	protected Random rnd = new Random();

	protected void doGossiping() {
		try {

			// Create gossip message
			Collection<Tree> values = nodeInfos.values();
			int size = values.size();
			if (size == 0) {
				return;
			}
			Tree root = new Tree();
			root.put("ver", ServiceBroker.PROTOCOL_VERSION);
			root.put("sender", nodeID);
			Tree nodes = root.putList("nodes");

			// Add current node to message
			Tree descriptor = registry.generateDescriptor();
			descriptor.put("port", port);
			descriptor.put("cpu", monitor.getTotalCpuPercent());
			nodes.addObject(descriptor);

			// Add node infos to message
			ArrayList<Tree> liveEndpoints = new ArrayList<>(size);
			ArrayList<Tree> unreachableEndpoints = new ArrayList<>(size);
			for (Tree info : nodeInfos.values()) {
				if (info.get("when", 0L) > 0) {
					nodes.addObject(info);
				}
				if (info.get("offlineSince") != null) {
					unreachableEndpoints.add(info);
				} else {
					liveEndpoints.add(info);
				}
			}
			int liveEndpointCount = liveEndpoints.size();
			int unreachableEndpointCount = unreachableEndpoints.size();

			// Create gossip packet
			byte[] packet = serialize(PACKET_GOSSIP_ID, root);

			// Do gossiping with a live endpoint
			if (liveEndpointCount > 0) {
				sendGossipToRandomEndpoint(liveEndpoints, liveEndpointCount, packet);
			}

			// Do gossiping with a unreachable endpoint
			if (unreachableEndpointCount > 0) {

				// 10 nodes:
				// 1 dead / (9 live + 1) = 0.10
				// 5 dead / (5 live + 1) = 0.83
				// 9 dead / (1 live + 1) = 4.50
				double ratio = unreachableEndpointCount / (liveEndpointCount + 1);

				// Random number between 0.0 and 1.0
				double random = rnd.nextDouble();
				if (random < ratio) {
					sendGossipToRandomEndpoint(unreachableEndpoints, unreachableEndpointCount, packet);
				}
			}

		} catch (Exception cause) {
			logger.error("Unable to send gossip message to peer!", cause);
		}
	}

	protected void sendGossipToRandomEndpoint(ArrayList<Tree> endpoints, int size, byte[] packet) {
		for (int i = 0; i < size; i++) {

			// Choose random endpoint
			int index = rnd.nextInt(size);
			Tree info = endpoints.get(index);

			// Send data
			String targetNodeID = info.get("sender", (String) null);
			if (nodeID.equals(targetNodeID)) {
				continue;
			}

			// Send gossip message to node
			writer.send(targetNodeID, info, packet);
			break;
		}
	}

	// --- UDP BROADCAST MESSAGE RECEIVEd ---

	public void udpPacketReceiver(String nodeID, int cpu, String host, int port) {

		// Debug
		if (debug) {
			logger.info("UDP message received (node ID: " + nodeID + ", CPU usage: " + cpu + ").");
		}

		// Store data
		long now = System.currentTimeMillis();
		if (nodeInfos.containsKey(nodeID)) {
			nodeActivities.put(nodeID, new NodeActivity(now, cpu));
		} else {
			Tree info = new Tree();
			info.put("sender", nodeID);
			info.put("when", 0);
			info.put("offlineSince", now);
			info.putObject("hostName", host);
			info.put("port", port);
			nodeInfos.put(nodeID, info);
			logger.info("Node \"" + nodeID + "\" registered.");
		}
	}

	// --- GETTERS AND SETTERS ---

	public String[] getUrls() {
		return urls;
	}

	public void setUrls(String[] urls) {
		this.urls = urls;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getGossipPeriod() {
		return gossipPeriod;
	}

	public void setGossipPeriod(int gossipPeriod) {
		this.gossipPeriod = gossipPeriod;
	}

	public int getMaxKeepAliveConnections() {
		return maxKeepAliveConnections;
	}

	public void setMaxKeepAliveConnections(int maxKeepAliveConnections) {
		this.maxKeepAliveConnections = maxKeepAliveConnections;
	}

	public int getKeepAliveTimeout() {
		return keepAliveTimeout;
	}

	public void setKeepAliveTimeout(int maxKeepAliveTimeout) {
		this.keepAliveTimeout = maxKeepAliveTimeout;
	}

	public int getMaxPacketSize() {
		return maxPacketSize;
	}

	public void setMaxPacketSize(int maxPacketSize) {
		this.maxPacketSize = maxPacketSize;
	}

	public String getMulticastHost() {
		return multicastHost;
	}

	public void setMulticastHost(String broadcastHost) {
		this.multicastHost = broadcastHost;
	}

	public int getMulticastPort() {
		return multicastPort;
	}

	public void setMulticastPort(int broadcastPort) {
		this.multicastPort = broadcastPort;
	}

	public int getMulticastPeriod() {
		return multicastPeriod;
	}

	public void setMulticastPeriod(int broadcastPeriod) {
		this.multicastPeriod = broadcastPeriod;
	}

}