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

import services.moleculer.monitor.Monitor;
import services.moleculer.transporter.TcpTransporter;

/**
 * UDP broadcaster / receiver of the TCP Transporter.
 */
public final class UDPBroadcaster {

	// --- LOGGER ---

	private static final Logger logger = LoggerFactory.getLogger(UDPBroadcaster.class);

	// --- PROPERTIES ---

	/**
	 * TCP port (used by the Transporter and Gossiper services)
	 */
	protected final int port;
	
	/**
	 * UDP multicast host of automatic discovery service
	 */
	private final String broadcastHost;

	/**
	 * UDP multicast port of automatic discovery service
	 */
	private final int broadcastPort;

	/**
	 * UDP broadcast period in SECONDS
	 */
	protected final int broadcastPeriod;

	/**
	 * Debug monde
	 */
	private final boolean debug;

	/**
	 * Current NodeID
	 */
	private final String nodeID;

	// --- COMPONENTS ---

	/**
	 * Sender's executor
	 */
	private final ScheduledExecutorService scheduler;

	/**
	 * CPU monitor
	 */
	private final Monitor monitor;

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

	public UDPBroadcaster(String nodeID, TcpTransporter transporter, ScheduledExecutorService scheduler,
			Monitor monitor) {
		this.nodeID = nodeID;
		this.transporter = transporter;
		this.scheduler = scheduler;
		this.monitor = monitor;
		this.debug = transporter.isDebug();
		this.broadcastHost = transporter.getMulticastHost();
		this.broadcastPort = transporter.getMulticastPort();
		this.broadcastPeriod = transporter.getMulticastPeriod();
		this.port = transporter.getPort();
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
		udpReceiver = new MulticastSocket(broadcastPort);
		udpReceiver.joinGroup(InetAddress.getByName(broadcastHost));
		logger.info("Discovery service started on udp://" + broadcastHost + ':' + broadcastPort + '.');

		// Start packet receiver
		executor = Executors.newSingleThreadExecutor();
		executor.execute(this::receive);

		// Start packet sender
		timer = scheduler.scheduleAtFixedRate(this::send, 1, broadcastPeriod, TimeUnit.SECONDS);
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
				InetAddress address = InetAddress.getByName(broadcastHost);
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

	private final void send() {
		MulticastSocket udpSocket = null;
		try {
			String msg = nodeID + ':' + monitor.getTotalCpuPercent() + ':' + port;
			byte[] bytes = msg.getBytes();
			udpSocket = new MulticastSocket(broadcastPort);
			InetAddress address = InetAddress.getByName(broadcastHost);
			DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, broadcastPort);
			udpSocket.send(packet);
			if (debug) {
				logger.info("UDP message submitted: " + msg);
			}
		} catch (Exception cause) {
			logger.error("Unable to send UDP packet!", cause);
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

		// Variables
		byte[] buf = new byte[512];

		// Loop
		while (!Thread.currentThread().isInterrupted()) {
			try {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				udpReceiver.receive(packet);
				byte[] data = packet.getData();
				if (data == null) {
					continue;
				}
				String received = new String(data).trim();
				if (debug) {
					logger.info("UDP message received: " + received);
				}
				String tokens[] = received.split(":");
				if (tokens.length != 3) {
					continue;
				}
				if (nodeID.equals(tokens[0])) {
					continue;
				}
				int cpu = 0;
				try {
					cpu = Integer.parseInt(tokens[1]);
				} catch (Exception cause) {
					logger.warn("Invalid CPU usage format (" + tokens[1] + ")!");
				}
				String host = packet.getAddress().getHostAddress();
				int port = 0;
				try {
					port = Integer.parseInt(tokens[2]);
				} catch (Exception cause) {
					logger.warn("Invalid port number format (" + tokens[2] + ")!");
				}
				
				// Notify TCP Transporter
				transporter.udpPacketReceiver(tokens[0], cpu, host, port);
			
			} catch (Exception cause) {
				String msg = cause == null ? null : cause.getMessage();
				if (msg != null && msg.contains("closed")) {
					return;
				}
				logger.warn("Unexpected error occured in UDP broadcaster!", cause);
				if (udpReceiver != null) {
					try {
						InetAddress address = InetAddress.getByName(broadcastHost);
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
					udpReceiver = new MulticastSocket(broadcastPort);
					udpReceiver.joinGroup(InetAddress.getByName(broadcastHost));
					logger.info("UDP broadcaster reconnected.");
				} catch (Exception interrupt) {
					return;
				}
			}
		}
	}
}
