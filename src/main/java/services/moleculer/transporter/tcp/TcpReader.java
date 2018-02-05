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
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import services.moleculer.transporter.TcpTransporter;

/**
 * Packet receiver Thread of the TCP Transporter.
 */
public final class TcpReader implements Runnable {

	// --- LOGGER ---

	private static final Logger logger = LoggerFactory.getLogger(TcpReader.class);

	// --- PROPERTIES ---

	/**
	 * Maximum size of an incoming packet
	 */
	private int maxPacketSize;

	/**
	 * Debug mode
	 */
	private final boolean debug;

	// --- COMPONENTS ---

	/**
	 * Parent transporter
	 */
	private final TcpTransporter transporter;

	// --- CONSTRUCTOR ---

	public TcpReader(TcpTransporter transporter) {
		this.transporter = transporter;
		debug = transporter.isDebug();
	}

	// --- NIO VARIABLES ---

	private ServerSocketChannel serverChannel;
	private Selector selector;

	// --- CONNECT ---

	/**
	 * Server's executor
	 */
	private ExecutorService executor;

	/**
	 * Current TPC port
	 */
	private int currentPort;

	public final void connect() throws Exception {

		// Create selector
		disconnect();

		// Open channel
		serverChannel = ServerSocketChannel.open();
		ServerSocket serverSocket = serverChannel.socket();
		serverSocket.bind(new InetSocketAddress(transporter.getPort()));
		serverChannel.configureBlocking(false);
		selector = Selector.open();
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);

		// Get current port
		InetSocketAddress address = (InetSocketAddress) serverChannel.getLocalAddress();
		currentPort = address.getPort();

		// Get properties
		maxPacketSize = transporter.getMaxPacketSize();

		// Start selector
		executor = Executors.newSingleThreadExecutor();
		executor.execute(this);
	}

	// --- GET CURRENT PORT ---

	public int getCurrentPort() {
		return currentPort;
	}

	// --- DISCONNECT ---

	@Override
	protected final void finalize() throws Throwable {
		disconnect();
	}

	public final void disconnect() {

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
				close(key, null);
			}
			try {
				selector.close();
			} catch (Exception ignored) {
			}
			selector = null;
		}

		// Close server socket
		if (serverChannel != null) {
			try {
				serverChannel.close();
			} catch (Exception ignored) {
			}
			serverChannel = null;
		}
	}

	// --- READER LOOP ---

	@Override
	public final void run() {

		// Read buffer
		ByteBuffer readBuffer = ByteBuffer.allocate(Math.min(maxPacketSize, 1024 * 1024));

		// Loop
		while (true) {

			// Waiting for sockets
			int n;
			try {
				n = selector.select();
			} catch (NullPointerException nullPointer) {
				continue;
			} catch (Exception anyError) {
				break;
			}
			if (n < 1) {
				continue;
			}
			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
			while (keys.hasNext()) {
				SelectionKey key = keys.next();
				if (key == null) {
					continue;
				}
				if (!key.isValid()) {
					keys.remove();
					continue;
				}
				if (key.isAcceptable()) {

					// Accept channel
					try {

						// Register socket
						SocketChannel channel = serverChannel.accept();
						channel.configureBlocking(false);
						
						channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
						channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
						channel.setOption(StandardSocketOptions.SO_LINGER, -1);
						
						channel.register(selector, SelectionKey.OP_READ);

						// Debug
						if (debug) {
							logger.info("Client channel opened from " + channel.getRemoteAddress() + ".");
						}

					} catch (Exception cause) {
						close(key, cause);
					}

				} else if (key.isReadable()) {

					// Read data
					try {
						SocketChannel channel = (SocketChannel) key.channel();
						readBuffer.rewind();
						n = channel.read(readBuffer);
						if (n < 0) {
							throw new IOException();
						}
						if (n == 0) {
							keys.remove();
							continue;
						}
						readBuffer.flip();
						byte[] packet = new byte[readBuffer.remaining()];
						readBuffer.get(packet);
						byte[] remaining = (byte[]) key.attachment();
						if (remaining != null) {
							byte[] tmp = new byte[remaining.length + packet.length];
							System.arraycopy(remaining, 0, tmp, 0, remaining.length);
							System.arraycopy(packet, 0, tmp, remaining.length, packet.length);
							packet = tmp;
						}
						if (packet.length > 5) {

							// Check packet's size
							int len = ((0xFF & packet[1]) << 24) | ((0xFF & packet[2]) << 16)
									| ((0xFF & packet[3]) << 8) | (0xFF & packet[4]);							
							if (maxPacketSize > 0 && len > maxPacketSize) {
								throw new Exception("Incoming packet is larger than the \"maxPacketSize\" limit (" + len
										+ " > " + maxPacketSize + ")!");
							} else if (len < 6) {
								throw new Exception(
										"Incoming packet is smaller than the header's size (" + len + " < 6)!");
							}

							// If all data present
							if (packet.length >= len) {

								// Verify header's CRC
								byte crc = (byte) (packet[1] ^ packet[2] ^ packet[3] ^ packet[4] ^ packet[5]);
								if (crc != packet[0]) {
									throw new Exception("Invalid CRC (" + crc + " != " + packet[0] + ")!");
								}

								// Verify type
								byte type = packet[5];
								if (type < 1 || type > 7) {

									// Unknown packet type!
									throw new Exception("Invalid packet type (" + type + ")!");
								}
								if (packet.length > len) {
									
									// Get remaining bytes
									remaining = new byte[packet.length - len];
									System.arraycopy(packet, len, remaining, 0, remaining.length);
									key.attach(remaining);

								} else {

									// Clear attachment
									key.attach(null);
								}

								// Remove header
								byte[] body = new byte[len - 6];
								System.arraycopy(packet, 6, body, 0, body.length);
								packet = body;

								// Debug
								if (debug) {
									logger.info((6 + body.length) + " bytes received from " + channel.getRemoteAddress()
											+ ".");
								}

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
						close(key, cause);
					}
				}
				keys.remove();
			}
		}
	}

	// --- CLOSE CHANNEL ---

	protected void close(SelectionKey key, Exception cause) {
		if (key == null) {
			return;
		}

		// Cancel key
		key.cancel();

		// Get channel
		SelectableChannel channel = key.channel();
		if (channel == null) {
			return;
		}

		// Debug
		if (debug) {
			try {
				if (channel instanceof SocketChannel) {
					SocketChannel socketChannel = (SocketChannel) channel;
					logger.info("Client channel closed from " + socketChannel.getRemoteAddress() + ".", cause);
				}
			} catch (Exception ignored) {
			}
		}

		// Close channel
		try {
			channel.close();
		} catch (Exception ignored) {
		}
	}

}