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
import services.moleculer.service.CpuSnapshot;
import services.moleculer.service.Name;
import services.moleculer.service.NodeDescriptor;
import services.moleculer.service.OfflineSnapshot;
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
				nodes.put(sender, NodeDescriptor.offline(sender, useHostname, parts[0], port));
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
					NodeDescriptor node = nodes.get(buffer.nodeID);
					if (node != null && node.switchToOffline()) {

						// Remove actions and listeners
						registry.removeActions(buffer.nodeID);
						eventbus.removeListeners(buffer.nodeID);

						// Notify listeners (unexpected disconnection)
						logger.info("Node \"" + buffer.nodeID + "\" disconnected.");
						broadcastNodeDisconnected(node.info, true);
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

	// --- SEND HEARTBEAT (UNUSED) ---
	
	@Override
	protected void sendHeartbeatPacket() {
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
				NodeDescriptor node = nodes.get(nodeID);
				if (node == null) {
					logger.warn("Unknown node ID (" + nodeID + ")!");
					return;
				}
				writer.send(node, packet);

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
	protected void checkTimeouts() {

		// Check offline timeout
		long now = System.currentTimeMillis();
		long offlineTimeoutMillis = offlineTimeout * 1000L;
		NodeDescriptor node;
		Iterator<NodeDescriptor> i = nodes.values().iterator();
		while (i.hasNext()) {
			node = i.next();
			if (now - node.getOfflineSince(Long.MAX_VALUE) > offlineTimeoutMillis) {

				// Remove node from Map
				i.remove();
				logger.info("Node \"" + nodeID + "\" is no longer registered because it was inactive for "
						+ offlineTimeout + " seconds.");
			}
		}
	}
	
	// --- UDP MULTICAST MESSAGE RECEIVED ---

	public void udpPacketReceived(String nodeID, String host, int port) {

		// Debug
		if (debug) {
			logger.info("Discovery message received from \"" + nodeID + "\" node (host: " + host + ", port: " + port
					+ ").");
		}

		// Check node
		NodeDescriptor prevNode = nodes.get(nodeID);
		NodeDescriptor newNode = NodeDescriptor.offline(nodeID, useHostname, host, port);
		if (prevNode == null) {

			// Add as new, offline node
			nodes.put(nodeID, newNode);
		} else {

			// Check hostname and port
			if (prevNode == null || !prevNode.host.equals(newNode.host) || prevNode.host != newNode.host) {

				// Host or port number changed -> reregister as offline
				nodes.put(nodeID, newNode);
			}
		}
	}

	// --- GOSSIPING ---

	protected Random rnd = new Random();

	/**
	 * Create and send a Gossip request packet.
	 */
	protected void sendGossipRequest() {
		try {

			// Create gossip request
			Tree root = new Tree();
			root.put("ver", ServiceBroker.PROTOCOL_VERSION);
			root.put("sender", nodeID);

			// UDP enabled -> add host and port to request
			NodeDescriptor self = registry.getDescriptor(true);
			if (urls == null || urls.length == 0) {
				root.put("host", self.host);
				root.put("port", self.port);
			}

			// Add "online" and "offline" blocks
			Tree online = root.putMap("online");
			Tree offline = root.putMap("offline");

			// Add current node
			Tree thisNode = online.putList(nodeID);
			thisNode.add(self.when);
			CpuSnapshot c = self.getCpuSnapshot();
			if (c != null) {
				thisNode.add(c.when);
				thisNode.add(c.value);				
			} else {
				thisNode.add(System.currentTimeMillis());
				thisNode.add(monitor.getTotalCpuPercent());
			}

			// Separate live and offline nodes
			int size = nodes.size() * 3 / 2;
			NodeDescriptor[] liveEndpoints = new NodeDescriptor[size];
			NodeDescriptor[] unreachableEndpoints = new NodeDescriptor[size];

			int liveEndpointCount = 0;
			int unreachableEndpointCount = 0;

			// Loop on nodes
			for (NodeDescriptor node : nodes.values()) {
				OfflineSnapshot o = node.getOfflineSnapshot();
				if (o != null) {

					// Offline
					unreachableEndpoints[unreachableEndpointCount++] = node;
					if (o.when > 0) {
						offline.putList(node.nodeID).add(o.when).add(o.since);
					}
				} else {
					if (!node.local) {

						// Online
						liveEndpoints[liveEndpointCount++] = node;
						if (node.when > 0) {
							c = node.getCpuSnapshot();
							online.putList(node.nodeID).add(node.when).add(c == null ? 0L : c.when)
									.add(c == null ? 0 : c.value);
						}
					}
				}
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

	protected void sendGossipToRandomEndpoint(NodeDescriptor[] endpoints, int size, byte[] packet, Tree message) {

		// Choose a random endpoint
		NodeDescriptor node = endpoints[rnd.nextInt(size)];

		// Debug
		if (debug) {
			logger.info("Gossip request submitting to \"" + node.nodeID + "\" node:\r\n" + message);
		}

		// Send gossip request to node
		writer.send(node, packet);
	}

	// --- GOSSIP REQUEST MESSAGE RECEIVED ---

	protected final AtomicLong lastRequest = new AtomicLong();

	protected void processGossipRequest(Tree data) throws Exception {

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
		allNodes.add(registry.getDescriptor(true));
		for (NodeDescriptor node : allNodes) {
			Tree online = onlineReq == null ? null : onlineReq.get(node.nodeID);
			Tree offline = offlineReq == null ? null : offlineReq.get(node.nodeID);
			long when = 0;
			long since = 0;
			long cpuWhen = 0;
			int cpu = 0;
			if (offline != null) {
				if (!offline.isEnumeration() || offline.size() != 2) {
					logger.warn("Invalid \"offline\" block: " + offline.toString(false));
					continue;
				}
				when = offline.get(0).asLong();
				since = offline.get(0).asLong();
			} else if (online != null) {
				if (!online.isEnumeration() || online.size() != 3) {
					logger.warn("Invalid \"online\" block: " + online.toString(false));
					continue;
				}
				when = online.get(0).asLong();
				cpuWhen = online.get(0).asLong();
				cpu = online.get(0).asInteger();
			}
			if (when == 0 || when < node.when) {

				// We have newer info or requester doesn't know it
				OfflineSnapshot o = node.getOfflineSnapshot();
				if (o == null) {
					if (node.local) {
						Tree row = onlineRsp.putList(node.nodeID);
						row.addObject(node.info);
						CpuSnapshot c = node.getCpuSnapshot();
						if (c == null) {
							row.add(System.currentTimeMillis()).add(monitor.getTotalCpuPercent());
						} else {
							row.add(c.when).add(c.value);
						}
					} else {
						Tree row = onlineRsp.putList(node.nodeID);
						row.addObject(node.info);
						CpuSnapshot c = node.getCpuSnapshot();
						if (c != null) {
							row.add(c.when).add(c.value);
						}
					}
				} else {
					offlineRsp.putList(node.nodeID).add(o.when).add(o.since);
				}
			}

			OfflineSnapshot o = node.getOfflineSnapshot();
			if (offline != null) {

				// Requester said it is OFFLINE
				if (o != null) {

					// We also knew it as offline
					// Update 'offlineSince' if it is older than us
					node.switchToOffline(when, since);
					continue;
				}
				if (!node.local) {

					// We know it is online, so we change it to offline
					if (node.switchToOffline(when, since)) {

						// Remove remote actions and listeners
						registry.removeActions(node.nodeID);
						eventbus.removeListeners(node.nodeID);

						// Notify listeners (not unexpected disconnection)
						logger.info("Node \"" + sender + "\" disconnected.");
						broadcastNodeDisconnected(node.info, false);
					}
					continue;
				}

				// We send back that we are online
				// Update to a newer 'when' if my is older
				if (when >= node.when) {
					NodeDescriptor self = registry.getDescriptor(false);
					Tree row = onlineRsp.putList(node.nodeID);
					row.addObject(self.info);					
					CpuSnapshot c = node.getCpuSnapshot();
					if (c == null) {
						row.add(System.currentTimeMillis()).add(monitor.getTotalCpuPercent());
					} else {
						row.add(c.when).add(c.value);
					}
				}
			} else if (online != null) {

				// Requester said it is ONLINE
				if (o == null) {
					CpuSnapshot c = node.getCpuSnapshot();
					if (c == null || cpuWhen > c.when) {

						// We update our CPU info
						node.updateCpu(cpuWhen, cpu);

					} else if (cpuWhen < c.when) {

						// We have newer CPU value, send back
						onlineRsp.putList(node.nodeID).add(c.when).add(c.value);
					}
				} else {

					// We knew it as offline. We do nothing, because we'll
					// request it and we'll receive its INFO
					continue;
				}
			}
		}

		// Whether we know the sender
		NodeDescriptor senderNode = nodes.get(sender);
		if (senderNode == null) {
			senderNode = NodeDescriptor.offline(sender, useHostname, data.get("host", "unknown"), data.get("port", 0));
			nodes.put(sender, senderNode);
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
		writer.send(senderNode, packet);
	}

	// --- GOSSIP RESPONSE MESSAGE RECEIVED ---

	protected void processGossipResponse(Tree data) throws Exception {

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
				long cpuWhen = 0;
				int cpu = 0;
				if (row.size() == 1) {
					info = row.get(0);
				} else if (row.size() == 2) {
					cpuWhen = row.get(0).asLong();
					cpu = row.get(1).asInteger();
				} else if (row.size() == 3) {
					info = row.get(0);
					cpuWhen = row.get(1).asLong();
					cpu = row.get(2).asInteger();
				} else {
					logger.warn("Invalid \"online\" block: " + row.toString(false));
					continue;
				}

				if (info != null) {

					// Update "info" block,
					// send updated, connected or reconnected event
					updateNodeInfo(nodeID, info);
				}
				if (cpuWhen > 0) {

					// We update our CPU info
					NodeDescriptor node = nodes.get(nodeID);
					CpuSnapshot c = node.getCpuSnapshot();
					if (node != null && (c == null || c.when < cpuWhen)) {
						node.updateCpu(cpuWhen, cpu);
					}
				}
			}
		}

		// Process "offline" block
		if (offline != null) {
			for (Tree row : offline) {
				String nodeID = row.getName();
				if (nodeID.equals(nodeID)) {
					continue;
				}
				if (!row.isEnumeration() || row.size() != 2) {
					logger.warn("Invalid \"offline\" block: " + row);
					continue;
				}

				// Get parameters from input
				long when = row.get(0).asLong();
				long since = row.get(1).asLong();

				NodeDescriptor node = nodes.get(nodeID);
				if (node == null) {
					return;
				}
				if (node.when < when) {
					if (node.switchToOffline(when, since)) {

						// We know it is online, so we change it to offline
						// Remove remote actions and listeners
						registry.removeActions(node.nodeID);
						eventbus.removeListeners(node.nodeID);

						// Notify listeners (not unexpected disconnection)
						logger.info("Node \"" + nodeID + "\" disconnected.");
						broadcastNodeDisconnected(node.info, false);

					}
				}
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