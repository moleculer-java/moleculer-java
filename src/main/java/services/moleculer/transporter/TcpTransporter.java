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

import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;
import services.moleculer.transporter.tcp.TcpEndpoint;
import services.moleculer.transporter.tcp.TcpWriteBuffer;

/**
 * TCP Transporter. Now it's just an empty sketch (it doesn't work).
 */
@Name("TCP Transporter")
public class TcpTransporter extends Transporter {

	// --- STANDARD PACKET IDS ---

	protected static final byte PACKET_EVENT_ID = 1;
	protected static final byte PACKET_REQUEST_ID = 2;
	protected static final byte PACKET_RESPONSE_ID = 3;
	protected static final byte PACKET_DISCOVER_ID = 4;
	protected static final byte PACKET_INFO_ID = 5;
	protected static final byte PACKET_DISCONNECT_ID = 6;
	protected static final byte PACKET_PING_ID = 7;
	protected static final byte PACKET_PONG_ID = 8;

	// --- PACKET IDS OF GOSSIPER ---

	protected static final byte PACKET_GOSSIP_REQUEST_ID = 9;
	protected static final byte PACKET_GOSSIP_RESPONSE_ID = 10;

	// --- REGISTERED ENDPOINTS ---

	protected final HashMap<String, TcpEndpoint> endpoints = new HashMap<>();

	// --- ENDPOINT CACHES ---

	protected final AtomicReference<ArrayList<TcpEndpoint>> cachedLiveEndpoints = new AtomicReference<>();
	protected final AtomicReference<ArrayList<TcpEndpoint>> cachedUnreachableEndpoints = new AtomicReference<>();

	// --- OTHER WORKING VARIABLES ---

	protected Random rnd = new Random();

	// --- NIO SELECTOR ---

	protected Selector selector;

	// --- WRITE BUFFERS ---

	protected HashMap<String, TcpWriteBuffer> writeBuffers = new HashMap<>();

	// --- CONSTUCTORS ---

	public TcpTransporter() {
		super();
	}

	public TcpTransporter(String prefix) {
		super(prefix);
	}

	// --- START TRANSPORTER ---

	/**
	 * Cancelable timer
	 */
	protected volatile ScheduledFuture<?> timer;

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

		// Process config

