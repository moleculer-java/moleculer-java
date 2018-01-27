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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import services.moleculer.transporter.TcpTransporter;

public class TcpReader implements Runnable {

	// --- SHARED BUFFER ---

	protected static final ByteBuffer requestBuffer = ByteBuffer.allocateDirect(16384);

	// --- PROPERTIES ---

	/**
	 * Parent transporter
	 */
	protected final TcpTransporter transporter;

	/**
	 * Maximum size of an incoming packet
	 */
	protected int maxPacketSize;

	// --- CONSTRUCTOR ---

	public TcpReader(TcpTransporter transporter) {
		this.transporter = transporter;
	}

	// --- NIO VARIABLES ---

	protected ServerSocketChannel serverChannel;
	protected Selector selector;

	// --- CONNECT ---

	protected ExecutorService executor;

	public void connect() throws Exception {

		// Create selector and server socket
		disconnect();
		serverChannel = ServerSocketChannel.open();
		ServerSocket serverSocket = serverChannel.socket();
		serverSocket.bind(new InetSocketAddress(transporter.getPort()));
		serverChannel.configureBlocking(false);
		selector = Selector.open();
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);

		// Get properties
		maxPacketSize = transporter.getMaxPacketSize();

		// Start selector
		executor = Executors.newSingleThreadExecutor();
		executor.execute(this);
	}

	// --- DISCONNECT ---

	@Override
	protected void finalize() throws Throwable {
		disconnect();
	}

	public void disconnect() {

		// Close selector thread
		if (executor != null) {
			try {
				executor.shutdownNow();
			} catch (Exception ignored) {
			}
			executor = null;
		}

		// Close selector
		if (selector != null) {
			for (SelectionKey key : selector.keys()) {
				close(key.channel());
			}
			try {
				selector.close();
			} catch (Exception ignored) {
			}
			selector = null;
		}

		// Close server socket
		if (serverChannel != null) {
			close(serverChannel);
			serverChannel = null;
		}
	}

	// --- READER LOOP ---

	@Override
	public void run() {

		// Variables
		byte[] bytes, copy, packet, remaining;
		SocketChannel channel = null;
		Iterator<SelectionKey> keys;
		SelectionKey key;
		byte crc, type;
		int n, len;

		// Loop
		while (!Thread.currentThread().isInterrupted()) {

			// Waiting for sockets
			try {
				if (null == selector) {
					continue;
				}
				n = selector.select();
			} catch (NullPointerException nullPointer) {
				continue;
			} catch (Exception anyError) {
				break;
			}
			if (n != 0) {
				keys = selector.selectedKeys().iterator();
				while (keys.hasNext()) {
					key = keys.next();
					if (key == null) {
						continue;
					}
					if (key.isValid()) {
						if (key.isAcceptable()) {
							try {
								if (null != serverChannel) {
									channel = serverChannel.accept();
								}
							} catch (IOException cause) {
								keys.remove();
								continue;
							}
							if (channel != null) {
								try {

									// Register socket
									channel.configureBlocking(false);
									key = channel.register(selector, 0);
									key.interestOps(SelectionKey.OP_READ);

								} catch (Exception cause) {
									close(channel);
								}
							}
						} else if (key.isReadable()) {

							// Read data
							try {
								channel = (SocketChannel) key.channel();
								n = channel.read(requestBuffer);
								if (n < 0) {
									throw new IOException();
								}
								requestBuffer.flip();
								packet = new byte[requestBuffer.remaining()];
								requestBuffer.get(packet);
								requestBuffer.rewind();
								bytes = (byte[]) key.attachment();
								if (bytes != null) {
									copy = new byte[packet.length + bytes.length];
									System.arraycopy(bytes, 0, copy, 0, bytes.length);
									System.arraycopy(packet, 0, copy, bytes.length, packet.length);
									packet = copy;
								}
								if (packet.length > 5) {

									// Check size
									if (maxPacketSize > 0 && packet.length > maxPacketSize) {
										throw new Exception(
												"Incomin packet is larger than the \"maxPacketSize\" limit ("
														+ packet.length + " > " + maxPacketSize + ")!");
									}

									// Read header and check CRC
									crc = (byte) (packet[1] ^ packet[2] ^ packet[3] ^ packet[4] ^ packet[5]);
									if (crc != packet[0]) {
										throw new Exception("Invalid CRC (" + crc + " != " + packet[0] + ")!");
									}
									len = ((0xFF & packet[1]) << 24) | ((0xFF & packet[2]) << 16)
											| ((0xFF & packet[3]) << 8) | (0xFF & packet[4]);

									// Check length
									if (packet.length >= len) {

										// All of bytes received
										type = packet[5];
										if (packet.length > len) {

											// Get remaining bytes
											remaining = new byte[packet.length - len];
											System.arraycopy(packet, len, remaining, 0, remaining.length);
											key.attach(remaining);
											copy = new byte[len];
											System.arraycopy(packet, 0, copy, 0, copy.length);
											packet = copy;
											len = copy.length;

										} else {

											// Clear attachment
											key.attach(null);
										}

										// Remove header
										copy = new byte[len - 6];
										System.arraycopy(packet, 6, copy, 0, copy.length);
										packet = copy;

										// Process incoming message
										transporter.received(type, packet);

									} else {

										// Waiting for data
										key.attach(packet);
									}

								} else {

									// Waiting for data
									key.attach(packet);
								}
							} catch (Exception cause) {
								close(key.channel());
							}

						}
					}
					keys.remove();
				}
			}
		}
	}

	protected void close(SelectableChannel channel) {
		if (channel != null) {
			try {
				channel.close();
			} catch (Exception ignored) {
			}
		}
	}

}