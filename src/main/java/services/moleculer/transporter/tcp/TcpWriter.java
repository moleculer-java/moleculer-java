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
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.datatree.Tree;
import services.moleculer.transporter.TcpTransporter;

public class TcpWriter implements Runnable {

	// --- PROPERTIES ---

	protected final TcpTransporter transporter;

	protected final LinkedHashMap<String, SelectionKey> allKeys = new LinkedHashMap<>();

	protected final HashMap<SocketChannel, KeyAttachment> newChannels = new HashMap<>();
	protected final HashSet<SelectionKey> writableKeys = new HashSet<>();

	protected final AtomicBoolean hasNewChannels = new AtomicBoolean();
	protected final AtomicBoolean hasWritableKeys = new AtomicBoolean();

	protected Selector selector;

	// --- COMPONENTS ---

	protected ScheduledExecutorService scheduler;

	// --- CONSTRUCTOR ---

	public TcpWriter(TcpTransporter transporter, ScheduledExecutorService scheduler) {
		this.transporter = transporter;
		this.scheduler = scheduler;
	}

	// --- CONNECT ---

	/**
	 * Cancelable timer of timeout handler
	 */
	protected volatile ScheduledFuture<?> timer;

	/**
	 * Writer thread
	 */
	protected ExecutorService executor;

	/**
	 * Max number of opened connections
	 */
	protected int maxConnections;

	/**
	 * Keep-alive timeout in MILLISECONDS
	 */
	protected long keepAliveTimeout;

	public void connect() throws Exception {

		// Set max number of connections
		maxConnections = transporter.getMaxKeepAliveConnections();
		keepAliveTimeout = transporter.getKeepAliveTimeout() * 1000L;

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
	protected void finalize() throws Throwable {
		disconnect();
	}

	public void disconnect() {

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
		synchronized (newChannels) {
			closeAll(newChannels.keySet());
		}
		synchronized (writableKeys) {
			writableKeys.clear();
		}
	}

	protected void closeAll(Collection<SocketChannel> channels) {
		for (SocketChannel channel : channels) {
			try {
				channel.close();
			} catch (Exception ignored) {
			}
		}
		channels.clear();
	}

	// --- MANAGE TIMEOUTS ---

	protected void manageTimeouts() {

		// Collect closeable channels
		LinkedList<SelectableChannel> collected = new LinkedList<>();
		long timeLimit = keepAliveTimeout > 0 ? System.currentTimeMillis() - keepAliveTimeout : 0;
		Iterator<SelectionKey> keys;
		KeyAttachment attachment;
		int channelsToClose;
		SelectionKey key;
		synchronized (allKeys) {
			channelsToClose = maxConnections > 0 ? allKeys.size() - maxConnections : 0;
			keys = allKeys.values().iterator();
			while (keys.hasNext()) {
				key = keys.next();
				if (key == null) {
					continue;
				}
				attachment = (KeyAttachment) key.attachment();
				if (attachment == null) {
					collected.add(key.channel());
					continue;
				}
				if (timeLimit > 0) {
					if (attachment.invalidate(timeLimit)) {
						collected.add(key.channel());
						continue;
					}
				}
				if (channelsToClose > 0) {
					if (attachment.invalidate(timeLimit)) {
						collected.add(key.channel());
						channelsToClose--;
						continue;
					}
				}
			}
		}

		// Close channels
		for (SelectableChannel channel : collected) {
			if (channel != null) {
				try {
					channel.close();
				} catch (Exception ingored) {
				}
			}
		}
	}

	// --- WRITE TO SOCKET ---

	public void send(String nodeID, Tree info, byte[] packet) {
		KeyAttachment attachment = null;
		try {

			// Get channel and append packet to selection key
			SelectionKey key = null;
			synchronized (allKeys) {
				key = allKeys.get(nodeID);
			}

			// Open new connection
			if (key == null) {
				openNewConnection(nodeID, info, packet);
				return;
			}

			// Try to use an existing connection
			attachment = (KeyAttachment) key.attachment();
			if (attachment == null || !attachment.use()) {
				openNewConnection(nodeID, info, packet);
				return;
			}
			attachment.append(packet);
			synchronized (writableKeys) {
				writableKeys.add(key);
			}
			hasWritableKeys.set(true);
			selector.wakeup();

		} catch (Exception cause) {
			transporter.unableToSend(attachment, cause);
		}
	}

	protected void openNewConnection(String nodeID, Tree info, byte[] packet) throws Exception {

		// Create new attachment
		String host = info.get("hostName", (String) null);
		if (host == null) {
			Tree ipList = info.get("ipList");
			if (ipList.size() > 0) {
				host = ipList.get(0).asString();
			} else {
				throw new Exception("Missing or empty \"ipList\" property!");
			}
		}
		int port = info.get("port", 7328);
		KeyAttachment attachment = new KeyAttachment(nodeID, host, port, packet);

		// Create new socket
		InetSocketAddress address = new InetSocketAddress(host, port);
		SocketChannel channel = SocketChannel.open(address);
		channel.configureBlocking(false);

		// Send channel to registering
		synchronized (newChannels) {
			newChannels.put(channel, attachment);
		}
		hasNewChannels.set(true);
		selector.wakeup();
	}

	// --- WRITER LOOP ---

	@Override
	public void run() {

		// Variables
		KeyAttachment attachment = null;
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

				// Register new channels
				if (hasNewChannels.compareAndSet(true, false)) {
					synchronized (newChannels) {
						for (Map.Entry<SocketChannel, KeyAttachment> entry : newChannels.entrySet()) {
							entry.getKey().register(selector, SelectionKey.OP_WRITE, entry.getValue());
						}
						newChannels.clear();
					}
				}

				// Set key status
				if (hasWritableKeys.compareAndSet(true, false)) {
					synchronized (writableKeys) {
						for (SelectionKey writableKey : writableKeys) {
							writableKey.interestOps(SelectionKey.OP_WRITE);
						}
						writableKeys.clear();
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
						attachment = (KeyAttachment) key.attachment();
						channel = (SocketChannel) key.channel();
						if (!attachment.write(channel)) {

							// All data sent
							key.interestOps(0);
						}
					} catch (Exception cause) {
						transporter.unableToSend(attachment, cause);
						close(attachment, key, channel);
					}
				}
				keys.remove();
			}
		}
	}

	protected void close(KeyAttachment attachment, SelectionKey key, SocketChannel channel) {
		if (attachment != null) {
			synchronized (allKeys) {
				allKeys.remove(attachment.nodeID);
			}
		}
		if (channel == null) {
			if (key == null) {
				return;
			}
			channel = (SocketChannel) key.channel();
		}
		if (channel != null) {
			try {
				channel.close();
			} catch (Exception ignored) {
			}
		}
	}

}