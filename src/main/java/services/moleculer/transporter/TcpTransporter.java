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
import services.moleculer.transporter.tcp.TcpReader;
import services.moleculer.transporter.tcp.TcpWriter;
import services.moleculer.util.CheckedTree;
import services.moleculer.transporter.tcp.TcpEndpoint;

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

	// --- ALL REGISTERED ENDPOINTS ---

	protected final HashMap<String, TcpEndpoint> endpoints = new HashMap<>();

	// --- PROPERTIES ---

	/**
	 * TCP port.
	 */
	protected int port = 7328;

	/**
	 * Socket socketTimeout, in milliseconds (0 = no timeout).
	 */
	protected int socketTimeout;

	/**
	 * Gossiping period time, in seconds
	 */
	protected int gossipingSeconds = 1;

	// --- CONSTUCTORS ---

	public TcpTransporter() {
		super();
	}

	public TcpTransporter(String prefix) {
		super(prefix);
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

		// Process config
		port = config.get("port", port);
		socketTimeout = config.get("socketTimeout", socketTimeout);
		gossipingSeconds = config.get("cleanup", gossipingSeconds);
	}

	// --- CONNECT ---

	/**
	 * Cancelable timer
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

	@Override
	public void connect() {
		try {

			// Create reader and writer
			disconnect();
			reader = new TcpReader(this);
			writer = new TcpWriter(this);

			// Start reader and writer
			reader.connect();
			writer.connect();

			// Start gossiper
			if (gossipingSeconds < 1) {
				gossipingSeconds = 1;
			}
			timer = scheduler.scheduleWithFixedDelay(this::doGossiping, gossipingSeconds * 2, gossipingSeconds,
					TimeUnit.SECONDS);

			// Ok, transporter started
			logger.info("TCP pub-sub connection estabilished.");

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
		disconnect();
	}

	// --- MESSAGE RECEIVED ---

	public void messageReceived(byte packetID, byte[] packet) {

		// TODO message received
		System.out.println("PACKET RECEIVED: " + new String(packet));
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

				// Send packet...
				if (nodeID == null) {

					// Send to all live endpoints
					ArrayList<TcpEndpoint> liveEndpoints = getEndpoints(true);
					for (TcpEndpoint endpoint : liveEndpoints) {
						writer.send(endpoint, packet);
					}

				} else {

					// Send packet to one endpoint
					TcpEndpoint endpoint;
					synchronized (endpoints) {
						endpoint = endpoints.get(nodeID);
					}
					if (endpoint == null) {
						logger.warn("Unknown node ID (" + nodeID + ")!");
						return;
					}
					writer.send(endpoint, packet);

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
		packet[1] = (byte) (packet.length >>> 24);
		packet[0] = (byte) (packet[1] ^ packet[2] ^ packet[3] ^ packet[4] ^ packet[5]);
		System.arraycopy(data, 0, packet, 6, data.length);
		return packet;
	}

	// --- GOSSIPING (SINGLE THREAD) ---

	protected Random rnd = new Random();

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

		// TODO TEST
		TcpEndpoint endpoint = new TcpEndpoint("server-2", "127.0.0.1", 7328);
		try {
			byte[] packet = serialize(PACKET_EVENT_ID, new CheckedTree("hello"));
			System.out.println("SEND: " + new String(packet));
			writer.send(endpoint, packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void sendGossipToRandomEndpoint(ArrayList<TcpEndpoint> endpoints, ArrayList<TcpEndpoint> rest) {
		int size = endpoints.size();
		if (size < 1) {
			return;
		}

		// Create message
		Tree message = new Tree();
		Tree nodes = message.putList("nodes");
		for (TcpEndpoint endpoint : endpoints) {
			endpoint.writeStatus(nodes.addMap());
		}
		for (TcpEndpoint endpoint : rest) {
			endpoint.writeStatus(nodes.addMap());
		}

		// Choose random endpoint
		int index = rnd.nextInt(size);
		TcpEndpoint endpoint = endpoints.get(index);

		// Create data packet to send
		try {

			// Create gossip packet
			byte[] packet = serialize(PACKET_GOSSIP_REQUEST_ID, message);

			// Send gossip request to node
			writer.send(endpoint, packet);

		} catch (Exception cause) {
			logger.error("Unable to serialize data!", cause);
		}
	}

	// --- ENDPOINT CACHE (MULTI-THREAD) ---

	protected final AtomicReference<ArrayList<TcpEndpoint>> cachedLiveEndpoints = new AtomicReference<>();
	protected final AtomicReference<ArrayList<TcpEndpoint>> cachedUnreachableEndpoints = new AtomicReference<>();

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
				if (endpoint.isOnline()) {
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

	// --- GETTERS AND SETTERS ---

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int timeout) {
		this.socketTimeout = timeout;
	}

	public int getGossipingSeconds() {
		return gossipingSeconds;
	}

	public void setGossipingSeconds(int gossipingSeconds) {
		this.gossipingSeconds = gossipingSeconds;
	}

}