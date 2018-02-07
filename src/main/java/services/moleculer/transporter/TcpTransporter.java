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

import static services.moleculer.util.CommonUtils.getHostName;
import static services.moleculer.util.CommonUtils.parseURLs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.service.CpuUsage;
import services.moleculer.service.Name;
import services.moleculer.service.NodeDescriptor;
import services.moleculer.service.NodeStatus;
import services.moleculer.transporter.tcp.TcpReader;
import services.moleculer.transporter.tcp.TcpWriter;
import services.moleculer.transporter.tcp.UDPBroadcaster;

/**
 * TCP Transporter with optional UDP discovery ("zero configuration") module.
 * <br>
 * <br>
 * TCP Transporter uses fault tolerant and peer-to-peer <b>Gossip Protocol</b>
 * to discover location and service information about the other nodes
 * participating in a Moleculer Cluster. In Moleculer's P2P architecture all
 * nodes are equal, there is no "leader" or "controller" node, so the cluster is
 * truly horizontally scalable. This transporter aims to run on top of an
 * infrastructure of hundreds of nodes.<br>
 * <br>
 * Nodes can explore each other in two ways. With or without UDP packets. If the
 * URLs of all nodes specified in startup parameters (in
 * "tcp://host:port/nodeID" format), TCP Transporter will work without UDP.
 * Otherwise the TCP Transporter starts an UDP server, and the Moleculer nodes
 * detect each other with UDP packets. Events and function calls always go
 * through TCP channels.<br>
 * <br>
 * TCP Transporter provides the <b>highest speed</b> data transfer between the
 * nodes (eg. hundred thousand packets per second can be transmitted from one
 * node to another over a high-speed LAN).
 */
@Name("TCP Transporter")
public class TcpTransporter extends Transporter {

	// --- PACKET IDS ---

	protected static final byte PACKET_EVENT_ID = 1;
	protected static final byte PACKET_REQUEST_ID = 2;
	protected static final byte PACKET_RESPONSE_ID = 3;
	protected static final byte PACKET_PING_ID = 4;
	protected static final byte PACKET_PONG_ID = 5;
	protected static final byte PACKET_GOSSIP_REQ_ID = 6;
	protected static final byte PACKET_GOSSIP_RSP_ID = 7;
	protected static final byte PACKET_GOSSIP_HELLO_ID = 8;

	// --- PROPERTIES ---

	/**
	 * TCP port (used by the Transporter and Gossiper services). A port number
	 * of zero will let the system pick up an ephemeral port in a bind
	 * operation.
	 */
	protected int port = 0;

	/**
	 * Gossiping period time, in SECONDS.
	 */
	protected int gossipPeriod = 1;

	/**
	 * Max number of keep-alive connections (-1 = unlimited, 0 = disable
	 * keep-alive connections).
	 */
	protected int maxConnections = 32;

	/**
	 * Max enable packet size (BYTES).
	 */
	protected int maxPacketSize = 1024 * 1024 * 16;

	/**
	 * List of URLs ("tcp://host:port/nodeID" or "host:port/nodeID" or
	 * "host/nodeID"), when UDP discovery is disabled.
	 */
	protected String[] urls = {};

	/**
	 * UDP multicast host of automatic discovery service ("zero config" mode).
	 */
	protected String multicastHost = "230.0.0.0";

	/**
	 * UDP multicast port of automatic discovery service.
	 */
	protected int multicastPort = 4445;

	/**
	 * UDP multicast period in SECONDS.
	 */
	protected int multicastPeriod = 60;

	/**
	 * Maximum number of outgoing multicast packets (0 = runs forever)
	 */
	protected int multicastPackets;

	// --- CONSTUCTORS ---

	/**
	 * Start TCP Transporter in "zero config" mode, with automatic UDP service
	 * discovery.
	 */
	public TcpTransporter() {
	}

