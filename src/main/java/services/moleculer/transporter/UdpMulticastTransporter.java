/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.service.Name;

/**
 * Multicast UDP-based, server-less Transporter. It cannot transfer large
 * amounts of data in one package; 64 kilobytes is the theoretical maximum size
 * of a complete IP datagram, but only 576 bytes are guaranteed to be routed.
 * UDP does not behave well in a lossy network by itself. Can only be used
 * safely if two or three nodes are connected via "localhost".<br>
 * <br>
 * If you need a RELIABLE data transport without central server (i.e. no loss of
 * data, forwarding large messages) then TCPTransporter is the suitable option.
 * Usage:
 * 
 * <pre>
 * ServiceBroker broker = ServiceBroker.builder()
 *                                     .nodeID("node1")
 *                                     .transporter(new UdpMulticastTransporter())
 *                                     .build();
 * </pre>
 *
 * @see AblyTransporter
 * @see TcpTransporter
 * @see AmqpTransporter
 * @see RedisTransporter
 * @see NatsTransporter
 * @see MqttTransporter
 * @see JmsTransporter
 * @see GoogleTransporter
 * @see KafkaTransporter
 */
@Name("UDP Multicast Transporter")
public class UdpMulticastTransporter extends Transporter {

	// --- VARIABLES ---

	/**
	 * UDP bind address (null = autodetect)
	 */
	protected String bindAddress;

	/**
	 * UDP multicast address
	 */
	protected String address = "239.0.0.0";

	/**
	 * Resuse addresses
	 */
	protected boolean reuseAddr = true;

	/**
	 * UDP multicast port
	 */
	protected int port = 4450;

	/**
	 * UDP multicast TTL
	 */
	protected int multicastTTL = 1;

	/**
	 * 64 kilobytes is the theoretical maximum size of a complete IP datagram,
	 * but only 576 bytes are guaranteed to be routed
	 */
	protected int bufferSize = 64 * 1024;

	/**
	 * Subscribed channels
	 */
	protected HashSet<String> subscriptions = new HashSet<>();

	/**
	 * Subscribed channels
	 */
	protected LinkedList<MulticastReceiver> receivers = new LinkedList<>();

	// --- EXECUTOR ---

	protected ExecutorService executor;

	// --- CONNECT ---

	@Override
	public void connect() {
		try {

			// Create client connection
			disconnect();
			executor = Executors.newCachedThreadPool();
			synchronized (receivers) {
				if (bindAddress != null && !bindAddress.isEmpty()) {

					// Create receiver for a NetworkInterface
					InetAddress address = InetAddress.getByName(bindAddress);
					startReceivers(NetworkInterface.getByInetAddress(address));
				} else {

					// Create receivers for all NetworkInterfaces
					Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
					while (en.hasMoreElements()) {
						startReceivers(en.nextElement());
					}

				}
			}
			logger.info("Multicast UDP pub-sub connection estabilished.");
			connected();
		} catch (Exception cause) {
			String msg = cause.getMessage();
			if (msg == null || msg.isEmpty()) {
				msg = "Unable to connect to Multicast UDP Socket!";
			} else if (!msg.endsWith("!") && !msg.endsWith(".")) {
				msg += "!";
			}
			logger.warn(msg);
			reconnect();
		}
	}

	protected void startReceivers(NetworkInterface ni) throws Exception {
		try {
			if (ni == null || ni.isLoopback() || !ni.supportsMulticast()) {
				return;
			}
			List<InterfaceAddress> list = ni.getInterfaceAddresses();
			if (list == null || list.isEmpty()) {
				return;
			}

			// Create multicast receiver
			MulticastReceiver receiver = new MulticastReceiver(this, ni);
			synchronized (receivers) {
				receivers.add(receiver);
			}
			executor.execute(receiver);
		} catch (Exception cause) {
			String msg = cause.getMessage();
			if (msg != null && msg.contains("Inet4Address")) {
				return;
			}
			throw cause;
		}
	}

	// --- DISCONNECT ---

	protected void disconnect() {
		boolean notify = false;
		synchronized (receivers) {
			notify = !receivers.isEmpty();
			if (notify) {
				for (MulticastReceiver receiver : receivers) {
					receiver.close();
				}
				receivers.clear();
			}
		}
		if (executor != null) {
			notify = true;
			executor.shutdownNow();
			executor = null;
		}
		subscriptions.clear();

		// Notify internal listeners
		if (notify) {
			broadcastTransporterDisconnected();
		}
	}

