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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.transporter.TcpTransporter;

/**
 * Packet sender Thread of the TCP Transporter.
 */
public final class TcpWriter implements Runnable {

	// --- LOGGER ---

	private static final Logger logger = LoggerFactory.getLogger(TcpWriter.class);

	// --- COMPONENTS ---

	private final ScheduledExecutorService scheduler;

	// --- PROPERTIES ---

	/**
	 * Max number of opened connections
	 */
	private final int maxConnections;

	/**
	 * Keep-alive timeout in MILLISECONDS
	 */
	private final long keepAliveTimeout;

	/**
	 * Debug monde
	 */
	private final boolean debug;

	/**
	 * Use hostnames instead of IP addresses As the DHCP environment is dynamic,
	 * any later attempt to use IPs instead hostnames would most likely yield
	 * false results. Therefore, use hostnames if you are using DHCP.
	 */
	private final boolean useHostname;
	
	// --- NIO VARIABLES ---

	private final TcpTransporter transporter;

	private final LinkedHashMap<String, SendBuffer> buffers = new LinkedHashMap<>();

	private final HashSet<SendBuffer> writable = new HashSet<>();
	private final AtomicBoolean hasWritable = new AtomicBoolean();

	private Selector selector;

	// --- CONSTRUCTOR ---

	public TcpWriter(TcpTransporter transporter, ScheduledExecutorService scheduler) {
		this.transporter = transporter;
		this.scheduler = scheduler;
		this.debug = transporter.isDebug();
		this.useHostname = transporter.isUseHostname();
		this.maxConnections = transporter.getMaxKeepAliveConnections();
		this.keepAliveTimeout = transporter.getKeepAliveTimeout() * 1000L;
	}

	// --- CONNECT ---

	/**
	 * Cancelable timer of timeout handler
	 */
	private volatile ScheduledFuture<?> timer;

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

		// Start timeout handler
		if (maxConnections > 0 && keepAliveTimeout > 0) {
			timer = scheduler.scheduleAtFixedRate(this::manageTimeouts, 1, 1, TimeUnit.SECONDS);
		}
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
			try {
				selector.close();
			} catch (Exception ignored) {
			}
			selector = null;
		}

		// Close sockets
		synchronized (buffers) {
			SelectionKey key;
			for (SendBuffer attachment : buffers.values()) {
				key = attachment.key();
				if (key != null) {
					close((SocketChannel) key.channel());
				}
			}
			buffers.clear();
		}
		synchronized (writable) {
			writable.clear();
		}
	}

	// --- MANAGE TIMEOUTS ---

	private final void manageTimeouts() {

		// Collect closeable channels
		LinkedList<SendBuffer> closeables = new LinkedList<>();
		long timeLimit = keepAliveTimeout > 0 ? System.currentTimeMillis() - keepAliveTimeout : 0;
		Iterator<SendBuffer> allBuffers;
		SendBuffer buffer;
		int connectionsToClose;
		synchronized (buffers) {
			connectionsToClose = maxConnections > 0 ? buffers.size() - maxConnections : 0;
			allBuffers = buffers.values().iterator();
			while (allBuffers.hasNext()) {
				buffer = allBuffers.next();
				if (timeLimit > 0) {
					if (buffer.invalidate(timeLimit)) {
						closeables.add(buffer);
						allBuffers.remove();
						continue;
					}
				}
				if (connectionsToClose > 0) {
					if (buffer.invalidate(0)) {
						closeables.add(buffer);
						connectionsToClose--;
						allBuffers.remove();
						continue;
					}
				}
			}
		}

		// Close channels
		SelectionKey key;
		for (SendBuffer closeable : closeables) {
			key = closeable.key();
			if (key != null) {
				close((SocketChannel) key.channel());
			}
		}
	}

	// --- WRITE TO SOCKET ---

	public final void send(String nodeID, Tree info, byte[] packet) {

		// Get or create buffer
		SendBuffer buffer = null;
		synchronized (buffers) {
			buffer = buffers.get(nodeID);
			if (buffer == null || !buffer.use()) {

				// Create new attachment
				String host = null;
				if (useHostname) {
					host = info.get("hostname", (String) null);
				}
				if (host == null) {
					Tree ipList = info.get("ipList");
					if (ipList.size() > 0) {
						host = ipList.get(0).asString();
					} else {
						throw new IllegalArgumentException("Missing or empty \"ipList\" property!");
					}
				}
				int port = info.get("port", 7328);
				buffer = new SendBuffer(nodeID, host, port, packet);
				buffers.put(nodeID, buffer);
			}
		}
		buffer.append(packet);
		synchronized (writable) {
			writable.add(buffer);
		}
		hasWritable.set(true);
		selector.wakeup();
	}

	// --- WRITER LOOP ---

	@Override
	public final void run() {

		// Variables
		SendBuffer buffer = null;
		SocketChannel channel = null;
		Iterator<SelectionKey> keys;
		SelectionKey key;
		int n;

		// Loop
		while (!Thread.currentThread().isInterrupted()) {

			// Waiting for sockets
			try {
				if (null == selector) {
					continue;
				}
				n = selector.select();

				// Set key status
				if (hasWritable.compareAndSet(true, false)) {
					synchronized (writable) {
						for (SendBuffer writableBuffer : writable) {
							key = writableBuffer.key();
							if (key == null) {
								
								// Create new socket
								try {
									InetSocketAddress address = new InetSocketAddress(writableBuffer.host, writableBuffer.port);
									channel = SocketChannel.open(address);
									channel.configureBlocking(false);

									// Register socket in selector
									key = channel.register(selector, SelectionKey.OP_WRITE, writableBuffer);
									writableBuffer.key(key);

								} catch (Exception cause) {
									writableBuffer.invalidate();
									synchronized (buffers) {
										buffers.remove(writableBuffer.nodeID);
									}
									transporter.unableToSend(writableBuffer, cause);
								}
							} else {

								// Switch to write mode
								key.interestOps(SelectionKey.OP_WRITE);
							}
						}
						writable.clear();
					}
				}

				// Has keys?
				if (n < 1) {
					continue;
				}

			} catch (NullPointerException nullPointer) {
				continue;
			} catch (Exception fatalError) {
				break;
			}

			// Loop on keys
			keys = selector.selectedKeys().iterator();
			while (keys.hasNext()) {
				key = keys.next();
				if (key == null) {
					continue;
				}
				if (key.isValid() && key.isWritable()) {

					// Write data
					try {
						buffer = (SendBuffer) key.attachment();
						channel = (SocketChannel) key.channel();
						if (!buffer.write(channel)) {

							// Debug
							if (debug) {
								logger.info("Message submission finished to " + channel.getRemoteAddress() + ".");
							}

							// All data sent
							key.interestOps(0);
						}
					} catch (Exception cause) {
						if (buffer != null) {
							synchronized (buffers) {
								buffers.remove(buffer.nodeID);
							}
							transporter.unableToSend(buffer, cause);
						}
						close(channel);
					}
				}
				keys.remove();
			}
		}
	}

	// --- CLOSE CHANNEL ---

	private final void close(SocketChannel channel) {
		if (channel != null) {

			// Debug
			if (debug) {
				try {
					logger.info("Client channel closed to " + channel.getRemoteAddress() + ".");
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

}