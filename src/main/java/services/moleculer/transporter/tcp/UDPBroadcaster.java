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
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import services.moleculer.transporter.TcpTransporter;

/**
 * UDP multicast / broadcast discovery service of the TCP Transporter. Use the
 * "udpMulticast" boolean parameter, to switch to multicast from broadcast.
 */
public class UDPBroadcaster {

	// --- LOGGER ---

	protected static final Logger logger = LoggerFactory.getLogger(UDPBroadcaster.class);

	// --- PROPERTIES ---

	/**
	 * Current namespace
	 */
	protected final String namespace;

	/**
	 * TCP port (used by the Transporter and Gossiper services)
	 */
	protected final int port;

	/**
	 * Use UDP multicast or UDP broadcast
	 */
	protected final boolean udpMulticast;

	/**
	 * Maximum number of outgoing multicast packets (0 = runs forever)
	 */
	protected final int udpMaxDiscovery;

	/**
	 * UDP broadcast/multicast period in SECONDS
	 */
	protected final int udpPeriod;

	/**
	 * UDP broadcast/multicast address of automatic discovery service. The
	 * preferred broadcast address is "255.255.255.255", use "230.0.0.0" in
	 * multicast mode.
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
	 * TTL of UDP packets
	 */
	protected final int udpTTL;

	/**
	 * Debug mode
	 */
	protected final boolean debug;

	/**
	 * Current NodeID
	 */
	protected final String nodeID;

	/**
	 * Use hostnames instead of IP addresses As the DHCP environment is dynamic,
	 * any later attempt to use IPs instead hostnames would most likely yield
	 * false results. Therefore, use hostnames if you are using DHCP.
	 */
	protected final boolean useHostname;

	// --- COMPONENTS ---

	/**
	 * Sender's executor
	 */
	protected final ScheduledExecutorService scheduler;

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

	public UDPBroadcaster(String nodeID, TcpTransporter transporter, ScheduledExecutorService scheduler) {
		this.nodeID = nodeID;
		this.transporter = transporter;
		this.scheduler = scheduler;
		this.namespace = transporter.getNamespace();
		this.debug = transporter.isDebug();
		this.port = transporter.getCurrentPort();
		this.useHostname = transporter.isUseHostname();
		this.udpMulticast = transporter.isUdpMulticast();
		this.udpMaxDiscovery = transporter.getUdpMaxDiscovery();
		this.udpPeriod = transporter.getUdpPeriod();
		this.udpAddress = transporter.getUdpAddress();
		this.udpReuseAddr = transporter.isUdpReuseAddr();
		this.udpPort = transporter.getUdpPort();
		this.udpTTL = transporter.getUdpTTL();
	}

	// --- CONNECT ---

	/**
	 * Cancelable timer of sender
	 */
	protected volatile ScheduledFuture<?> timer;

	/**
	 * Multicast receiver
	 */
	protected MulticastSocket multicastReceiver;

	/**
	 * Broadcast receiver
	 */
	protected DatagramSocket broadcastReceiver;

	public void connect() throws Exception {
		disconnect();

		// Start packet receiver
		executor = Executors.newSingleThreadExecutor();
		if (udpMulticast) {

			// Start multicast receiver
			multicastReceiver = new MulticastSocket(udpPort);
			multicastReceiver.joinGroup(InetAddress.getByName(udpAddress));
			executor.execute(this::receiveMulticast);
			logger.info("Multicast discovery service started on udp://" + udpAddress + ':' + udpPort + '.');
		} else {

			// Start broadcast receiver
			broadcastReceiver = new DatagramSocket();
			broadcastReceiver.setBroadcast(true);
			broadcastReceiver.connect(InetAddress.getByName(udpAddress), udpPort);
			executor.execute(this::receiveBroadcast);
			logger.info("Broadcast discovery service started on udp://" + udpAddress + ':' + udpPort + '.');
		}

		// Start multicast / broadcast sender
		timer = scheduler.scheduleAtFixedRate(this::send, 1, udpPeriod, TimeUnit.SECONDS);
	}

	// --- DISCONNECT ---

	@Override
	protected void finalize() throws Throwable {
		disconnect();
	}

	public void disconnect() {

		// Close timer
		if (timer != null) {
			timer.cancel(true);
			timer = null;
		}

		// Stop sender
		if (executor != null) {
			try {
				executor.shutdownNow();
			} catch (Exception ignored) {
			}
			executor = null;
		}

		// Stop multicast receiver
		if (multicastReceiver != null) {
			try {
				InetAddress address = InetAddress.getByName(udpAddress);
				multicastReceiver.leaveGroup(address);
			} catch (Exception ignored) {
			}
			try {
				multicastReceiver.close();
			} catch (Exception ignored) {
			}
			multicastReceiver = null;
			logger.info("Multicast discovery service stopped.");
		}

		// Stop broadcast receiver
		if (broadcastReceiver != null) {
			try {
				broadcastReceiver.close();
			} catch (Exception ignored) {
			}
			broadcastReceiver = null;
			logger.info("Broadcast discovery service stopped.");
		}
	}