	/**
	 * Start TCP Transporter in full TCP mode, without UDP discovery. Valid URL
	 * syntax is "tcp://host:port/nodeID" or "host:port/nodeID".
	 */
	public TcpTransporter(String... urls) {
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

		// Disable offline timeout when use host list
		if (urls != null && urls.length > 0) {
			offlineTimeout = 0;
		} else if (offlineTimeout > 0 && offlineTimeout < 15) {
			offlineTimeout = 15;
		}

		// Process basic properties (eg. "prefix")
		super.start(broker, config);

		// TCP server's port
		port = config.get("port", port);

		// Gossiper's gossiping period in seconds
		gossipPeriod = config.get("gossipPeriod", gossipPeriod);

		// TCP socket properties
		maxConnections = config.get("maxConnections", maxConnections);

		// Maxiumum enabled size of a packet, in bytes
		maxPacketSize = config.get("maxPacketSize", maxPacketSize);

		// UDP discovery ("zero config" mode)
		multicastHost = config.get("multicastHost", multicastHost);
		multicastPort = config.get("multicastPort", multicastPort);
		multicastPeriod = config.get("multicastPeriod", multicastPeriod);
		multicastPackets = config.get("multicastPackets", multicastPackets);

		// Parse URLs (in "full TCP mode")
		urls = parseURLs(config, urls);
		if (urls != null && urls.length > 0) {
			for (String url : urls) {
				int i = url.indexOf("://");
				if (i > -1 && i < url.length() - 4) {
					url = url.substring(i + 3);
				}
				url = url.replace('/', ':');
				String[] parts = url.split(":");
				if (parts.length < 3) {
					logger.warn("Invalid URL format (" + url
							+ ")! Valid syntax is \"tcp://host:port/nodeID\" or \"host:port/nodeID\"!");
					continue;
				}
				String sender = parts[2];
				if (sender.equals(nodeID)) {
					continue;
				}
				int port;
				try {
					port = Integer.parseInt(parts[1]);
				} catch (Exception e) {
					logger.warn("Invalid URL format (" + url
							+ ")! Valid syntax is \"tcp://host:port/nodeID\" or \"host:port/nodeID\"!");
					continue;
				}
				String host = parts[0];
				nodes.put(sender, new NodeDescriptor(sender, host, port));
			}
		}
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

	/**
	 * Current TCP port
	 */
	protected int currentPort;

	@Override
	public void connect() {
		try {

			// Create reader and writer
			disconnect();
			reader = new TcpReader(this);
			writer = new TcpWriter(this);

			// Start TCP server
			reader.connect();
			currentPort = reader.getCurrentPort();

			// Start data writer (TCP client)
			writer.connect();

			// TCP + UDP mode ("zero config")
			if (urls == null || urls.length == 0) {
				broadcaster = new UDPBroadcaster(namespace, nodeID, this, scheduler, preferHostname);
				broadcaster.connect();
			}

			// Start gossiper
			timer = scheduler.scheduleWithFixedDelay(this::sendGossipRequest, gossipPeriod, gossipPeriod,
					TimeUnit.SECONDS);

			// Start timers
			connected();

			// Ok, transporter started
			logger.info("Message receiver started on tcp://" + getHostName() + ':' + currentPort + ".");

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

		// Stop gossiper's timer
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

			// Send message to proper component
			try {

				switch (packetID) {
				case PACKET_EVENT_ID:

					// Incoming event
					if (debug) {
						logger.info("Event message received:\r\n" + data);
					}
					eventbus.receiveEvent(data);
					return;

				case PACKET_REQUEST_ID:

					// Incoming request
					if (debug) {
						logger.info("Request message received:\r\n" + data);
					}
					registry.receiveRequest(data);
					return;

				case PACKET_RESPONSE_ID:

					// Incoming response
					if (debug) {
						logger.info("Response message received:\r\n" + data);
					}
					registry.receiveResponse(data);
					return;

				case PACKET_PING_ID:

					// Not implemented
					if (debug) {
						logger.info("Ping message received:\r\n" + data);
					}
					return;

				case PACKET_PONG_ID:

					// Not implemented
					if (debug) {
						logger.info("Pong message received:\r\n" + data);
					}
					return;

				case PACKET_GOSSIP_REQ_ID:

					// Incoming gossip request
					processGossipRequest(data);
					return;

				case PACKET_GOSSIP_RSP_ID:

					// Incoming gossip request
					processGossipResponse(data);
					return;

				case PACKET_GOSSIP_HELLO_ID:

					// Incoming "hello" message
					processGossipHello(data);
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

	public void unableToSend(String nodeID, byte[] packet, Throwable cause) {
		if (nodeID != null && packet != null) {
			executor.execute(() -> {
				try {

					// Debug
					if (debug) {
						logger.warn("Unable to send message to \"" + nodeID + "\".", cause);
					}

					// Mark endpoint as offline
					NodeDescriptor node = nodes.get(nodeID);
					if (node != null && node.switchToOffline()) {

						// Remove actions and listeners
						registry.removeActions(nodeID);
						eventbus.removeListeners(nodeID);
						writer.close(node.nodeID);

						// Notify listeners (unexpected disconnection)
						logger.info("Node \"" + nodeID + "\" disconnected.");
						broadcastNodeDisconnected(node.info, true);
					}

					// Remove header
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
						response.put("sender", nodeID);
						response.put("success", false);
						response.put("data", (String) null);
						if (cause != null) {

							// Add message
							Tree errorMap = response.putMap("error");
							errorMap.put("message", cause.getMessage());

							// Add trace
							StringWriter sw = new StringWriter(128);
							PrintWriter pw = new PrintWriter(sw);
							cause.printStackTrace(pw);
							errorMap.put("trace", sw.toString());
						}

						// Send error response back to the source
						registry.receiveResponse(response);
					}
				} catch (Exception error) {
					logger.warn("Unable to handle error!", error);
				}
			});
		}
	}

	// --- STORE CPU USAGE ---

	@Override
	protected void sendHeartbeatPacket() {
		registry.getDescriptor().setCpuUsage(monitor.getTotalCpuPercent());
	}

	// --- SEND DISCONNECT (UNUSED) ---
	
	@Override
	protected void sendDisconnectPacket() {
	}
	
	// --- SUBSCRIBE (UNUSED) ---

	@Override
	public Promise subscribe(String channel) {
		return Promise.resolve();
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

					// Broadcast messaging is not supported
					return;

				}
				String command = channel.substring(s + 1, e);
				String nodeID = channel.substring(e + 1);

				// Switch by packet type
				byte packetID;
				switch (command) {
				case PACKET_EVENT:
					if (debug) {
						logger.info("Event message submitting:\r\n" + message);
					}
					packetID = PACKET_EVENT_ID;
					break;
				case PACKET_REQUEST:
					if (debug) {
						logger.info("Request message submitting:\r\n" + message);
					}
					packetID = PACKET_REQUEST_ID;
					break;
				case PACKET_RESPONSE:
					if (debug) {
						logger.info("Response message submitting:\r\n" + message);
					}
					packetID = PACKET_RESPONSE_ID;
					break;
				case PACKET_PING:
					if (debug) {
						logger.info("Ping message submitting:\r\n" + message);
					}
					packetID = PACKET_PING_ID;
					break;
				case PACKET_PONG:
					if (debug) {
						logger.info("Pong message submitting:\r\n" + message);
					}
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
				writer.send(nodeID, packet);

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

	// --- TIMEOUT PROCESS ---

	@Override
	protected synchronized void checkTimeouts() {

		// Check offline timeout
		if (offlineTimeout < 1)
			return;
		
		long now = System.currentTimeMillis();
		long offlineTimeoutMillis = offlineTimeout * 1000L;
		NodeDescriptor node;
		Iterator<NodeDescriptor> i = nodes.values().iterator();
		while (i.hasNext()) {
			node = i.next();
			if (now - node.getOfflineSince(Long.MAX_VALUE) > offlineTimeoutMillis) {

				// Remove node from Map
				i.remove();
				if (writer != null) {
					writer.close(node.nodeID);
				}
				logger.info("Node \"" + node.nodeID + "\" is no longer registered because it was inactive for "
						+ offlineTimeout + " seconds.");
			}
		}
	}

	// --- UDP MULTICAST MESSAGE RECEIVED ---

	public void udpPacketReceived(String sender, String host, int port) {

		// Debug
		if (debug) {
			logger.info("Discovery message received from \"" + sender + "\" node (host: " + host + ", port: " + port
					+ ").");
		}

		// Register as offline node (if unknown)
		registerAsNewNode(sender, host, port);
	}

	// --- GOSSIP HELLO MESSAGE RECEIVED ---

	protected void processGossipHello(Tree data) {

		// Debug
		String sender = data.get("sender", (String) null);
		if (debug) {
			logger.info("Gossip \"hello\" received from \"" + sender + "\" node:\r\n" + data);
		}

		// Get previous parameters
		String host = data.get("host", "unknown");
		int port = data.get("port", 0);

		// Register as offline node (if unknown)
		registerAsNewNode(sender, host, port);
	}

	protected synchronized void registerAsNewNode(String sender, String host, int port) {

		// Check node
		if (nodeID.equals(sender)) {
			return;
		}
		NodeDescriptor prevNode = nodes.get(sender);
		if (prevNode == null) {

			// Add as new, offline node
			nodes.put(sender, new NodeDescriptor(sender, host, port));
			logger.info("Node \"" + sender + "\" registered.");
		} else {

			// Check hostname and port
			if (!prevNode.host.equalsIgnoreCase(host) || prevNode.port != port) {

				// Host or port number changed -> reregister as offline,
				// and keep the original info block
				if (prevNode.info == null) {
					nodes.put(sender, new NodeDescriptor(sender, host, port));
				} else {
					Tree newInfo = prevNode.info.clone();
					if (preferHostname) {
						newInfo.put("hostname", host);
					} else {
						Tree ipList = newInfo.get("ipList");
						if (ipList == null) {
							ipList = newInfo.putList("ipList");
						} else {
							ipList.clear();
						}
						ipList.add(host);
					}
					newInfo.put("port", port);
					newInfo.put("seq",  prevNode.getSequence());
					nodes.put(sender, new NodeDescriptor(newInfo, preferHostname, false, null, true));
					writer.close(sender);
				}
			}
		}
	}

	// --- SEND GOSSIP REQUEST TO RANDOM NODES ---

	/**
	 * Random generator.
	 */
	protected Random rnd = new Random();

	/**
	 * Create and send a Gossip request packet.
	 */
	protected void sendGossipRequest() {
		try {

			// Are we alone?
			if (nodes.isEmpty()) {
				return;
			}

			// Create gossip request
			Tree root = new Tree();
			root.put("ver", ServiceBroker.PROTOCOL_VERSION);
			root.put("sender", nodeID);

			// Add "online" and "offline" blocks
			Tree online = root.putMap("online");
			Tree offline = root.putMap("offline");

			// Add current node
			NodeDescriptor self = registry.getDescriptor();
			Tree thisNode = online.putList(nodeID);
			thisNode.add(self.getSequence());
			CpuUsage c = self.getCpuUsage();
			thisNode.add(c.sequence).add(c.value);

			// Separate online and offline nodes
			int size = nodes.size() * 2;
			String[] liveEndpoints = new String[size];
			String[] unreachableEndpoints = new String[size];

			int liveEndpointCount = 0;
			int unreachableEndpointCount = 0;

			// Loop on registered nodes
			for (NodeDescriptor node : nodes.values()) {
				NodeStatus status = node.getStatus();
				if (status.offlineSince > 0) {

					// Offline
					if (unreachableEndpointCount < unreachableEndpoints.length) {
						unreachableEndpoints[unreachableEndpointCount++] = node.nodeID;
					}
					if (status.sequence > 0) {
						offline.put(node.nodeID, status.sequence);
					}
				} else {
					if (!node.local) {

						// Online
						if (liveEndpointCount < liveEndpoints.length) {
							liveEndpoints[liveEndpointCount++] = node.nodeID;
						}
						if (status.sequence > 0) {
							c = node.getCpuUsage();
							online.putList(node.nodeID).add(node.getSequence()).add(c.sequence).add(c.value);
						}
					}
				}
			}

			// Remove empty "offline" node
			if (offline.isEmpty()) {
				offline.remove();
			}

			// Serialize gossip packet (JSON, MessagePack, etc.)
			byte[] packet = serialize(PACKET_GOSSIP_REQ_ID, root);

			// Do gossiping with a live endpoint
			if (liveEndpointCount > 0) {
				sendGossipToRandomEndpoint(liveEndpoints, liveEndpointCount, packet, root);
			}

			// Do gossiping with a unreachable endpoint
			if (unreachableEndpointCount > 0) {

				// 10 nodes:
				// 1 offline / (9 online + 1) = 0.10
				// 3 offline / (7 online + 1) = 0.37
				// 5 offline / (5 online + 1) = 0.83
				// 9 offline / (1 online + 1) = 4.50
				double ratio = (double) unreachableEndpointCount / ((double) liveEndpointCount + 1);

				// Random number between 0.0 and 1.0
				double random = rnd.nextDouble();
				if (random < ratio) {
					sendGossipToRandomEndpoint(unreachableEndpoints, unreachableEndpointCount, packet, root);
				}
			}

		} catch (Exception cause) {
			logger.error("Unable to send gossip message to peer!", cause);
		}
	}

	protected void sendGossipToRandomEndpoint(String[] endpoints, int size, byte[] packet, Tree message) {

		// Choose a random endpoint
		String nodeID;
		if (endpoints.length == 1) {
			nodeID = endpoints[0];
		} else {
			nodeID = endpoints[rnd.nextInt(size)];
		}

		// Debug
		if (debug) {
			logger.info("Gossip request submitting to \"" + nodeID + "\" node:\r\n" + message);
		}

		// Send gossip request to node
		writer.send(nodeID, packet);
	}

	// --- GOSSIP REQUEST MESSAGE RECEIVED ---

	protected final AtomicLong lastRequest = new AtomicLong();

	protected synchronized void processGossipRequest(Tree data) throws Exception {

		// Debug
		String sender = data.get("sender", (String) null);
		if (debug) {
			logger.info("Gossip request received from \"" + sender + "\" node:\r\n" + data);
		}

		// Create gossip response
		Tree root = new Tree();
		root.put("ver", ServiceBroker.PROTOCOL_VERSION);
		root.put("sender", this.nodeID);

		Tree onlineRsp = root.putMap("online");
		Tree offlineRsp = root.putMap("offline");

		// Online / offline nodes in request
		Tree onlineReq = data.get("online");
		Tree offlineReq = data.get("offline");

		// Loop in nodes
		LinkedList<NodeDescriptor> allNodes = new LinkedList<>(nodes.values());
		allNodes.add(registry.getDescriptor());
		for (NodeDescriptor node : allNodes) {
			Tree online = onlineReq == null ? null : onlineReq.get(node.nodeID);
			Tree offline = offlineReq == null ? null : offlineReq.get(node.nodeID);

			// Online or offline sequence number
			long seq = 0;

			// CPU data
			long cpuSeq = 0;
			int cpu = 0;

			if (offline != null) {
				if (!offline.isPrimitive()) {
					logger.warn("Invalid \"offline\" block: " + offline.toString(false));
					continue;
				}
				seq = offline.asLong();
			} else if (online != null) {
				if (!online.isEnumeration() || online.size() != 3) {
					logger.warn("Invalid \"online\" block: " + online.toString(false));
					continue;
				}
				seq = online.get(0).asLong();
				cpuSeq = online.get(1).asLong();
				cpu = online.get(2).asInteger();
			}

			NodeStatus status = node.getStatus();
			if (seq == 0 || seq < status.sequence) {

				// We have newer info or requester doesn't know it
				if (status.offlineSince == 0) {
					Tree row = onlineRsp.putList(node.nodeID);
					row.addObject(node.info);
					CpuUsage c = node.getCpuUsage();
					if (c.sequence > 0) {
						row.add(c.sequence).add(c.value);
					}
				} else {
					offlineRsp.put(node.nodeID, status.sequence);
				}
			}

			if (offline != null) {

				// Requester said it is OFFLINE
				if (status.offlineSince > 0) {

					// We also knew it as offline
					node.switchToOffline(seq);
					continue;
				}
				if (!node.local) {

					// We know it is online, so we change it to offline
					if (status.offlineSince == 0 || node.switchToOffline(seq)) {

						// Remove remote actions and listeners
						registry.removeActions(node.nodeID);
						eventbus.removeListeners(node.nodeID);
						writer.close(node.nodeID);

						// Notify listeners (not unexpected disconnection)
						logger.info("Node \"" + node.nodeID + "\" disconnected.");
						broadcastNodeDisconnected(node.info, false);
					}
					continue;
				}

				// We send back that we are online
				// Update to a newer 'when' if my is older
				if (seq >= status.sequence) {
					registry.incrementSequence(seq);
					NodeDescriptor self = registry.getDescriptor();
					Tree row = onlineRsp.putList(node.nodeID);
					row.addObject(self.info);
					CpuUsage c = self.getCpuUsage();
					if (c.sequence > 0) {
						row.add(c.sequence).add(c.value);
					}
				}
			} else if (online != null) {

				// Requester said it is ONLINE
				if (status.offlineSince == 0) {
					CpuUsage c = node.getCpuUsage();

					// We update our CPU info
					node.setCpuUsage(cpuSeq, cpu);

					if (cpuSeq < c.sequence) {

						// We have newer CPU value, send back
						onlineRsp.putList(node.nodeID).add(c.sequence).add(c.value);
					}
				} else {

					// We knew it as offline. We do nothing, because we'll
					// request it and we'll receive its INFO
					continue;
				}
			}
		}

		// Remove empty blocks
		boolean emptyOnlineBlock = onlineRsp.isEmpty();
		boolean emptyOfflineBlock = offlineRsp.isEmpty();
		if (emptyOnlineBlock && emptyOfflineBlock) {

			// Message is empty
			return;
		}
		if (emptyOnlineBlock) {
			onlineRsp.remove();
		}
		if (emptyOfflineBlock) {
			offlineRsp.remove();
		}

		// Debug
		if (debug) {
			logger.info("Gossip response submitting to \"" + sender + "\" node:\r\n" + root);
		}

		// Serialize response
		byte[] packet = serialize(PACKET_GOSSIP_RSP_ID, root);

		// Send response
		writer.send(sender, packet);
	}

	// --- GOSSIP RESPONSE MESSAGE RECEIVED ---

	protected synchronized void processGossipResponse(Tree data) throws Exception {

		// Debug
		if (debug) {
			logger.info("Gossip response received from \"" + data.get("sender", "unknown") + "\" node:\r\n" + data);
		}

		// Online / offline nodes in responnse
		Tree online = data.get("online");
		Tree offline = data.get("offline");

		// Process "online" block
		if (online != null) {
			for (Tree row : online) {

				// Get nodeID
				String nodeID = row.getName();
				if (this.nodeID.equals(nodeID)) {
					continue;
				}
				int size = row.size();
				if (!row.isEnumeration() || size < 1 || size > 3) {
					logger.warn("Invalid \"offline\" block: " + row);
					continue;
				}

				// Get parameters from input
				Tree info = null;
				long cpuSeq = 0;
				int cpu = 0;

				if (row.size() == 1) {
					info = row.get(0);
				} else if (row.size() == 2) {
					cpuSeq = row.get(0).asLong();
					cpu = row.get(1).asInteger();
				} else if (row.size() == 3) {
					info = row.get(0);
					cpuSeq = row.get(1).asLong();
					cpu = row.get(2).asInteger();
				} else {
					logger.warn("Invalid \"online\" block: " + row.toString(false));
					continue;
				}

				if (info != null) {
					info.put("sender", nodeID);
					// Update "info" block,
					// send updated, connected or reconnected event
					updateNodeInfo(nodeID, info);
				}
				if (cpuSeq > 0) {

					// We update our CPU info
					NodeDescriptor node = nodes.get(nodeID);
					if (node != null) {
						node.setCpuUsage(cpuSeq, cpu);
					}
				}
			}
		}

		// Process "offline" block
		if (offline != null) {
			for (Tree row : offline) {
				String nodeID = row.getName();

				NodeDescriptor node = nodes.get(nodeID);
				if (node == null || node.local) {
					return;
				}

				if (!row.isPrimitive()) {
					logger.warn("Invalid \"offline\" block: " + row);
					continue;
				}

				// Get parameters from input
				long seq = row.asLong();

				if (node.getSequence() < seq && node.switchToOffline(seq)) {

					// We know it is online, so we change it to offline
					// Remove remote actions and listeners
					registry.removeActions(node.nodeID);
					eventbus.removeListeners(node.nodeID);
					writer.close(node.nodeID);

					// Notify listeners (not unexpected disconnection)
					logger.info("Node \"" + node.nodeID + "\" disconnected.");
					broadcastNodeDisconnected(node.info, false);
				}
			}
		}
	}

	// --- GOSSIP HELLO MESSAGE ---

	protected byte[] cachedHelloMessage;

	/**
	 * Create Gossip HELLO packet. Hello message is invariable, so we can cache
	 * it.
	 */
	public byte[] generateGossipHello() throws Exception {
		if (cachedHelloMessage != null) {
			return cachedHelloMessage;
		}
		Tree root = new Tree();
		root.put("ver", ServiceBroker.PROTOCOL_VERSION);
		root.put("sender", nodeID);
		NodeDescriptor node = registry.getDescriptor();
		root.put("host", node.host);
		root.put("port", node.port);
		cachedHelloMessage = serialize(PACKET_GOSSIP_HELLO_ID, root);
		return cachedHelloMessage;
	}

	// --- UNUSED METHODS ---

	public void setHeartbeatInterval(int heartbeatInterval) {
		throw new UnsupportedOperationException();
	}

	public void setHeartbeatTimeout(int heartbeatTimeout) {
		throw new UnsupportedOperationException();
	}

	// --- GETTERS AND SETTERS ---

	public String[] getUrls() {
		return urls;
	}

	public void setUrls(String... urls) {
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

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
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

	public int getCurrentPort() {
		return currentPort;
	}

	public int getMulticastPackets() {
		return multicastPackets;
	}

	public void setMulticastPackets(int multicastPackets) {
		this.multicastPackets = multicastPackets;
	}

}