		// Start gossiper process
		timer = scheduler.scheduleAtFixedRate(this::doGossiping, 1, 1, TimeUnit.SECONDS);
	}

	// --- CONNECT ---

	@Override
	public void connect() {
		try {

			// Create selector
			if (selector != null) {
				disconnect();
			}
			selector = Selector.open();

			// TODO Create server socket

			// Send discover
			executor.execute(() -> {

				// Do the discovery process
				sendDiscoverPacket(discoverBroadcastChannel);
				sendInfoPacket(infoBroadcastChannel);

				// Ok, peer(s) connected
				logger.info("TCP pub-sub connection estabilished.");
			});

		} catch (Exception cause) {
			String msg = cause.getMessage();
			if (msg == null || msg.isEmpty()) {
				msg = "Unable to estabilish TCP connection!";
			} else if (!msg.endsWith("!") && !msg.endsWith(".")) {
				msg += "!";
			}
			logger.warn(msg);
			reconnect();
		}
	}

	// --- DISCONNECT ---

	protected void disconnect() {

		// Close server socket

		// Close client sockets

		// Close selector
		if (selector != null) {
			try {
				selector.close();
			} catch (Exception ignored) {
			}
		}
		selector = null;
	}

	// --- RECONNECT ---

	protected void reconnect() {
		disconnect();
		logger.info("Trying to reconnect...");
		scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
	}

	// --- ANY I/O ERROR ---

	@Override
	protected void error(Throwable cause) {
		logger.warn("Unexpected communication error occured!", cause);
		reconnect();
	}

	// --- STOP TRANSPORTER ---

	/**
	 * Closes transporter.
	 */
	@Override
	public void stop() {

		// Stop timer
		if (timer != null) {
			timer.cancel(false);
			timer = null;
		}

		// Disconnect, close sockets
		disconnect();
	}

	// --- SUBSCRIBE ---

	@Override
	public Promise subscribe(String channel) {

		// Unused method
		return Promise.resolve();
	}

	// --- PUBLISH ---

	@Override
	public void publish(String channel, Tree message) {
		if (selector != null) {
			try {

				// Parse channel
				int s = channel.indexOf('.');
				if (s == -1) {
					logger.warn("Invalid channel syntax (" + channel + ")!");
					return;
				}
				int e = channel.indexOf('.', s + 1);
				final String command;
				final String nodeID;
				if (e == -1) {
					command = channel.substring(s + 1);
					nodeID = null;
				} else {
					command = channel.substring(s + 1, e);
					nodeID = channel.substring(e + 1);
				}

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
				case PACKET_DISCOVER:
					packetID = PACKET_DISCOVER_ID;
					break;
				case PACKET_INFO:
					packetID = PACKET_INFO_ID;
					break;
				case PACKET_DISCONNECT:
					packetID = PACKET_DISCONNECT_ID;
					break;
				case PACKET_PING:
					packetID = PACKET_PING_ID;
					break;
				case PACKET_PONG:
					packetID = PACKET_PONG_ID;
					break;
				case PACKET_HEARTBEAT:

					// Not used in TCP transporter
					logger.warn("Unsupported command (" + command + ")!");
					return;

				default:
					logger.warn("Invalid command (" + command + ")!");
					return;
				}

				// Create data packet to send
				byte[] packet = serialize(packetID, message);

				// Send to node(s)
				if (nodeID == null) {
					sendToAllNodes(packet);
				} else {
					TcpEndpoint endpoint;
					synchronized (endpoints) {
						endpoint = endpoints.get(nodeID);
					}
					if (endpoint == null) {
						logger.warn("Unknown node ID (" + nodeID + ")!");
						return;
					}
					sendToNode(endpoint, packet);
				}

			} catch (Exception cause) {
				logger.warn("Unable to send message!", cause);
				reconnect();
			}
		}
	}

	protected byte[] serialize(byte packetID, Tree message) throws Exception {
		byte[] data = serializer.write(message);
		byte[] packet = new byte[data.length + 6];
		packet[5] = packetID;
		packet[4] = (byte) packet.length;
		packet[3] = (byte) (packet.length >>> 8);
		packet[2] = (byte) (packet.length >>> 16);
		packet[1] = (byte) (packet.length >>> 32);
		packet[0] = (byte) (packet[1] ^ packet[2] ^ packet[3] ^ packet[4] ^ packet[5]);
		System.arraycopy(data, 0, packet, 6, data.length);
		return packet;
	}

	protected void sendToAllNodes(byte[] packet) {

		// Send to all live endpoints
		ArrayList<TcpEndpoint> liveEndpoints = getEndpoints(true);
		for (TcpEndpoint endpoint : liveEndpoints) {
			sendToNode(endpoint, packet);
		}
	}

	protected void sendToNode(TcpEndpoint endpoint, byte[] packet) {

		// Send to endpoint
		TcpWriteBuffer writeBuffer;
		synchronized (writeBuffers) {
			writeBuffer = writeBuffers.get(endpoint.nodeID());
		}
		if (writeBuffer != null) {
			writeBuffer.write(packet);
			return;
		}

		// TODO Create new buffer (open socket)
	}

	// --- GOSSIPING ---

	protected void doGossiping() {

		// Get live / unreachable endpoints
		ArrayList<TcpEndpoint> liveEndpoints = getEndpoints(true);
		ArrayList<TcpEndpoint> unreachableEndpoints = getEndpoints(false);

		// Do gossiping with a live endpoint
		sendGossipToRandomEndpoint(liveEndpoints, unreachableEndpoints);

		// Do gossiping with a unreachable endpoint
		int unreachableEndpointCount = unreachableEndpoints.size();
		if (unreachableEndpointCount > 0) {

			// 10 nodes:
			// 1 dead / (9 live + 1) = 0.10
			// 5 dead / (5 live + 1) = 0.83
			// 9 dead / (1 live + 1) = 4.50
			double ratio = unreachableEndpointCount / (liveEndpoints.size() + 1);

			// Random number between 0.0 and 1.0
			double random = rnd.nextDouble();
			if (random < ratio) {
				sendGossipToRandomEndpoint(unreachableEndpoints, liveEndpoints);
			}

		}
	}

	protected ArrayList<TcpEndpoint> getEndpoints(boolean live) {
		synchronized (endpoints) {
			ArrayList<TcpEndpoint> result;
			if (live) {
				result = cachedLiveEndpoints.get();
			} else {
				result = cachedUnreachableEndpoints.get();
			}
			if (result != null) {
				return result;
			}
			int size = endpoints.size();
			ArrayList<TcpEndpoint> liveEndpoints = new ArrayList<>(size);
			ArrayList<TcpEndpoint> unreachableEndpoints = new ArrayList<>(size);
			for (TcpEndpoint endpoint : endpoints.values()) {
				if (endpoint.live()) {
					liveEndpoints.add(endpoint);
				} else {
					unreachableEndpoints.add(endpoint);
				}
			}
			cachedLiveEndpoints.set(liveEndpoints);
			cachedUnreachableEndpoints.set(unreachableEndpoints);
			if (live) {
				return liveEndpoints;
			}
			return unreachableEndpoints;
		}
	}

	protected void sendGossipToRandomEndpoint(ArrayList<TcpEndpoint> endpoints, ArrayList<TcpEndpoint> rest) {
		int size = endpoints.size();
		if (size < 1) {
			return;
		}
		int index = rnd.nextInt(size);
		TcpEndpoint peer = endpoints.get(index);

		// Create message
		Tree message = new Tree();
		Tree nodes = message.putList("nodes");
		for (TcpEndpoint endpoint : endpoints) {
			endpoint.writeTo(nodes.addMap());
		}
		for (TcpEndpoint endpoint : rest) {
			endpoint.writeTo(nodes.addMap());
		}

		// Create data packet to send
		try {

			byte[] packet = serialize(PACKET_GOSSIP_REQUEST_ID, message);

			// Send gossip request to node
			sendToNode(peer, packet);

		} catch (Exception cause) {
			logger.error("Unable to serialize data!", cause);
		}
	}

}