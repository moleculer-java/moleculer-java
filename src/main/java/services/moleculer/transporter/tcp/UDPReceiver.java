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
package services.moleculer.transporter.tcp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import services.moleculer.transporter.TcpTransporter;

public abstract class UDPReceiver {

	// --- LOGGER ---

	protected static final Logger logger = LoggerFactory.getLogger(UDPReceiver.class);
	
	// --- PROPERTIES ---
		
	/**
	 * Current namespace
	 */
	protected final String namespace;
	
	/**
	 * Debug mode
	 */
	protected final boolean debug;
	
	/**
	 * Use hostnames instead of IP addresses As the DHCP environment is dynamic,
	 * any later attempt to use IPs instead hostnames would most likely yield
	 * false results. Therefore, use hostnames if you are using DHCP.
	 */
	protected final boolean useHostname;
	
	/**
	 * Current NodeID
	 */
	protected final String nodeID;

	/**
	 * IP address
	 */
	protected final String udpAddress;
	
	/**
	 * Resuse addresses
	 */
	protected final boolean udpReuseAddr;

	/**
	 * UDP broadcast/multicast port
	 */
	protected final int udpPort;

	/**
	 * UDP multicast TTL
	 */
	protected final int udpMulticastTTL;
	
	/**
	 * TCP port (used by the Transporter and Gossiper services)
	 */
	protected final int port;

	// --- RECEIVER'S EXECUTOR ---

	/**
	 * Receiver's executor
	 */
	protected ExecutorService executor;
	
	// --- COMPONENTS ---

	/**
	 * Parent transporter
	 */
	protected final TcpTransporter transporter;
	
	// --- CONSTRUCTOR ---
	
	protected UDPReceiver(String nodeID, String udpAddress, TcpTransporter transporter) {
		this.nodeID = nodeID;
		this.udpAddress = udpAddress;
		this.transporter = transporter;
		this.namespace = transporter.getNamespace();
		this.debug = transporter.isDebug();
		this.useHostname = transporter.isUseHostname();
		this.udpReuseAddr = transporter.isUdpReuseAddr();
		this.udpPort = transporter.getUdpPort();
		this.udpMulticastTTL = transporter.getUdpMulticastTTL();
		this.port = transporter.getCurrentPort();
	}
	
	// --- CONNECT ---
	
	protected void connect() throws Exception {
		
		// Start packet receiver
		executor = Executors.newSingleThreadExecutor();
		executor.execute(this::receive);
	}

	// --- DISCONNECT ---

	protected void disconnect() {
		
		// Stop receiver thread
		if (executor != null) {
			try {
				executor.shutdownNow();
			} catch (Exception ignored) {
			}
			executor = null;
		}
	}

	// --- MESSAGE SENDER ---
	
	protected abstract void send();
	
	// --- MESSAGE RECEIVER ---
	
	protected abstract void receive();
	
	// --- COMMON MESSAGE PROCESSOR ---

	protected void processReceivedMessage(DatagramPacket packet, byte[] buffer) {

		String received = new String(buffer).trim();
		String tokens[] = received.split("\\|");
		if (tokens.length != 3) {
			logger.warn("Malformed UDP message received (" + received + ")!");
			return;
		}

		// Check prefix and nodeID
		if (!namespace.equals(tokens[0]) || nodeID.equals(tokens[1])) {
			return;
		}
		if (tokens[1].isEmpty()) {
			logger.warn("Empty nodeID received!");
			return;
		}

		// Get source hostname or IP
		String host;
		try {
			InetAddress address = packet.getAddress();
			if (useHostname) {
				host = address.getHostName();
				if (host == null || host.isEmpty() || host.contains("localhost")) {
					host = address.getHostAddress();
				}
			} else {
				host = address.getHostAddress();
				if (host == null || host.isEmpty() || host.startsWith("127.")) {
					host = address.getHostName();
				}
			}
			if (host == null || host.isEmpty()) {
				logger.warn("Unable to detect sender's hostname!");
				return;
			}
		} catch (Exception cause) {
			logger.warn("Unable to detect sender's IP address or hostname!", cause);
			return;
		}
		int port = 0;
		try {
			port = Integer.parseInt(tokens[2]);
			if (port < 1) {
				logger.warn("Invalid port number (" + port + ")!");
				return;
			}
		} catch (Exception cause) {
			logger.warn("Invalid port number format (" + tokens[2] + ")!");
			return;
		}

		// Notify TCP Transporter
		transporter.udpPacketReceived(tokens[1], host.toLowerCase(), port);
	}
	
}
