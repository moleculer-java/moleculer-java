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

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import services.moleculer.service.NodeDescriptor;
import services.moleculer.transporter.TcpTransporter;

/**
 * Packet sender Thread of the TCP Transporter.
 */
public class TcpWriter implements Runnable {

	// --- LOGGER ---

	protected static final Logger logger = LoggerFactory.getLogger(TcpWriter.class);

	// --- PROPERTIES ---

	/**
	 * Max number of opened connections
	 */
	protected final int maxConnections;

	/**
	 * Debug monde
	 */
	protected final boolean debug;

	// --- PARENT TRANSPORTER ---

	protected final TcpTransporter transporter;

	// --- NIO VARIABLES ---

	protected final ConcurrentLinkedQueue<SendBuffer> opened = new ConcurrentLinkedQueue<>();

	protected final LinkedHashMap<String, SendBuffer> buffers = new LinkedHashMap<>();

	protected Selector selector;

	// --- CONSTRUCTOR ---

	public TcpWriter(TcpTransporter transporter) {
		this.transporter = transporter;
		this.debug = transporter.isDebug();
		this.maxConnections = transporter.getMaxKeepAliveConnections();
	}

	// --- CONNECT ---

	/**
	 * Writer thread
	 */
	private ExecutorService executor;

	public final void connect() throws Exception {

		// Create selector
		disconnect();
		selector = Selector.open();

		// Start selector's loop
		executor = Executors.newSingleThreadExecutor();
		executor.execute(this);
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

		// Close sockets
		if (selector != null) {
			for (SelectionKey key : selector.keys()) {
				try {
					if (key != null) {
						SendBuffer buffer = (SendBuffer) key.attachment();
						if (buffer != null) {
							buffer.close();
						}
					}
				} catch (Exception ignored) {
				}
			}

			// Close selector
			try {
				selector.close();
			} catch (Exception ignored) {
			}
			selector = null;
		}

		// Clear buffers
		synchronized (buffers) {
			buffers.clear();
		}
	}

	// --- WRITE TO SOCKET ---

	public final void send(NodeDescriptor node, byte[] packet) {
		try {

			// Get or create buffer
			SendBuffer buffer = null;
			synchronized (buffers) {
				buffer = buffers.get(node.nodeID);
				if (buffer == null) {
					buffer = new SendBuffer(node.nodeID, node.host, node.port);
					buffers.put(node.nodeID, buffer);
					opened.add(buffer);
				}
			}
			buffer.append(packet);
			if (buffer.write()) {
				selector.wakeup();
			}
		} catch (Exception cause) {
			transporter.unableToSend(node.nodeID, packet, cause);
		}
	}

	// --- WRITER LOOP ---

	@Override
	public final void run() {

		// Loop
		while (true) {

			// Waiting for sockets
			int n;
			try {
				n = selector.select();
			} catch (NullPointerException nullPointer) {
				continue;
			} catch (Exception cause) {
				break;
			}

			// Open new connections
			SendBuffer buffer = opened.poll();
			SelectionKey key = null;			
			while (buffer != null) {
				try {
					InetSocketAddress address = new InetSocketAddress(buffer.host, buffer.port);
					SocketChannel channel = SocketChannel.open(address);
					channel.configureBlocking(false);
					channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
					channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
					channel.setOption(StandardSocketOptions.SO_LINGER, -1);
					channel.register(selector, SelectionKey.OP_CONNECT);
					key = channel.register(selector, SelectionKey.OP_WRITE);
					key.attach(buffer);
					buffer.connected(key, channel);
					if (debug) {
						logger.info("Client channel opened to \"" + buffer.nodeID + "\".");
					}
				} catch (Exception cause) {
					if (buffer != null) {
						synchronized (buffers) {
							buffers.remove(buffer.nodeID);
						}
						transporter.unableToSend(buffer.nodeID, buffer.getCurrentPacket(), cause);
					}
					continue;
				}
				buffer = opened.poll();
			}

			if (n < 1) {
				continue;
			}
			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
			while (keys.hasNext()) {
				key = keys.next();
				if (key == null) {
					continue;
				}
				if (!key.isValid()) {
					keys.remove();
					continue;
				}
				if (key.isWritable()) {

					// Write data
					buffer = null;
					try {
						buffer = (SendBuffer) key.attachment();
						if (buffer != null) {
							buffer.write();
						}
					} catch (Exception cause) {
						if (buffer != null) {
							synchronized (buffers) {
								buffers.remove(buffer.nodeID);
							}
							transporter.unableToSend(buffer.nodeID, buffer.getCurrentPacket(), cause);
						}
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
					logger.info("Client channel closed to " + socketChannel.getRemoteAddress() + ".", cause);
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