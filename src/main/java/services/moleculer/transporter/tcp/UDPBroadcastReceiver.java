/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
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
package services.moleculer.transporter.tcp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import services.moleculer.transporter.TcpTransporter;

public class UDPBroadcastReceiver extends UDPReceiver {

	// --- PROPERTIES ---

	/**
	 * Broadcast receiver
	 */
	protected DatagramSocket broadcastReceiver;

	// --- CONSTRUCTOR ---

	protected UDPBroadcastReceiver(String nodeID, String udpAddress, TcpTransporter transporter) {
		super(nodeID, udpAddress, transporter);
	}

	// --- CONNECT ---

	@Override
	protected void connect() throws Exception {

		// Start broadcast receiver
		broadcastReceiver = new DatagramSocket();
		broadcastReceiver.setReuseAddress(udpReuseAddr);
		broadcastReceiver.setBroadcast(true);
		broadcastReceiver.connect(InetAddress.getByName(udpAddress), udpPort);

		// Start thread
		super.connect();

		logger.info("Broadcast discovery service started on udp://" + udpAddress + ':' + udpPort + '.');
	}

	// --- DISCONNECT ---

	@Override
	protected void disconnect() {

		// Stop thread
		super.disconnect();

		// Stop broadcast receiver
		if (broadcastReceiver != null) {
			try {
				broadcastReceiver.close();
			} catch (Exception ignored) {
			}
			broadcastReceiver = null;
			logger.info("Broadcast discovery service stopped on udp://" + udpAddress + ':' + udpPort + '.');
		}
	}

	// --- MESSAGE SENDER ---

	@Override
	protected void send() {
		String msg = namespace + '|' + nodeID + '|' + port;

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
				logger.info("Broadcast message submitted (namespace: " + namespace + ", node ID: " + nodeID + ", port: "
						+ port + ").");
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

	// --- UDP BROADCAST RECEIVER ---

	@Override
	protected void receive() {
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
				try {
					Thread.sleep(1000);
				} catch (Exception interrupt) {
					return;
				}
			}
		}
	}

}