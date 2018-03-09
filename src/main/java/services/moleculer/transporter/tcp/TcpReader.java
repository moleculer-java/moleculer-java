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
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import services.moleculer.transporter.TcpTransporter;

/**
 * Packet receiver Thread of the TCP Transporter.
 */
public class TcpReader implements Runnable {

	// --- LOGGER ---

	protected static final Logger logger = LoggerFactory.getLogger(TcpReader.class);

	// --- PROPERTIES ---

	/**
	 * Maximum size of an incoming packet
	 */
	protected int maxPacketSize;

	/**
	 * Debug mode
	 */
	protected final boolean debug;

	// --- COMPONENTS ---

	/**
	 * Parent transporter
	 */
	protected final TcpTransporter transporter;

	// --- CONSTRUCTOR ---

	public TcpReader(TcpTransporter transporter) {
		this.transporter = transporter;
		debug = transporter.isDebug();
	}

	// --- NIO VARIABLES ---

	protected ServerSocketChannel serverChannel;
	protected Selector selector;

	// --- CONNECT ---

	/**
	 * Server's executor
	 */
	protected ExecutorService executor;

	/**
	 * Current TPC port
	 */
	protected int currentPort;

	public void connect() throws Exception {

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

		// Close server socket
		if (serverChannel != null) {
			try {
				serverChannel.close();
			} catch (Exception ignored) {
			}
			serverChannel = null;
		}

		// Close selector
		if (selector != null) {
			HashSet<SelectionKey> keys = new HashSet<>();
			for (int i = 0; i < 5; i++) {
				try {
					keys.addAll(selector.keys());
					break;
				} catch (ConcurrentModificationException ignored) {
				}
			}
			for (SelectionKey key : keys) {
				close(key, null);
			}
			try {
				selector.close();
			} catch (Exception ignored) {
			}
			selector = null;
		}
	}

	// --- READER LOOP ---

	@Override
	public void run() {

		// Read buffer
		ByteBuffer readBuffer = ByteBuffer.allocate(Math.min(maxPacketSize, 1024 * 1024));
		byte[] readArray = readBuffer.array();

		// Processing variables
		Iterator<SelectionKey> keys;
		SocketChannel channel;
		SelectionKey key;

		byte[] bytes, remaining;
		int processed, pos;

		// Loop
		while (true) {

			// Waiting for sockets
			int n;
			try {
				n = selector.select(3000);
			} catch (NullPointerException nullPointer) {
				continue;
			} catch (Exception anyError) {
				break;
			}
			if (n < 1) {
				continue;
			}
			keys = selector.selectedKeys().iterator();
			while (keys.hasNext()) {
				key = keys.next();
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
						channel = serverChannel.accept();
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
					} finally {
						keys.remove();
					}

				} else if (key.isReadable()) {
					try {

						// Read bytes into the read buffer
						channel = (SocketChannel) key.channel();
						readBuffer.clear();
						n = channel.read(readBuffer);
						if (n < 0) {
							throw new IOException();
						}
						if (n == 0) {
							keys.remove();
							continue;
						}

						// Debug
						if (debug) {
							logger.info(n + " bytes received from " + channel.getRemoteAddress() + ".");
						}

						// Get the incoming bytes
						bytes = new byte[n];
						System.arraycopy(readArray, 0, bytes, 0, n);

						// Add remaining bytes
						remaining = (byte[]) key.attachment();
						if (remaining != null) {
							byte[] tmp = new byte[remaining.length + bytes.length];
							System.arraycopy(remaining, 0, tmp, 0, remaining.length);
							System.arraycopy(bytes, 0, tmp, remaining.length, bytes.length);
							bytes = tmp;
							key.attach(null);
						}

						// Split data
						pos = 0;
						while ((processed = processPacket(bytes, pos)) > 0) {
							pos += processed;
						}

						// Has remaining?
						if (pos < bytes.length) {
							if (pos == 0) {
								remaining = bytes;
							} else {
								remaining = new byte[bytes.length - pos];
								System.arraycopy(bytes, pos, remaining, 0, remaining.length);
							}
							key.attach(remaining);
						}

					} catch (Exception cause) {
						close(key, cause);
					} finally {
						keys.remove();
					}
				}
			}
		}
	}

	protected int processPacket(byte[] bytes, int pos) throws Exception {

		// Too short packet
		if (bytes.length - pos < 6) {
			return 0;
		}

		// Check packet's size
		int len = ((0xFF & bytes[pos + 1]) << 24) | ((0xFF & bytes[pos + 2]) << 16) | ((0xFF & bytes[pos + 3]) << 8)
				| (0xFF & bytes[pos + 4]);

		if (maxPacketSize > 0 && len > maxPacketSize) {
			throw new Exception("Incoming packet is larger than the \"maxPacketSize\" limit (" + len + " > "
					+ maxPacketSize + ")!");
		} else if (len < 6) {
			throw new Exception("Incoming packet is smaller than the header's size (" + len + " < 6)!");
		}

		// If all data present
		if (bytes.length >= pos + len) {

			// Verify header's CRC
			byte crc = (byte) (bytes[pos + 1] ^ bytes[pos + 2] ^ bytes[pos + 3] ^ bytes[pos + 4] ^ bytes[pos + 5]);
			if (crc != bytes[pos]) {
				throw new Exception("Invalid CRC (" + crc + " != " + bytes[pos] + ")!");
			}

			// Verify type
			byte type = bytes[pos + 5];
			if (type < 1 || type > 8) {

				// Unknown packet type!
				throw new Exception("Invalid packet type (" + type + ")!");
			}

			// Remove header
			byte[] body = new byte[len - 6];
			System.arraycopy(bytes, pos + 6, body, 0, body.length);

			// Process incoming message
			transporter.received(type, body);

			return len;
		}

		// Byte array is smaller than the packet length
		return 0;
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