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

import static services.moleculer.util.CommonUtils.getHostName;
import static services.moleculer.util.CommonUtils.parseURLs;
import static services.moleculer.util.CommonUtils.readTree;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;
import services.moleculer.transporter.tcp.NodeDescriptor;
import services.moleculer.transporter.tcp.TcpReader;
import services.moleculer.transporter.tcp.TcpWriter;
import services.moleculer.transporter.tcp.UDPLocator;
import services.moleculer.util.FastBuildTree;

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
 * 
 * @see RedisTransporter
 * @see NatsTransporter
 * @see MqttTransporter
 * @see JmsTransporter
 * @see GoogleTransporter
 * @see KafkaTransporter
 * @see AmqpTransporter
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
	protected int gossipPeriod = 2;

	/**
	 * Max number of keep-alive connections (-1 = unlimited, 0 = disable
	 * keep-alive connections).
	 */
	protected int maxConnections = 32;

	/**
	 * Max enable packet size (BYTES).
	 */
	protected int maxPacketSize = 1024 * 1024;

	/**
	 * List of URLs ("tcp://host:port/nodeID" or "host:port/nodeID" or
	 * "host/nodeID"), when UDP discovery is disabled.
	 */
	protected String[] urls = {};

	/**
	 * Use hostnames instead of IP addresses As the DHCP environment is dynamic,
	 * any later attempt to use IPs instead hostnames would most likely yield
	 * false results. Therefore, use hostnames if you are using DHCP.
	 */
	protected boolean useHostname = true;

	/**
	 * UDP broadcast/multicast port
	 */
	protected int udpPort = 4445;

	/**
	 * UDP bind address (null = autodetect)
	 */
	protected String udpBindAddress;

	/**
	 * UDP broadcast/multicast period in SECONDS
	 */
	protected int udpPeriod = 30;

	/**
	 * Resuse addresses
	 */
	protected boolean udpReuseAddr = true;

	/**
	 * Maximum number of outgoing multicast packets (0 = runs forever)
	 */
	protected int udpMaxDiscovery = 0;

	/**
	 * UDP multicast address of automatic discovery service.
	 */
	protected String udpMulticast = "239.0.0.0";

	/**
	 * TTL of UDP multicast packets
	 */
	protected int udpMulticastTTL = 1;

	/**
	 * Use UDP broadcast WITH UDP multicast (false = use UDP multicast only)
	 */
	protected boolean udpBroadcast = false;

	/**
	 * Random generator.
	 */
	protected final Random rnd = new Random();

	/**
	 * Cancelable timer of gossiper
	 */
	protected volatile ScheduledFuture<?> gossiperTimer;

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
	protected UDPLocator locator;

	/**
	 * Current TCP port
	 */
	protected int currentPort;

	// --- LOCAL NODE'S DESCRIPTOR ---

	/**
	 * Current node descriptor
	 */
	protected NodeDescriptor cachedDescriptor;

	/**
	 * Current node descriptor
	 */
	protected AtomicLong timestamp = new AtomicLong();

	// --- CACHED GOSSIP HELLO MESSAGE ---

	protected byte[] cachedHelloMessage;

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

	/**
	 * Start TCP Transporter in full TCP mode, without UDP discovery. Loads node
	 * list from an URL (as an http or file resource in JSON/XML/YAML format).
	 * Sample JSON file:<br>
	 * <br>
	 * {<br>
	 * "nodes":[<br>
	 * "tcp://host1:port1/nodeID1",<br>
	 * "tcp://host2:port2/nodeID2",<br>
	 * "tcp://host3:port3/nodeID3"<br>
	 * ]<br>
	 * }
	 */
	public TcpTransporter(URL urlList) throws Exception {
		this.urls = parseURLs(readTree(urlList.toString()), "nodes", null);
	}

	// --- CONNECT ---

	@Override
	public void connect() {
		try {

			// Create reader and writer
			disconnect();
			reader = new TcpReader(this);
			writer = new TcpWriter(this);

			// Disable offline timeout when use host list
			if (urls != null && urls.length > 0) {
				offlineTimeout = 0;
				nodes.clear();
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
					int port;
					try {
						port = Integer.parseInt(parts[1]);
					} catch (Exception e) {
						logger.warn("Invalid URL format (" + url
								+ ")! Valid syntax is \"tcp://host:port/nodeID\" or \"host:port/nodeID\"!");
						continue;
					}
					String sender = parts[2];
					if (sender.equals(nodeID)) {

						// TCP server's port (port in URL list)
						this.port = port;
						continue;
					}
					String host = parts[0];
					nodes.put(sender, new NodeDescriptor(sender, useHostname, host, port));
				}
			} else if (offlineTimeout > 0 && offlineTimeout < 15) {
				offlineTimeout = 15;
			}

			// Process basic properties (eg. "prefix")
			heartbeatTimeout = 0;
			heartbeatInterval = 0;

			// Start TCP server
			reader.connect();
			currentPort = reader.getCurrentPort();

			// Create descriptor of current node
			Tree info = registry.getDescriptor();
			info.put("port", currentPort);
			info.put("seq", "0");
			cachedDescriptor = new NodeDescriptor(nodeID, useHostname, true, info);

			// Start data writer (TCP client)
			writer.connect();

			// TCP + UDP mode ("zero config")
			if (urls == null || urls.length == 0) {
				locator = new UDPLocator(nodeID, this, scheduler);
				locator.connect();
			}

			// Start gossiper
			gossiperTimer = scheduler.scheduleWithFixedDelay(this::sendGossipRequest, gossipPeriod, gossipPeriod,
					TimeUnit.SECONDS);

			// Start timeout checker's timer
			if (checkTimeoutTimer == null && offlineTimeout > 0) {
				int period = Math.max(offlineTimeout / 3, 10);
				checkTimeoutTimer = scheduler.scheduleAtFixedRate(this::checkTimeouts, period, period,
						TimeUnit.SECONDS);
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
		if (locator != null) {
			locator.disconnect();
			locator = null;
		}

		// Stop gossiper's timer
		if (gossiperTimer != null) {
			gossiperTimer.cancel(false);
			gossiperTimer = null;
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
	public void stopped() {

		// Stop timers
		super.stopped();

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

					// Send pong
					if (debug) {
						logger.info("Ping message received:\r\n" + data);
					}
					String sender = data.get("sender", "");
					if (sender == null || sender.isEmpty()) {
						logger.warn("Missing \"sender\" property:\r\n" + data);
						return;
					}
					data.put("sender", this.nodeID);
					data.put("target", System.currentTimeMillis());
					writer.send(sender, serialize(PACKET_PONG_ID, data));
					return;

				case PACKET_PONG_ID:

					// Pong received
					if (debug) {
						logger.info("Pong message received:\r\n" + data);
					}
					registry.receivePong(data);
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

	public void unableToSend(String nodeID, LinkedList<byte[]> packets, Throwable cause) {
		if (nodeID != null) {
			executor.execute(() -> {

				// Debug
				if (debug) {
					logger.warn("Unable to send message to \"" + nodeID + "\".", cause);
				}

				// Mark endpoint as offline
				NodeDescriptor node = nodes.get(nodeID);
				boolean disconnected = false;
				if (node != null) {
					node.writeLock.lock();
					try {
						if (node != null && node.markAsOffline()) {

							// Remove actions and listeners
							registry.removeActions(nodeID);
							eventbus.removeListeners(nodeID);
							writer.close(node.nodeID);
							disconnected = true;

						}
					} catch (Exception error) {
						logger.warn("Unable to turn off node!", error);
					} finally {
						node.writeLock.unlock();
					}
				}
				if (node != null && disconnected) {

					// Notify listeners (unexpected disconnection)
					logger.info("Node \"" + nodeID + "\" disconnected.");
					broadcastNodeDisconnected(node.info, true);
				}

				// Send error back to the source
				if (packets != null) {
					FastBuildTree errorMap = null;
					if (cause != null) {
						errorMap = new FastBuildTree(2);

						// Add message
						errorMap.putUnsafe("message", cause.getMessage());

						// Add trace
						StringWriter sw = new StringWriter(128);
						PrintWriter pw = new PrintWriter(sw);
						cause.printStackTrace(pw);
						errorMap.putUnsafe("trace", sw.toString());
					}
					for (byte[] packet : packets) {
						try {

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
								FastBuildTree response = new FastBuildTree(6);
								response.putUnsafe("id", id);
								response.putUnsafe("ver", ServiceBroker.PROTOCOL_VERSION);
								response.putUnsafe("sender", nodeID);
								response.putUnsafe("success", false);
								response.putUnsafe("data", (String) null);
								if (errorMap != null) {
									response.putUnsafe("error", errorMap);
								}
								registry.receiveResponse(response);
							}
						} catch (Exception error) {
							logger.warn("Unable to handle error!", error);
						}
					}
				}
			});
		}
	}

	// --- SEND DISCONNECT (UNUSED) ---

	@Override
	protected void sendDisconnectPacket() {

		// Do nothing
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
					throw new IOException("Outgoing packet is larger than the \"maxPacketSize\" limit (" + packet.length
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

	// --- LOCAL NODE'S DESCRIPTOR ---

	public NodeDescriptor getDescriptor() {
		cachedDescriptor.writeLock.lock();
		try {

			// Check timestamp
			long current = registry.getTimestamp();
			if (timestamp.get() == current) {
				return cachedDescriptor;
			} else {
				while (true) {
					current = registry.getTimestamp();
					cachedDescriptor.info = registry.getDescriptor();
					if (current == registry.getTimestamp()) {
						timestamp.set(current);
						break;
					}
				}
				cachedDescriptor.seq++;
				cachedDescriptor.info.put("seq", cachedDescriptor.seq);
				cachedDescriptor.info.put("port", reader.getCurrentPort());
			}

		} finally {
			cachedDescriptor.writeLock.unlock();
		}
		return cachedDescriptor;
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
		String host = data.get("host", (String) null);
		int port = data.get("port", 0);

		// Register as offline node (if unknown)
		registerAsNewNode(sender, host, port);
	}

	protected void registerAsNewNode(String sender, String host, int port) {

		// Check node
		if (sender == null || sender.isEmpty()) {
			throw new IllegalArgumentException("Empty sender field!");
		}
		if (host == null || host.isEmpty()) {
			throw new IllegalArgumentException("Empty host field!");
		}
		if (port < 1) {
			throw new IllegalArgumentException("Invalid port value (" + port + ")!");
		}
		if (nodeID.equalsIgnoreCase(sender)) {
			return;
		}
		NodeDescriptor node = nodes.get(sender);
		if (node == null) {

			// Add as new, offline node
			try {
				nodes.put(sender, new NodeDescriptor(sender, useHostname, host, port));
				logger.info("Node \"" + sender + "\" registered.");
			} catch (Exception cause) {
				logger.warn("Unable to register new node!", cause);
			}

		} else {
			node.writeLock.lock();
			try {

				// Host or port number changed
				if (!node.host.equalsIgnoreCase(host) || node.port != port) {
					node.host = host;
					node.port = port;
					if (node.info != null) {
						if (useHostname) {
							node.info.put("hostname", host);
						} else {
							Tree ipList = node.info.get("ipList");
							if (ipList == null) {
								ipList = node.info.putList("ipList");
							} else {
								ipList.clear();
							}
							ipList.add(host);
						}
						node.info.put("port", port);
					}
					writer.close(sender);
				}

			} finally {
				node.writeLock.unlock();
			}
		}
	}

	// --- SEND GOSSIP REQUEST TO RANDOM NODES ---

	/**
	 * Create and send a Gossip request packet.
	 */
	protected Tree sendGossipRequest() {
		try {

			// Update CPU
			NodeDescriptor descriptor = getDescriptor();
			int cpu = monitor.getTotalCpuPercent();
			descriptor.writeLock.lock();
			try {
				descriptor.updateCpu(cpu);
			} finally {
				descriptor.writeLock.unlock();
			}

			// Are we alone?
			if (nodes.isEmpty()) {
				return null;
			}

			// Add "online" and "offline" blocks
			Collection<NodeDescriptor> descriptors = nodes.values();
			int size = nodes.size() + 32;
			FastBuildTree online = new FastBuildTree(size);
			FastBuildTree offline = new FastBuildTree(size);

			// Add current node
			descriptor.readLock.lock();
			try {
				ArrayList<Object> array = new ArrayList<>(3);
				array.add(descriptor.seq);
				array.add(descriptor.cpuSeq);
				array.add(descriptor.cpu);
				online.putUnsafe(nodeID, array);
			} finally {
				descriptor.readLock.unlock();
			}

			// Separate online and offline nodes
			String[] liveEndpoints = new String[size];
			String[] unreachableEndpoints = new String[size];

			int liveEndpointCount = 0;
			int unreachableEndpointCount = 0;

			// Loop on registered nodes
			for (NodeDescriptor node : descriptors) {
				node.readLock.lock();
				try {
					if (node.offlineSince > 0) {

						// Offline
						if (unreachableEndpointCount < unreachableEndpoints.length) {
							unreachableEndpoints[unreachableEndpointCount++] = node.nodeID;
						}
						if (node.seq > 0) {
							offline.put(node.nodeID, node.seq);
						}
					} else {
						if (!node.local) {

							// Online
							if (liveEndpointCount < liveEndpoints.length) {
								liveEndpoints[liveEndpointCount++] = node.nodeID;
							}
							if (node.seq > 0) {
								ArrayList<Object> array = new ArrayList<>(3);
								array.add(node.seq);
								array.add(node.cpuSeq);
								array.add(node.cpu);
								online.putUnsafe(node.nodeID, array);
							}
						}
					}
				} finally {
					node.readLock.unlock();
				}
			}

			// Create gossip request
			FastBuildTree root = new FastBuildTree(4);
			root.putUnsafe("ver", ServiceBroker.PROTOCOL_VERSION);
			root.putUnsafe("sender", nodeID);
			root.putUnsafe("online", online.asObject());
			if (!offline.isEmpty()) {
				root.putUnsafe("offline", offline.asObject());
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

			// For unit testing
			return root;

		} catch (Exception cause) {
			logger.error("Unable to send gossip message to peer!", cause);
		}
		return null;
	}

	protected void sendGossipToRandomEndpoint(String[] endpoints, int size, byte[] packet, Tree message)
			throws Exception {

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

	protected Tree processGossipRequest(Tree data) throws Exception {

		// Debug
		String sender = data.get("sender", (String) null);
		if (debug) {
			logger.info("Gossip request received from \"" + sender + "\" node:\r\n" + data);
		}

		// Add "online" and "offline" response blocks
		LinkedList<NodeDescriptor> allNodes = new LinkedList<>(nodes.values());
		NodeDescriptor descriptor = getDescriptor();
		allNodes.add(descriptor);
		int size = allNodes.size() + 1;
		FastBuildTree onlineRsp = new FastBuildTree(size);
		FastBuildTree offlineRsp = new FastBuildTree(size);

		// Online / offline nodes in request
		Tree onlineReq = data.get("online");
		Tree offlineReq = data.get("offline");

		// Loop in nodes
		LinkedList<NodeDescriptor> disconnectedNodes = new LinkedList<>();
		for (NodeDescriptor node : allNodes) {
			node.writeLock.lock();
			try {

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

				if ((seq == 0 || seq < node.seq) && node.seq > 0) {

					// We have newer info or requester doesn't know it
					if (node.offlineSince == 0) {
						if (!node.info.isEmpty()) {
							if ((cpuSeq == 0 || cpuSeq < node.cpuSeq) && node.cpuSeq > 0) {
								ArrayList<Object> array = new ArrayList<>(3);
								array.add(node.info.asObject());
								array.add(node.cpuSeq);
								array.add(node.cpu);
								onlineRsp.putUnsafe(node.nodeID, array);
							} else {
								onlineRsp.putUnsafe(node.nodeID, Collections.singletonList(node.info.asObject()));
							}
						}
					} else {
						offlineRsp.putUnsafe(node.nodeID, node.seq);
					}
				}

				if (offline != null) {

					// Requester said it is OFFLINE
					if (node.offlineSince > 0) {

						// We also knew it as offline
						node.markAsOffline(seq);
						continue;
					}
					if (!node.local) {
						if (node.offlineSince == 0) {

							// We know it is online, so we change it to offline
							if (node.markAsOffline(seq)) {

								// Remove remote actions and listeners
								registry.removeActions(node.nodeID);
								eventbus.removeListeners(node.nodeID);
								writer.close(node.nodeID);
								disconnectedNodes.add(node);

							} else if (seq == node.seq) {

								// We send back that this node is online
								node.seq = seq + 1;
								node.info.put("seq", node.seq);
								if (cpuSeq < node.cpuSeq && node.cpuSeq > 0) {
									ArrayList<Object> array = new ArrayList<>(3);
									array.add(node.info.asObject());
									array.add(node.cpuSeq);
									array.add(node.cpu);
									onlineRsp.putUnsafe(node.nodeID, array);
								} else {
									onlineRsp.putUnsafe(node.nodeID, Collections.singletonList(node.info.asObject()));
								}
							}
						}
						continue;
					}
				} else if (online != null) {

					// Requester said it is ONLINE
					if (node.offlineSince == 0) {
						if (cpuSeq > node.cpuSeq) {

							// We update our CPU info
							node.updateCpu(cpuSeq, cpu);

						} else if (cpuSeq < node.cpuSeq && node.cpuSeq > 0) {

							// We have newer CPU value, send back
							ArrayList<Object> array = new ArrayList<>(2);
							array.add(node.cpuSeq);
							array.add(node.cpu);
							onlineRsp.putUnsafe(node.nodeID, array);
						}
					} else {

						// We knew it as offline. We do nothing, because we'll
						// request it and we'll receive its INFO
						continue;
					}
				}
			} finally {
				node.writeLock.unlock();
			}
		}

		// Create gossip response
		FastBuildTree root = new FastBuildTree(4);
		root.putUnsafe("ver", ServiceBroker.PROTOCOL_VERSION);
		root.putUnsafe("sender", nodeID);

		// Remove empty blocks
		boolean emptyOnlineBlock = onlineRsp.isEmpty();
		boolean emptyOfflineBlock = offlineRsp.isEmpty();
		if (emptyOnlineBlock && emptyOfflineBlock) {

			// Message is empty
			return root;
		}
		if (!emptyOnlineBlock) {
			root.putUnsafe("online", onlineRsp.asObject());
		}
		if (!emptyOfflineBlock) {
			root.putUnsafe("offline", offlineRsp.asObject());
		}

		// Debug
		if (debug) {
			logger.info("Gossip response submitting to \"" + sender + "\" node:\r\n" + root);
		}

		// Serialize response
		byte[] packet = serialize(PACKET_GOSSIP_RSP_ID, root);

		// Send response
		writer.send(sender, packet);

		// Notify listeners (unexpected disconnection)
		for (NodeDescriptor node : disconnectedNodes) {
			logger.info("Node \"" + node.nodeID + "\" disconnected.");
			broadcastNodeDisconnected(node.info, true);
		}

		// For unit testing
		return root;
	}

	// --- GOSSIP RESPONSE MESSAGE RECEIVED ---

	protected void processGossipResponse(Tree data) throws Exception {

		// Debug
		if (debug) {
			String sender = data.get("sender", (String) null);
			logger.info("Gossip response received from \"" + sender + "\" node:\r\n" + data);
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

					// Update "info" block,
					// send updated, connected or reconnected event
					updateNodeInfo(nodeID, info);
				}
				if (cpuSeq > 0) {

					// We update our CPU info
					NodeDescriptor node = nodes.get(nodeID);
					if (node != null) {
						node.writeLock.lock();
						try {
							node.updateCpu(cpuSeq, cpu);
						} finally {
							node.writeLock.unlock();
						}
					}
				}
			}
		}

		// Process "offline" block
		if (offline != null) {
			for (Tree row : offline) {
				String nodeID = row.getName();

				NodeDescriptor node;
				if (this.nodeID.equals(nodeID)) {
					long seq = row.asLong();
					node = getDescriptor();
					node.writeLock.lock();
					try {
						long newSeq = Math.max(node.seq, seq + 1);
						if (node.seq < newSeq) {
							node.seq = newSeq;
							node.info.put("seq", newSeq);
						}
					} finally {
						node.writeLock.unlock();
					}
					continue;
				}

				node = nodes.get(nodeID);
				if (node == null) {
					return;
				}
				if (!row.isPrimitive()) {
					logger.warn("Invalid \"offline\" block: " + row);
					continue;
				}

				// Get parameters from input
				boolean disconnected = false;
				node.writeLock.lock();
				try {
					long seq = row.asLong();
					if (node.seq < seq && node.markAsOffline(seq)) {

						// We know it is online, so we change it to offline
						// Remove remote actions and listeners
						registry.removeActions(node.nodeID);
						eventbus.removeListeners(node.nodeID);
						writer.close(node.nodeID);
						disconnected = true;

					}
				} finally {
					node.writeLock.unlock();
				}
				if (node != null && disconnected) {

					// Notify listeners (not unexpected disconnection)
					logger.info("Node \"" + node.nodeID + "\" disconnected.");
					broadcastNodeDisconnected(node.info, false);
				}
			}
		}
	}

	// --- GOSSIP HELLO MESSAGE ---

	/**
	 * Create Gossip HELLO packet. Hello message is invariable, so we can cache
	 * it.
	 */
	public byte[] generateGossipHello() {
		if (cachedHelloMessage != null) {
			return cachedHelloMessage;
		}
		try {
			FastBuildTree root = new FastBuildTree(4);
			root.putUnsafe("ver", ServiceBroker.PROTOCOL_VERSION);
			root.putUnsafe("sender", nodeID);
			if (useHostname) {
				root.putUnsafe("host", getHostName());
			} else {
				root.putUnsafe("host", InetAddress.getLocalHost().getHostAddress());
			}
			root.putUnsafe("port", reader.getCurrentPort());
			cachedHelloMessage = serialize(PACKET_GOSSIP_HELLO_ID, root);
		} catch (Exception error) {
			throw new RuntimeException("Unable to create HELLO message!", error);
		}
		return cachedHelloMessage;
	}

	// --- UNUSED METHODS (TRANSPORTER USES SWIM INSTEAD OF HEARTBEATS) ---

	@Override
	public void broadcastInfoPacket() {

		// Do nothing
	}

	@Override
	public void setHeartbeatInterval(int heartbeatInterval) {
		throw new UnsupportedOperationException();
	}

	@Override
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

	public int getCurrentPort() {
		return currentPort;
	}

	public boolean isUseHostname() {
		return useHostname;
	}

	public void setUseHostname(boolean preferHostname) {
		this.useHostname = preferHostname;
	}

	public boolean isUdpBroadcast() {
		return udpBroadcast;
	}

	public void setUdpBroadcast(boolean udpBroadcast) {
		this.udpBroadcast = udpBroadcast;
	}

	public int getUdpMaxDiscovery() {
		return udpMaxDiscovery;
	}

	public void setUdpMaxDiscovery(int udpMaxDiscovery) {
		this.udpMaxDiscovery = udpMaxDiscovery;
	}

	public int getUdpPeriod() {
		return udpPeriod;
	}

	public void setUdpPeriod(int udpPeriod) {
		this.udpPeriod = udpPeriod;
	}

	public boolean isUdpReuseAddr() {
		return udpReuseAddr;
	}

	public void setUdpReuseAddr(boolean udpReuseAddr) {
		this.udpReuseAddr = udpReuseAddr;
	}

	public int getUdpPort() {
		return udpPort;
	}

	public void setUdpPort(int udpPort) {
		this.udpPort = udpPort;
	}

	public String getUdpBindAddress() {
		return udpBindAddress;
	}

	public void setUdpBindAddress(String udpBindAddress) {
		this.udpBindAddress = udpBindAddress;
	}

	public String getUdpMulticast() {
		return udpMulticast;
	}

	public void setUdpMulticast(String udpMulticast) {
		this.udpMulticast = udpMulticast;
	}

	public int getUdpMulticastTTL() {
		return udpMulticastTTL;
	}

	public void setUdpMulticastTTL(int udpMulticastTTL) {
		this.udpMulticastTTL = udpMulticastTTL;
	}

}