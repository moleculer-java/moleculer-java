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

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
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
public class UDPLocator {

	// --- LOGGER ---

	protected static final Logger logger = LoggerFactory.getLogger(UDPLocator.class);

	// --- PROPERTIES ---

	/**
	 * Current NodeID
	 */
	protected final String nodeID;

	/**
	 * Use UDP broadcast WITH UDP multicast (false = use UDP multicast only)
	 */
	protected final boolean udpBroadcast;

	/**
	 * Maximum number of outgoing multicast packets (0 = runs forever)
	 */
	protected final int udpMaxDiscovery;

	/**
	 * UDP broadcast/multicast period in SECONDS
	 */
	protected final int udpPeriod;

	// --- COMPONENTS ---

	/**
	 * Sender's executor
	 */
	protected final ScheduledExecutorService scheduler;

	/**
	 * Parent transporter
	 */
	protected final TcpTransporter transporter;

	// --- LIST OF RUNNING LOCATORS ---

	protected final ArrayList<UDPReceiver> receivers = new ArrayList<>();

	// --- CONSTRUCTOR ---

	public UDPLocator(String nodeID, TcpTransporter transporter, ScheduledExecutorService scheduler) {
		this.nodeID = nodeID;
		this.transporter = transporter;
		this.scheduler = scheduler;
		this.udpMaxDiscovery = transporter.getUdpMaxDiscovery();
		this.udpPeriod = transporter.getUdpPeriod();
		this.udpBroadcast = transporter.isUdpBroadcast();
	}

	// --- CONNECT ---

	/**
	 * Cancelable timer of sender
	 */
	protected volatile ScheduledFuture<?> timer;

	public void connect() throws Exception {
		disconnect();
		synchronized (receivers) {

			// Create multicast receiver
			receivers.add(new UDPMulticastReceiver(nodeID, transporter));

			// Create broadcast receivers
			if (udpBroadcast) {
				Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
				while (en.hasMoreElements()) {

					NetworkInterface ni = en.nextElement();
					List<InterfaceAddress> list = ni.getInterfaceAddresses();
					Iterator<InterfaceAddress> it = list.iterator();
					while (it.hasNext()) {
						InterfaceAddress ia = it.next();
						if (ia == null) {
							continue;
						}
						InetAddress address = ia.getBroadcast();
						if (address == null || address.isLoopbackAddress()) {
							continue;
						}
						String udpAddress = address.getHostAddress();
						if (udpAddress == null || udpAddress.isEmpty() || udpAddress.startsWith("127.")) {
							continue;
						}
						receivers.add(new UDPBroadcastReceiver(nodeID, udpAddress, transporter));
					}
				}
			}

			// Start broadcast receivers
			for (UDPReceiver receiver : receivers) {
				receiver.connect();
			}
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

		// Close receivers
		synchronized (receivers) {
			for (UDPReceiver receiver : receivers) {
				receiver.disconnect();
			}
			receivers.clear();
		}
	}

	// --- UDP BROADCAST / MULTICAST SENDER ---

	protected volatile int nextIndex = 0;
	
	protected volatile int numberOfSubmittedPackets = 0;

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
		}
		numberOfSubmittedPackets++;
		synchronized (receivers) {

			// First time use the all address to notify other nodes
			if (numberOfSubmittedPackets == 1) {
				for (UDPReceiver receiver : receivers) {
					receiver.send();
					try {
						Thread.sleep(200);						
					} catch (InterruptedException interrupt) {
						return;
					}
				}
				return;
			}

			// Use the next network address
			nextIndex++;
			if (nextIndex >= receivers.size()) {
				nextIndex = 0;
			}
			receivers.get(nextIndex).send();
		}
	}

}