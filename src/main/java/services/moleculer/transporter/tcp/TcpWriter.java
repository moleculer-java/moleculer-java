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
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import services.moleculer.transporter.TcpTransporter;

public class TcpWriter implements Runnable {

	// --- PROPERTIES ---

	protected final TcpTransporter transporter;
	protected final HashMap<TcpEndpoint, SocketChannel> channels = new HashMap<>();
	protected final HashMap<SocketChannel, LinkedList<ByteBuffer>> newChannels = new HashMap<>();
	protected final HashSet<SelectionKey> writableKeys = new HashSet<>();
	protected final HashSet<SocketChannel> channelsToClose = new HashSet<>();
	protected final AtomicBoolean hasNewChannels = new AtomicBoolean();
	protected final AtomicBoolean hasWritableKeys = new AtomicBoolean();

	protected Selector selector;

	// --- CONSTRUCTOR ---

	public TcpWriter(TcpTransporter transporter) {
		this.transporter = transporter;
	}

	// --- CONNECT ---

	/**
	 * Cancelable timer
	 */
	protected volatile ScheduledFuture<?> timer;

	protected ExecutorService executor;

	protected int maxConnections;

	public void connect() throws Exception {

		// Set max number of connections
		maxConnections = transporter.getMaxKeepAliveConnections();

		// Create selector
		disconnect();
		selector = Selector.open();

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
			try {
				selector.close();
			} catch (Exception ignored) {
			}
			selector = null;
		}

		// Close sockets
		synchronized (channels) {
			closeAll(channels.values());
		}
		synchronized (newChannels) {
			closeAll(newChannels.keySet());
		}
		synchronized (channelsToClose) {
			closeAll(channelsToClose);
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

	// --- WRITE TO SOCKET ---

	@SuppressWarnings("unchecked")
	public void send(TcpEndpoint endpoint, byte[] packet) {
		try {

			// Get channel and append packet to selection key
			SelectionKey key = null;
			SocketChannel channel;
			if (maxConnections == 0) {

				// Keep-alive disabled
				channel = null;
			} else {

				// Keep-alive enabled
				synchronized (channels) {
					channel = channels.get(endpoint);
					if (channel != null) {
						key = channel.keyFor(selector);
						if (key == null) {
							channels.remove(endpoint);
						}
					}
				}
			}

			// Create new socket
			if (channel == null || key == null) {
				LinkedList<ByteBuffer> buffers = new LinkedList<>();
				buffers.addLast(ByteBuffer.wrap(packet));

				InetSocketAddress address = new InetSocketAddress(endpoint.host, endpoint.port);
				SocketChannel client = SocketChannel.open(address);
				client.configureBlocking(false);

				// Register channel
				if (maxConnections != 0) {
					SocketChannel previous;
					synchronized (channels) {
						previous = channels.put(endpoint, client);

					}
					if (previous != null) {
						synchronized (channelsToClose) {
							channelsToClose.add(previous);
						}
					}

					// Close an empty, unused channel
					if (maxConnections != -1 && channels.size() > maxConnections) {
						Iterator<SocketChannel> opened = channels.values().iterator();
						while (opened.hasNext()) {
							SocketChannel testChannel = opened.next();
							synchronized (newChannels) {
								if (newChannels.containsKey(testChannel)) {
									continue;
								}
							}
							if (testChannel == client) {
								continue;
							}
							SelectionKey testKey = testChannel.keyFor(selector);
							if (testKey != null) {
								LinkedList<ByteBuffer> list = (LinkedList<ByteBuffer>) key.attachment();
								boolean empty;
								synchronized (list) {
									empty = list.isEmpty();
								}
								if (empty) {
									opened.remove();
									close(testChannel);
									break;
								}
							}
						}
					}
				}
				synchronized (newChannels) {
					newChannels.put(client, buffers);
				}
				hasNewChannels.set(true);
				selector.wakeup();

				return;
			}

			// Send data
			LinkedList<ByteBuffer> buffers = (LinkedList<ByteBuffer>) key.attachment();
			ByteBuffer buffer = ByteBuffer.wrap(packet);
			synchronized (buffers) {
				buffers.addLast(buffer);
			}
			synchronized (writableKeys) {
				writableKeys.add(key);
			}
			hasWritableKeys.set(true);
			selector.wakeup();

		} catch (Exception cause) {
			transporter.handlePostError(packet, cause);
		}
	}

	// --- WRITER LOOP ---

	@SuppressWarnings("unchecked")
	@Override
	public void run() {

		// Variables
		LinkedList<ByteBuffer> buffers;
		Iterator<SelectionKey> keys;
		ByteBuffer buffer = null;
		SocketChannel channel;
		boolean remove, empty;
		SelectionKey key;
		int n;

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
						for (Map.Entry<SocketChannel, LinkedList<ByteBuffer>> entry : newChannels.entrySet()) {
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
			} catch (Exception anyError) {
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
						buffers = (LinkedList<ByteBuffer>) key.attachment();
						synchronized (buffers) {
							empty = buffers.isEmpty();
						}
						if (empty) {
							switchToIdle(key);
							keys.remove();
							continue;
						}
						buffer = buffers.getFirst();
						if (buffer.hasRemaining()) {
							channel = (SocketChannel) key.channel();
							channel.write(buffer);
							remove = !buffer.hasRemaining();
						} else {
							remove = true;
						}
						if (remove) {
							synchronized (buffers) {
								buffers.removeFirst();
								empty = buffers.isEmpty();
							}
							if (empty) {
								switchToIdle(key);
							}
						}
					} catch (Exception cause) {
						if (buffer != null) {
							transporter.handlePostError(buffer.array(), cause);
						}
						close(key);
					} finally {
						buffer = null;
					}
				}
				keys.remove();
			}
		}
	}

	protected void switchToIdle(SelectionKey key) {
		key.interestOps(0);
		SelectableChannel channel = key.channel();
		if (channel != null) {
			if (maxConnections == 0) {

				// Keep-alive disabled
				close(channel);
			} else {

				// Keep-alive enabled
				boolean close;
				synchronized (channelsToClose) {
					close = channelsToClose.remove(channel);
				}
				if (close) {
					close(channel);
				}
			}
		}
	}

	protected void close(SelectionKey key) {
		if (key != null) {
			close(key.channel());
		}
	}

	protected void close(SelectableChannel channel) {
		if (channel != null) {
			try {
				channel.close();
			} catch (Exception ignored) {
			}
			synchronized (channels) {
				Iterator<SocketChannel> i = channels.values().iterator();
				while (i.hasNext()) {
					if (channel == i.next()) {
						i.remove();
						break;
					}
				}
			}
		}
	}

}