	// --- STOP INSTANCE ---
	
	@Override
	public void stopped() {
		super.stopped();
		disconnect();
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
		logger.warn("Unexpected communication error occurred!", cause);
		reconnect();
	}

	// --- SEND UDP PACKET ---

	@Override
	public void publish(String channel, Tree message) {

		// Send multicast packet
		MulticastSocket udpSocket = null;
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream(bufferSize);
			DataOutputStream out = new DataOutputStream(buffer);
			out.writeUTF(channel);
			byte[] messageBytes = serializer.write(message);
			out.writeInt(messageBytes.length);
			out.write(messageBytes);
			byte[] bytes = buffer.toByteArray();
			udpSocket = new MulticastSocket(port);
			udpSocket.setTimeToLive(multicastTTL);
			udpSocket.setReuseAddress(reuseAddr);
			InetAddress inetAddress = InetAddress.getByName(address);
			DatagramPacket packet = new DatagramPacket(bytes, bytes.length, inetAddress, port);
			udpSocket.send(packet);
			if (debug) {
				logger.info("Multicast UDP message submitted (namespace: " + namespace + ", node ID: " + nodeID
						+ ", port: " + port + ").");
			}
		} catch (Exception cause) {
			logger.error("Unable to send Multicast UDP Packet!", cause);
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

	// --- REGISTER CHANNEL ---

	@Override
	public Promise subscribe(String channel) {

		// Synchronization is unnecessary (subscriptions will not change)
		subscriptions.add(channel);
		if (debug) {
			logger.info("Channel \"" + channel + "\" registered.");
		}
		return Promise.resolve();
	}

	// --- UDP PACKET RECEIVER ---

	protected static class MulticastReceiver implements Runnable {

		// --- LOGGER ---

		protected static final Logger logger = LoggerFactory.getLogger(MulticastReceiver.class);

		// --- PARENT TRANSPORTER ---

		protected UdpMulticastTransporter transporter;

		// --- MULTICAST SOCKET ---

		/**
		 * Multicast receiver
		 */
		protected MulticastSocket multicastReceiver;

		public MulticastReceiver(UdpMulticastTransporter transporter, NetworkInterface netIf) throws IOException {
			this.transporter = transporter;

			// Create client connection
			multicastReceiver = new MulticastSocket(transporter.port);
			multicastReceiver.setReuseAddress(transporter.reuseAddr);

			InetAddress inetAddress = InetAddress.getByName(transporter.address);
			if (netIf == null) {
				multicastReceiver.joinGroup(inetAddress);
			} else {
				InetSocketAddress socketAddress = new InetSocketAddress(inetAddress, transporter.port);
				multicastReceiver.joinGroup(socketAddress, netIf);
			}

			// Log
			String msg = "Multicast UDP Transporter started on udp://" + transporter.address + ':' + transporter.port;
			if (netIf == null) {
				logger.info(msg + '.');
			} else {
				logger.info(msg + " (" + netIf.getDisplayName() + ").");
			}
		}

		public void close() {
			if (multicastReceiver != null) {
				try {
					multicastReceiver.close();
				} catch (Exception ignored) {

					// Do nothing
				}
				multicastReceiver = null;
			}
		}

		public void run() {
			while (!Thread.currentThread().isInterrupted() && multicastReceiver != null) {
				try {

					// Waiting for packet...
					byte[] bytes = new byte[transporter.bufferSize];
					DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
					multicastReceiver.receive(packet);
					ByteArrayInputStream buffer = new ByteArrayInputStream(bytes);
					DataInputStream in = new DataInputStream(buffer);
					String channel = in.readUTF();
					int len = in.readInt();
					byte[] message = new byte[len];
					in.readFully(message);

					// Synchronization is unnecessary (subscriptions will not
					// change)
					if (!transporter.subscriptions.contains(channel)) {
						continue;
					}
					if (transporter.debug) {
						logger.info("Multicast UDP message received (channel: " + channel + ", destination nodeID: "
								+ transporter.nodeID + ").");
					}

					// Process message
					transporter.received(channel, message);

				} catch (Exception cause) {
					String msg = cause == null ? null : cause.getMessage();
					if (msg != null && msg.contains("closed")) {
						return;
					}
					logger.warn("Unexpected error occurred in UDP broadcaster!", cause);
					try {
						Thread.sleep(1000);
					} catch (Exception interrupt) {
						return;
					}
				}
			}
		}

	}

}