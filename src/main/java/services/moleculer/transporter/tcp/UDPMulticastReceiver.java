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

import services.moleculer.transporter.TcpTransporter;

public class UDPMulticastReceiver extends UDPReceiver {

	// --- PROPERTIES ---
	
	/**
	 * Multicast receiver
	 */
	protected MulticastSocket multicastReceiver;
	
	// --- CONSTRUCTOR ---
	
	protected UDPMulticastReceiver(String nodeID, TcpTransporter transporter) {
		super(nodeID, transporter);
	}

	// --- CONNECT ---
	
	@Override
	protected void connect() throws Exception {
		
		// Start multicast receiver
		multicastReceiver = new MulticastSocket(udpPort);
		multicastReceiver.joinGroup(InetAddress.getByName(udpAddress));
		
		// Start thread
		super.connect();
		
		logger.info("Multicast discovery service started on udp://" + udpAddress + ':' + udpPort + '.');
	}

	// --- DISCONNECT ---

	@Override
	protected void disconnect() {
		
		// Stop thread
		super.disconnect();
		
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
	}
	
	// --- MESSAGE SENDER ---
	
	@Override
	protected void send() {
		String msg = namespace + '|' + nodeID + '|' + port;
		
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
	}
	
	// --- UDP MULTICAST RECEIVER ---

	@Override
	protected void receive() {
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
	
}