	// --- UDP BROADCAST / MULTICAST SENDER ---

	protected int numberOfSubmittedPackets = 0;

	protected void send() {

		// Check number of packets
		if (udpMaxDiscovery > 0) {
			if (numberOfSubmittedPackets >= udpMaxDiscovery) {
				if (timer != null) {
					logger.info("Discovery service stopped successfully, " + udpMaxDiscovery + " packets sent.");
					timer.cancel(false);
					timer = null;
				}
				return;
			}
			numberOfSubmittedPackets++;
		}
		String msg = namespace + '|' + nodeID + '|' + port;
		if (udpMulticast) {

			// Send multicast packet
			MulticastSocket udpSocket = null;
			try {
				byte[] bytes = msg.getBytes();
				udpSocket = new MulticastSocket(udpPort);
				udpSocket.setTimeToLive(udpTTL);
				udpSocket.setReuseAddress(udpReuseAddr);
				InetAddress address = InetAddress.getByName(udpAddress);
				DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, udpPort);
				udpSocket.send(packet);
				if (debug) {
					logger.info("Multicast message submitted (namespace: " + namespace + ", node ID: " + nodeID
							+ ", port: " + port + ").");
				}
			} catch (Exception cause) {
				logger.error("Unable to send multicast packet!", cause);
			} finally {
				try {
					if (udpSocket != null) {
						udpSocket.close();
					}
					udpSocket = null;
				} catch (Exception ignored) {
				}
			}
		} else {

			// Send broadcast packet
			DatagramSocket udpSocket = null;
			try {
				byte[] bytes = msg.getBytes();
				udpSocket = new DatagramSocket();
				udpSocket.setBroadcast(true);
				udpSocket.setReuseAddress(udpReuseAddr);
				InetAddress address = InetAddress.getByName(udpAddress);
				DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, udpPort);
				udpSocket.send(packet);
				if (debug) {
					logger.info("Broadcast message submitted (namespace: " + namespace + ", node ID: " + nodeID
							+ ", port: " + port + ").");
				}
			} catch (Exception cause) {
				logger.error("Unable to send broadcast packet!", cause);
			} finally {
				try {
					if (udpSocket != null) {
						udpSocket.close();
					}
					udpSocket = null;
				} catch (Exception ignored) {
				}
			}
		}
	}

	// --- UDP BROADCAST RECEIVER ---

	protected void receiveBroadcast() {
		while (!Thread.currentThread().isInterrupted()) {
			try {

				// Waiting for packet...
				byte[] buffer = new byte[512];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				broadcastReceiver.receive(packet);
				processReceivedMessage(packet, buffer);

			} catch (Exception cause) {
				String msg = cause == null ? null : cause.getMessage();
				if (msg != null && msg.contains("closed")) {
					return;
				}
				logger.warn("Unexpected error occured in UDP broadcaster!", cause);
				if (broadcastReceiver != null) {
					try {
						broadcastReceiver.close();
					} catch (Exception ignored) {
					}
				}
				try {
					Thread.sleep(1000);
					broadcastReceiver = new DatagramSocket();
					broadcastReceiver.setBroadcast(true);
					broadcastReceiver.connect(InetAddress.getByName(udpAddress), udpPort);
					logger.info("UDP broadcast discovery service reconnected.");
				} catch (Exception interrupt) {
					return;
				}
			}
		}
	}

	// --- UDP MULTICAST RECEIVER ---

	protected void receiveMulticast() {
		while (!Thread.currentThread().isInterrupted()) {
			try {

				// Waiting for packet...
				byte[] buffer = new byte[512];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				multicastReceiver.receive(packet);
				processReceivedMessage(packet, buffer);

			} catch (Exception cause) {
				String msg = cause == null ? null : cause.getMessage();
				if (msg != null && msg.contains("closed")) {
					return;
				}
				logger.warn("Unexpected error occured in UDP broadcaster!", cause);
				if (multicastReceiver != null) {
					try {
						InetAddress address = InetAddress.getByName(udpAddress);
						multicastReceiver.leaveGroup(address);
					} catch (Exception ignored) {
					}
					try {
						multicastReceiver.close();
					} catch (Exception ignored) {
					}
				}
				try {
					Thread.sleep(1000);
					multicastReceiver = new MulticastSocket(udpPort);
					multicastReceiver.joinGroup(InetAddress.getByName(udpAddress));
					logger.info("UDP multicast discovery service reconnected.");
				} catch (Exception interrupt) {
					return;
				}
			}
		}
	}

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