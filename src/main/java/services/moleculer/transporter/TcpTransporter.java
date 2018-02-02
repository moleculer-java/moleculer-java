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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.monitor.Monitor;
import services.moleculer.service.Name;
import services.moleculer.transporter.tcp.NodeActivity;
import services.moleculer.transporter.tcp.OfflineNode;
import services.moleculer.transporter.tcp.SendBuffer;
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
 * infrastructure of hundreds of nodes.
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
	 * Max number of keep-alive connections (0 = unlimited).
	 */
	protected int maxKeepAliveConnections;

	/**
	 * Keep-alive timeout in SECONDS (0 = no timeout).
	 */
	protected int keepAliveTimeout = 60;

	/**
	 * Max enable packet size (BYTES).
	 */
	protected int maxPacketSize = 1024 * 1024 * 64;

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

	/**
	 * Use hostnames instead of IP addresses As the DHCP environment is dynamic,
	 * any later attempt to use IPs instead hostnames would most likely yield
	 * false results. Therefore, use hostnames if you are using DHCP.
	 */
	protected boolean useHostname = true;

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

		// TCP transporter uses Gossip Protocol instead of HEARTBEAT signals
		heartbeatInterval = 0;
		heartbeatTimeout = 0;
		if (gossipPeriod < 1) {
			gossipPeriod = 1;
		}
		if (multicastPeriod < 10) {
			multicastPeriod = 10;
		}

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
		multicastPackets = config.get("multicastPackets", multicastPackets);

		// Use hostnames or IPs?
		useHostname = config.get("useHostname", useHostname);

		// Parse URLs (in "full TCP mode")
		urls = parseURLs(config, urls);
		if (urls != null && urls.length > 0) {
			writeLock.lock();
			try {
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
					String targetNodeID = parts[2];
					if (targetNodeID.equals(nodeID)) {
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

					// Add as offline node
					registerAsOffline(targetNodeID, parts[0], port);
				}
			} finally {
				writeLock.unlock();
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
			writer = new TcpWriter(this, scheduler);

			// Start TCP server
			reader.connect();
			currentPort = reader.getCurrentPort();

			// Start data writer (TCP client)
			writer.connect();

			// TCP + UDP mode ("zero config")
			if (urls == null || urls.length == 0) {
				broadcaster = new UDPBroadcaster(namespace, nodeID, this, scheduler);
				broadcaster.connect();
			}

			// Start gossiper
			timer = scheduler.scheduleWithFixedDelay(this::doGossiping, gossipPeriod, gossipPeriod, TimeUnit.SECONDS);

			// Start offline timeout timer
			if (checkTimeoutTimer == null && offlineTimeout > 0) {
				checkTimeoutTimer = scheduler.scheduleAtFixedRate(this::checkOfflineTimeouts, offlineTimeout / 2,
						offlineTimeout / 2, TimeUnit.SECONDS);
			}

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
						logger.warn("Unable to send message to " + buffer.host + ":" + buffer.port + ".", error);
					}

					// Mark endpoint as offline
					long now = System.currentTimeMillis();
					Tree current = null;
					writeLock.lock();
					try {

						// Is currently offline?
						OfflineNode offlineNode = offlineNodes.get(buffer.nodeID);

						// Remove services
						if (offlineNode == null) {

							// Register as offline
							offlineNodes.put(buffer.nodeID, new OfflineNode(now, now));

							// Uninstall services
							current = nodeInfos.get(buffer.nodeID);
							if (current != null) {
								if (current != null) {

									// Remove CPU usage and it's timestamp
									nodeActivities.remove(buffer.nodeID);

									// Remove remote actions
									registry.removeActions(buffer.nodeID);

									// Remove remote event listeners
									eventbus.removeListeners(buffer.nodeID);
								}
							}
						} else {

							// Update offline status
							offlineNodes.put(buffer.nodeID, new OfflineNode(now, offlineNode.since));
						}
					} finally {
						writeLock.unlock();
					}
					if (current != null) {

						// Log
						logger.info("Node \"" + buffer.nodeID + "\" disconnected.");

						// Notify listeners (unexpected disconnection)
						broadcastNodeDisconnected(current, true);
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
						response.put("sender", buffer.nodeID);
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

			// Create gossip request
			Collection<Tree> values = nodeInfos.values();
			int size = values.size();
			if (size == 0) {
				return;
			}
			Tree root = new Tree();
			root.put("ver", ServiceBroker.PROTOCOL_VERSION);
			root.put("sender", nodeID);

			// UDP enabled
			Tree current = registry.generateDescriptor();
			if (urls == null || urls.length == 0) {
				String host = null;
				if (useHostname) {
					host = getHostName();
				} else {
					Tree ipList = current.get("ipList");
					if (ipList.size() > 0) {
						String ip = ipList.get(0).asString();
						if (ip != null && !ip.isEmpty()) {
							host = ip;
						}
					}
					if (host == null) {
						host = getHostName();
					}
				}
				root.put("host", host);
				root.put("port", currentPort);
			}

			// Add "online" and "offline" blocks
			Tree online = root.putMap("online");
			Tree offline = root.putMap("offline");

			// Add current node
			Tree node = online.putList(nodeID);
			node.add(current.get("when").asLong());
			node.add(System.currentTimeMillis());
			node.add(monitor.getTotalCpuPercent());

			// Create gossip request message
			Tree[] liveEndpoints = new Tree[size];
			Tree[] unreachableEndpoints = new Tree[size];

			int liveEndpointCount = 0;
			int unreachableEndpointCount = 0;

			OfflineNode offlineNode;
			NodeActivity activity;
			String sender;

			readLock.lock();
			try {
				for (Tree info : nodeInfos.values()) {
					sender = info.get("sender", (String) null);
					if (sender == null || sender.isEmpty() || nodeID.equals(sender)) {
						continue;
					}
					offlineNode = offlineNodes.get(sender);
					if (offlineNode == null) {

						// Online
						liveEndpoints[liveEndpointCount++] = info;
						node = online.putList(sender);
						node.add(info.get("when").asLong());
						activity = nodeActivities.get(sender);
						if (activity == null) {
							node.add(0).add(0);
						} else {
							node.add(activity.when).add(activity.cpu);
						}

					} else {

						// Offline
						unreachableEndpoints[unreachableEndpointCount++] = info;
						if (offlineNode.when > 0) {
							node = offline.putList(sender);
							node.add(offlineNode.when).add(offlineNode.since);
						}
					}
				}
			} finally {
				readLock.unlock();
			}

			// Remove empty "offline" node
			if (offline.isEmpty()) {
				offline.remove();
			}

			// Serialize gossip packet
			byte[] packet = serialize(PACKET_GOSSIP_REQ_ID, root);

			// Do gossiping with a live endpoint
			if (liveEndpointCount > 0) {
				sendGossipToRandomEndpoint(liveEndpoints, liveEndpointCount, packet, root);
			}

			// Do gossiping with a unreachable endpoint
			if (unreachableEndpointCount > 0) {

				// 10 nodes:
				// 1 dead / (9 live + 1) = 0.10
				// 5 dead / (5 live + 1) = 0.83
				// 9 dead / (1 live + 1) = 4.50
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

	protected void sendGossipToRandomEndpoint(Tree[] endpoints, int size, byte[] packet, Tree message) {

		// Choose random endpoint
		Tree info = endpoints[rnd.nextInt(size)];

		// Get the nodeID
		String targetNodeID = info.get("sender", (String) null);

		// Debug
		if (debug) {
			logger.info("Gossip request submitting to \"" + targetNodeID + "\" node:\r\n" + message);
		}

		// Send gossip request to node
		writer.send(targetNodeID, info, packet);
	}

	// --- UDP MULTICAST MESSAGE RECEIVED ---

	public void udpPacketReceived(String nodeID, String host, int port) {

		// Debug
		if (debug) {
			logger.info("Discovery message received from \"" + nodeID + "\" node (host: " + host + ", port: " + port
					+ ").");
		}

		// Store data
		writeLock.lock();
		try {
			Tree current = nodeInfos.get(nodeID);
			if (current == null) {

				// Add to "nodeInfos" without services block,
				// this is a new (unknown / unregistered) node
				registerAsOffline(nodeID, host, port);

			} else {

				// Check hostname and port
				boolean hostChanged = !host.equals(current.get("hostname", ""));
				if (hostChanged) {
					Tree ipList = current.get("ipList");
					if (ipList != null) {
						for (Tree ip : ipList) {
							if (host.equals(ip.asString())) {
								hostChanged = false;
								break;
							}
						}
					}
				}
				if (hostChanged || current.get("port", 0) != port) {

					// Add to "nodeInfos" without services block,
					// node's hostname or IP address changed
					registerAsOffline(nodeID, host, port);
				}
			}
		} finally {
			writeLock.unlock();
		}
	}

	protected Tree registerAsOffline(String nodeID, String host, int port) {
		if (nodeID == null || host == null || nodeID.isEmpty() || host.isEmpty() || port < 1) {
			logger.warn("Missing parameters (node ID: " + nodeID + ", host: " + host + ", port: " + port + ")!");
			return null;
		}

		// Add to "nodeInfos" without services block
		Tree info = new Tree();
		info.put("sender", nodeID);
		info.put("when", 0);
		info.putObject("hostname", host);
		info.put("port", port);
		nodeInfos.put(nodeID, info);

		// Add to "offlineNodes"
		offlineNodes.put(nodeID, new OfflineNode(0, System.currentTimeMillis()));

		// Remove from activities
		nodeActivities.remove(nodeID);

		// The new info block
		return info;
	}

	// --- GOSSIP REQUEST MESSAGE RECEIVED ---

	protected final AtomicLong lastRequest = new AtomicLong();

	protected void processGossipRequest(Tree data) throws Exception {

		// Get sender nodeID
		String sender = data.get("sender", (String) null);
		if (sender == null || sender.isEmpty()) {
			logger.warn("Sender property not found:\r\n" + data);
			return;
		}

		// Debug
		if (debug) {
			logger.info("Gossip request received from \"" + sender + "\" node:\r\n" + data);
		}

		// Response blocks
		Tree online = data.get("online");
		Tree offline = data.get("offline");

		// Processing variables
		long when, currentWhen, since;
		Tree info, current, update;
		OfflineNode offlineNode;
		NodeActivity activity;
		long thisWhen = -1;
		String nodeID;
		int cpu;

		// --- APPLY CHANGES (OFFLINE AND CPU) ---

		// Disconnected / processed nodes
		HashMap<String, Tree> processedNodes = new HashMap<>(128);

		// Update CPU registry and offline status
		writeLock.lock();
		try {

			// Update CPU registry
			if (online != null) {
				for (Tree node : online) {
					if (node.size() == 3) {
						nodeID = node.getName();
						activity = nodeActivities.get(nodeID);
						when = node.get(1).asLong();
						cpu = node.get(2).asInteger();
						if (activity != null) {
							nodeActivities.put(nodeID, new NodeActivity(when, cpu));
						}
					} else {
						logger.warn("Invalid \"online\" block: " + node.toString(false));
					}
				}
			}

			// Update offline status
			if (offline != null) {
				for (Tree node : offline) {
					if (node.size() == 2) {
						nodeID = node.getName();
						if (this.nodeID.equals(nodeID)) {
							continue;
						}
						when = node.get(0).asLong();
						current = nodeInfos.get(nodeID);
						if (current != null && current.get("when", 0L) < when) {

							// Is currently offline?
							offlineNode = offlineNodes.get(nodeID);
							if (offlineNode != null) {
								if (offlineNode.when < when) {
									since = node.get(1).asLong();
									since = Math.min(offlineNode.since, since);
									offlineNodes.put(nodeID, new OfflineNode(when, since));
								}
								continue;
							}

							// Remove CPU usage and last heartbeat time
							nodeActivities.remove(nodeID);

							// Remove remote actions
							registry.removeActions(nodeID);

							// Remove remote event listeners
							eventbus.removeListeners(nodeID);

							// Add to disconnected nodes
							processedNodes.put(nodeID, current);
						}
					} else {
						logger.warn("Invalid \"offline\" block: " + node.toString(false));
					}
				}
			}

		} finally {
			writeLock.unlock();
		}

		// Notify listeners
		if (!processedNodes.isEmpty()) {
			for (Tree node : processedNodes.values()) {

				// Log
				logger.info("Node \"" + node.get("sender", "") + "\" disconnected.");

				// Notify listeners (unexpected disconnection)
				broadcastNodeDisconnected(node, true);
			}
		}

		// --- WRITE RESPONSE ---

		// Create response node
		Tree root = new Tree();
		root.put("ver", ServiceBroker.PROTOCOL_VERSION);
		root.put("sender", this.nodeID);

		Tree onlineRsp = root.putMap("online");
		Tree offlineRsp = root.putMap("offline");

		// Process config
		readLock.lock();
		try {
			info = nodeInfos.get(sender);
			if (info == null) {

				// Fix URL list (UDP disabled)
				if (urls != null && urls.length > 0) {
					logger.warn("Unknown node ID (" + sender + ")!");
					return;
				}

				// Add sender as offline node
				info = registerAsOffline(sender, data.get("host", ""), data.get("port", 0));
				if (info == null) {
					logger.warn("Unable to create new info block!");
					return;
				}
			}

			// Check "online" entries in request
			if (online != null) {
				for (Tree node : online) {
					if (node.size() == 3) {
						nodeID = node.getName();
						when = node.get(0).asLong();
						if (this.nodeID.equals(nodeID)) {
							thisWhen = when;
							continue;
						}
						if (processedNodes.put(nodeID, info) != null) {
							continue;
						}

						// Is it really online?
						offlineNode = offlineNodes.get(nodeID);
						if (offlineNode == null) {
							current = nodeInfos.get(nodeID);
							if (current != null) {
								update = null;

								// Add node info to response
								if (current.get("when", 0L) > when) {
									update = onlineRsp.putList(nodeID);
									update.addObject(current);
								}

								// Add new CPU info to response
								activity = nodeActivities.get(nodeID);
								if (activity != null && activity.when > node.get(1).asLong()) {
									if (update == null) {
										update = onlineRsp.putList(nodeID);
									}
									update.add(activity.when).add(activity.cpu);
								}
							}
						} else {

							// Add new offline block to response
							if (offlineNode.when > when) {
								offlineRsp.putList(nodeID).add(offlineNode.when).add(offlineNode.since);
							}
						}
					}
				}
			}

			// Check "offline" entries in request
			if (offline != null) {
				for (Tree node : offline) {
					if (node.size() == 2) {
						nodeID = node.getName();
						when = node.get(0).asLong();
						if (this.nodeID.equals(nodeID)) {
							thisWhen = Long.MAX_VALUE;
							continue;
						}
						if (processedNodes.put(nodeID, info) != null) {
							continue;
						}
						offlineNode = offlineNodes.get(nodeID);
						if (offlineNode == null) {

							// Is it really offline?
							current = nodeInfos.get(nodeID);
							if (current != null) {
								currentWhen = current.get("when", 0L);
								if (currentWhen > when) {

									// We know, it is online now
									update = onlineRsp.putList(nodeID);
									update.addObject(current);
									activity = nodeActivities.get(nodeID);
									if (activity != null && activity.when > when) {
										update.add(activity.when).add(activity.cpu);
									}

								}
							}
						} else {

							// Add new offline block to response
							if (offlineNode.when > when) {
								offlineRsp.putList(nodeID).add(offlineNode.when).add(offlineNode.since);
							}
						}
					}
				}
			}

			// Add unknown online nodes
			for (Map.Entry<String, NodeActivity> entry : nodeActivities.entrySet()) {
				nodeID = entry.getKey();
				if (processedNodes.put(nodeID, info) != null) {
					continue;
				}
				activity = entry.getValue();
				current = nodeInfos.get(nodeID);
				update = onlineRsp.putList(nodeID);
				update.addObject(current);
				update.add(activity.when).add(activity.cpu);
			}

			// Add unknown offline nodes
			for (Map.Entry<String, OfflineNode> entry : offlineNodes.entrySet()) {
				nodeID = entry.getKey();
				if (processedNodes.put(nodeID, info) != null) {
					continue;
				}
				offlineNode = entry.getValue();
				if (offlineNode.when > 0) {
					update = offlineRsp.putList(nodeID);
					update.add(offlineNode.when).add(offlineNode.since);
				}
			}

		} finally {
			readLock.unlock();
		}

		// Add this to online block
		update = onlineRsp.putList(this.nodeID);
		when = registry.getWhen();
		if (thisWhen != when) {
			if (thisWhen > when) {
				registry.clearCache();
			}
			current = registry.generateDescriptor().clone();
			current.put("port", currentPort);
			update.addObject(current);
		}
		update.add(System.currentTimeMillis()).add(monitor.getTotalCpuPercent());

		// Remove empty "offline" block
		if (offlineRsp.isEmpty()) {
			offlineRsp.remove();
		}

		// Debug
		if (debug) {
			logger.info("Gossip response submitting to \"" + sender + "\" node:\r\n" + root);
		}

		// Serialize response
		byte[] packet = serialize(PACKET_GOSSIP_RSP_ID, root);

		// Send response
		writer.send(sender, info, packet);
	}

	// --- GOSSIP RESPONSE MESSAGE RECEIVED ---

	protected void processGossipResponse(Tree data) throws Exception {

		// Debug
		String sender = data.get("sender", (String) null);
		if (sender == null || sender.isEmpty()) {
			logger.warn("Sender property not found:\r\n" + data);
			return;
		}
		if (debug) {
			logger.info("Gossip response received from \"" + sender + "\" node:\r\n" + data);
		}

		// Disconnected nodes
		LinkedList<Tree> disconnectedNodes = new LinkedList<>();

		Tree online = data.get("online");
		Tree offline = data.get("offline");

		// Processing variables
		long when, cpuWhen, since;
		OfflineNode offlineNode;
		NodeActivity activity;
		Tree current, info;
		String nodeID;
		int cpu;

		// Process config
		writeLock.lock();
		try {

			// Process "online" block
			if (online != null) {
				for (Tree node : online) {

					// Get parameters from input
					info = null;
					cpuWhen = 0;
					cpu = 0;
					if (node.size() == 1) {
						info = node.get(0);
					} else if (node.size() == 2) {
						cpuWhen = node.get(0).asLong();
						cpu = node.get(1).asInteger();
					} else if (node.size() == 3) {
						info = node.get(0);
						cpuWhen = node.get(1).asLong();
						cpu = node.get(2).asInteger();
					} else {
						logger.warn("Invalid \"online\" block: " + node.toString(false));
						continue;
					}

					// Get nodeID
					nodeID = node.getName();
					if (this.nodeID.equals(nodeID)) {
						continue;
					}

					// Process info block
					if (info != null) {
						when = info.get("when", 0L);
						current = nodeInfos.get(nodeID);
						if (current == null || when > current.get("when", 0)) {
							updateNodeInfo(nodeID, info);
						}
					}

					// Process CPU block
					if (cpuWhen > 0) {
						activity = nodeActivities.get(nodeID);
						if (activity == null) {
							if (nodeInfos.containsKey(nodeID)) {
								nodeActivities.put(nodeID, new NodeActivity(cpuWhen, cpu));
							}
						} else if (activity.when < cpuWhen) {
							nodeActivities.put(nodeID, new NodeActivity(cpuWhen, cpu));
						}
					}
				}
			}

			// Process "offline" block
			if (offline != null) {
				for (Tree node : offline) {
					if (node.size() == 2) {

						// Get parameters from input
						when = node.get(0).asLong();
						since = node.get(1).asLong();
						nodeID = node.getName();
						if (this.nodeID.equals(nodeID)) {
							continue;
						}

						// Is currently offline?
						offlineNode = offlineNodes.get(nodeID);
						if (offlineNode == null) {
							current = nodeInfos.get(nodeID);
							if (current != null) {

								// Add to offline nodes
								offlineNodes.put(nodeID, new OfflineNode(when, since));

								// Remove CPU usage and last heartbeat time
								nodeActivities.remove(nodeID);

								// Remove remote actions
								registry.removeActions(nodeID);

								// Remove remote event listeners
								eventbus.removeListeners(nodeID);

								// Add to disconnected nodes
								disconnectedNodes.add(current);

							}
						} else {
							if (offlineNode.when < when) {
								offlineNodes.put(nodeID, new OfflineNode(when, since));
							}
						}
					} else {
						logger.warn("Invalid \"offline\" block: " + node.toString(false));
					}
				}
			}
		} finally {
			writeLock.unlock();
		}

		// Notify listeners
		if (!disconnectedNodes.isEmpty()) {
			for (Tree node : disconnectedNodes) {

				// Log
				logger.info("Node \"" + node.get("sender", "") + "\" disconnected.");

				// Notify listeners (unexpected disconnection)
				broadcastNodeDisconnected(node, true);
			}
		}
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

	public int getCurrentPort() {
		return currentPort;
	}

	public boolean isUseHostname() {
		return useHostname;
	}

	public void setUseHostname(boolean useHostname) {
		this.useHostname = useHostname;
	}

	public int getMulticastPackets() {
		return multicastPackets;
	}

	public void setMulticastPackets(int multicastPackets) {
		this.multicastPackets = multicastPackets;
	}

}