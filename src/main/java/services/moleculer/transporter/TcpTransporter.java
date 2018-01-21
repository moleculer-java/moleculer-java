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
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * TCP Transporter. Now it's just an empty sketch (it doesn't work).
 */
@Name("TCP Transporter")
public class TcpTransporter extends Transporter {

	// --- PROPERTIES ---

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

			// Create server socket

			// Ok, created
			logger.info("TCP pub-sub connection estabilished.");
			connected();
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
		disconnect();
	}

	// --- SUBSCRIBE ---

	@Override
	public Promise subscribe(String channel) {

		// Do nothing
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

				// Create packet
				byte[] data = serializer.write(message);
				byte[] packet = new byte[data.length + 5];
				packet[3] = (byte) packet.length;
				packet[2] = (byte) (packet.length >>> 8);
				packet[1] = (byte) (packet.length >>> 16);
				packet[0] = (byte) (packet.length >>> 32);
				switch (command) {
				case PACKET_EVENT:
					packet[4] = 1;
					break;
				case PACKET_REQUEST:
					packet[4] = 2;
					break;
				case PACKET_RESPONSE:
					packet[4] = 3;
					break;
				case PACKET_DISCOVER:
					packet[4] = 4;
					break;
				case PACKET_INFO:
					packet[4] = 5;
					break;
				case PACKET_DISCONNECT:
					packet[4] = 6;
					break;
				case PACKET_HEARTBEAT:
					packet[4] = 7;
					break;
				case PACKET_PING:
					packet[4] = 8;
					break;
				case PACKET_PONG:
					packet[4] = 9;
					break;
				default:
					logger.warn("Invalid command (" + command + ")!");
					return;
				}

				// Send to node(s)
				if (nodeID == null) {
					sendToAllNodes(packet);
				} else {
					sendToNode(nodeID, packet);
				}

			} catch (Exception cause) {
				logger.warn("Unable to send message!", cause);
				reconnect();
			}
		}
	}

	protected void sendToAllNodes(byte[] packet) {
		
		// Loop on registered nodeIDs 
		// sendToNode(...)
		
	}
	
	protected void sendToNode(String nodeID, byte[] packet) {
		TcpWriteBuffer writeBuffer = writeBuffers.get(nodeID);
		if (writeBuffer == null) {
			
			// Create buffer
			// NodeID -> IP address + port
			
		}
		writeBuffer.write(packet);
	}

}