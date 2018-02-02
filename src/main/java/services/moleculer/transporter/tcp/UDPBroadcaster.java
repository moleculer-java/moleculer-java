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
 * UDP broadcaster / receiver of the TCP Transporter.
 */
public final class UDPBroadcaster {

	// --- LOGGER ---

	private static final Logger logger = LoggerFactory.getLogger(UDPBroadcaster.class);

	// --- PROPERTIES ---

	/**
	 * Current namespace
	 */
	private final String namespace;

	/**
	 * TCP port (used by the Transporter and Gossiper services)
	 */
	private final int port;

	/**
	 * UDP multicast host of automatic discovery service
	 */
	private final String multicastHost;

	/**
	 * UDP multicast port of automatic discovery service
	 */
	private final int multicastPort;

	/**
	 * UDP multicast period in SECONDS
	 */
	private final int multicastPeriod;

	/**
	 * Debug mode
	 */
	private final boolean debug;

	/**
	 * Current NodeID
	 */
	private final String nodeID;

	/**
	 * Use hostnames instead of IP addresses As the DHCP environment is dynamic,
	 * any later attempt to use IPs instead hostnames would most likely yield
	 * false results. Therefore, use hostnames if you are using DHCP.
	 */
	private final boolean useHostname;

	/**
	 * Maximum number of outgoing multicast packets (0 = runs forever)
	 */
	protected final int multicastPackets;

	// --- COMPONENTS ---

	/**
	 * Sender's executor
	 */
	private final ScheduledExecutorService scheduler;

	// --- RECEIVER'S EXECUTOR ---

	/**
	 * Receiver's executor
	 */
	private ExecutorService executor;

	// --- COMPONENTS ---

	/**
	 * Parent transporter
	 */
	private final TcpTransporter transporter;

	// --- CONSTRUCTOR ---

	public UDPBroadcaster(String namespace, String nodeID, TcpTransporter transporter,
			ScheduledExecutorService scheduler) {
		this.namespace = namespace;
		this.nodeID = nodeID;
		this.transporter = transporter;
		this.scheduler = scheduler;
		this.debug = transporter.isDebug();
		this.multicastHost = transporter.getMulticastHost();
		this.multicastPort = transporter.getMulticastPort();
		this.multicastPeriod = transporter.getMulticastPeriod();
		this.port = transporter.getCurrentPort();
		this.useHostname = transporter.isUseHostname();
		this.multicastPackets = transporter.getMulticastPackets();
	}

	// --- CONNECT ---

	/**
	 * Cancelable timer of sender
	 */
	private volatile ScheduledFuture<?> timer;

	/**
	 * Multicast receiver
	 */
	private MulticastSocket udpReceiver;

	public final void connect() throws Exception {

		// Join UDP group
		disconnect();
		udpReceiver = new MulticastSocket(multicastPort);
		udpReceiver.joinGroup(InetAddress.getByName(multicastHost));
		logger.info("Discovery service started on udp://" + multicastHost + ':' + multicastPort + '.');

		// Start packet receiver
		executor = Executors.newSingleThreadExecutor();
		executor.execute(this::receive);

		// Start packet sender
		timer = scheduler.scheduleAtFixedRate(this::send, 1, multicastPeriod, TimeUnit.SECONDS);
	}

	// --- DISCONNECT ---

	@Override
	protected final void finalize() throws Throwable {
		disconnect();
	}

	public final void disconnect() {

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

		// Stop receiver
		if (udpReceiver != null) {
			try {
				InetAddress address = InetAddress.getByName(multicastHost);
				udpReceiver.leaveGroup(address);
			} catch (Exception ignored) {
			}
			try {
				udpReceiver.close();
			} catch (Exception ignored) {
			}
			udpReceiver = null;
			logger.info("Discovery service stopped.");
		}

	}

	// --- UDP SENDER'S LOOP ---

	private int numberOfSubmittedPackets = 0;

	private final void send() {

		// Check number of packets
		if (multicastPackets > 0) {
			if (numberOfSubmittedPackets >= multicastPackets) {
				if (timer != null) {
					logger.info("Discovery sender stopped successfully, " + multicastPackets + " packets sent.");
					timer.cancel(false);
					timer = null;
				}
				return;
			}
			numberOfSubmittedPackets++;
		}

		// Send packet
		MulticastSocket udpSocket = null;
		try {
			String msg = namespace + '|' + nodeID + '|' + port;
			byte[] bytes = msg.getBytes();
			udpSocket = new MulticastSocket(multicastPort);
			InetAddress address = InetAddress.getByName(multicastHost);
			DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, multicastPort);
			udpSocket.send(packet);
			if (debug) {
				logger.info("Discovery message submitted (namespace: " + namespace + ", node ID: " + nodeID + ", port: "
						+ port + ").");
			}
		} catch (Exception cause) {
			logger.error("Unable to send discovery packet!", cause);
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

	// --- UDP SENDER'S LOOP ---

	private final void receive() {

		// Loop
		while (!Thread.currentThread().isInterrupted()) {
			try {

				// Waiting for packet...
				byte[] buffer = new byte[512];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				udpReceiver.receive(packet);
				String received = new String(buffer).trim();
				String tokens[] = received.split("\\|");
				if (tokens.length != 3) {
					logger.warn("Malformed UDP message received (" + received + ")!");
					continue;
				}

				// Check prefix and nodeID
				if (!namespace.equals(tokens[0]) || nodeID.equals(tokens[1])) {
					continue;
				}
				if (tokens[1].isEmpty()) {
					logger.warn("Empty nodeID received!");
					continue;
				}

				// Get source hostname or IP
				String host;
				try {
					InetAddress address = packet.getAddress();
					if (useHostname) {
						host = address.getHostName();
						if (host == null || host.isEmpty()) {
							host = address.getHostAddress();
						}
					} else {
						host = address.getHostAddress();
						if (host == null || host.isEmpty()) {
							host = address.getHostName();
						}
					}
					if (host == null || host.isEmpty()) {
						logger.warn("Unable to detect sender's hostname!");
						continue;
					}
				} catch (Exception cause) {
					logger.warn("Unable to detect sender's IP address or hostname!", cause);
					continue;
				}
				int port = 0;
				try {
					port = Integer.parseInt(tokens[2]);
					if (port < 1) {
						logger.warn("Invalid port number (" + port + ")!");
						continue;
					}
				} catch (Exception cause) {
					logger.warn("Invalid port number format (" + tokens[2] + ")!");
					continue;
				}

				// Notify TCP Transporter
				transporter.udpPacketReceived(tokens[1], host.toLowerCase(), port);

			} catch (Exception cause) {
				String msg = cause == null ? null : cause.getMessage();
				if (msg != null && msg.contains("closed")) {
					return;
				}
				logger.warn("Unexpected error occured in UDP broadcaster!", cause);
				if (udpReceiver != null) {
					try {
						InetAddress address = InetAddress.getByName(multicastHost);
						udpReceiver.leaveGroup(address);
					} catch (Exception ignored) {
					}
					try {
						udpReceiver.close();
					} catch (Exception ignored) {
					}
				}
				try {
					Thread.sleep(1000);
					udpReceiver = new MulticastSocket(multicastPort);
					udpReceiver.joinGroup(InetAddress.getByName(multicastHost));
					logger.info("UDP broadcaster reconnected.");
				} catch (Exception interrupt) {
					return;
				}
			}
		}
	}
}
