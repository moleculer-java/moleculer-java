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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import services.moleculer.transporter.TcpTransporter;

public class TcpWriter implements Runnable {

	// --- PROPERTIES ---

	/**
	 * Parent transporter
	 */
	protected final TcpTransporter transporter;

	// --- CONSTRUCTOR ---

	public TcpWriter(TcpTransporter transporter) {
		this.transporter = transporter;
	}

	// --- NIO VARIABLES ---

	protected Selector selector;

	// --- CONNECT ---

	protected ExecutorService executor;

	public void connect() throws Exception {

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

		// Close sockets
		for (SocketChannel channel : channels.values()) {
			try {
				channel.close();
			} catch (Exception ignored) {
			}
		}
		channels.clear();
		for (SocketChannel channel : newChannels.keySet()) {
			try {
				channel.close();
			} catch (Exception ignored) {
			}
		}
		newChannels.clear();
		
		// Close selector
		if (selector != null) {
			try {
				selector.wakeup();
			} catch (Exception ignored) {
			}
			try {
				selector.close();
			} catch (Exception ignored) {
			}
			selector = null;
		}
	}

	// --- WRITE TO SOCKET ---

	protected final HashMap<TcpEndpoint, SocketChannel> channels = new HashMap<>();

	protected final HashMap<SocketChannel, LinkedList<ByteBuffer>> newChannels = new HashMap<>();

	protected final HashSet<SelectionKey> writableKeys = new HashSet<>();
	
	@SuppressWarnings("unchecked")
	public void send(TcpEndpoint endpoint, byte[] packet) throws Exception {

		// TODO Synchronze buffers, newChannels
		
		// Get channel and append packet to selection key
		SocketChannel channel = channels.get(endpoint);
		if (channel == null) {
			
			InetSocketAddress address = new InetSocketAddress(endpoint.host, endpoint.port);
			SocketChannel client = SocketChannel.open(address);
			client.configureBlocking(false);
			channels.put(endpoint, client);
			
			LinkedList<ByteBuffer> buffers = new LinkedList<>();
			buffers.addLast(ByteBuffer.wrap(packet));
			newChannels.put(client, buffers);			
			selector.wakeup();
			
		} else {
			
			SelectionKey key = channel.keyFor(selector);
			if (key == null) {
				return;
			}
			LinkedList<ByteBuffer> buffers = (LinkedList<ByteBuffer>) key.attachment();
			if (buffers == null) {
				buffers = new LinkedList<>();
				key.attach(buffers);
			}
			buffers.addLast(ByteBuffer.wrap(packet));
			
			writableKeys.add(key);
			selector.wakeup();
			
		}
	}

	// --- WRITER LOOP ---

	@SuppressWarnings("unchecked")
	@Override
	public void run() {

		// Variables
		Iterator<SelectionKey> keys;
		SelectionKey key;
		int n;

		while (!Thread.currentThread().isInterrupted()) {

			// Waiting for sockets
			try {
				if (null == selector) {
					continue;
				}
				n = selector.select();
				
				// TODO Synchronze newChannels
				
				if (!newChannels.isEmpty()) {
					for (Map.Entry<SocketChannel, LinkedList<ByteBuffer>> entry: newChannels.entrySet()) {
						SocketChannel channel = entry.getKey();						
						channel.register(selector, SelectionKey.OP_WRITE, entry.getValue());
					}
					newChannels.clear();
				}
				if (!writableKeys.isEmpty()) {
					for (SelectionKey k: writableKeys) {
						k.interestOps(SelectionKey.OP_WRITE);
					}
					writableKeys.clear();
				}
				
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
					if (key.isValid() && key.isWritable()) {

						// Write data
						try {
							
							// TODO Synchronze buffers
							
							LinkedList<ByteBuffer> buffers = (LinkedList<ByteBuffer>) key.attachment();
							if (buffers == null || buffers.isEmpty()) {
								key.interestOps(0);
								key.attach(null);
								keys.remove();
								continue;
							}
							ByteBuffer buffer = buffers.getFirst();
							if (buffer.hasRemaining()) {
								SocketChannel client = (SocketChannel) key.channel();
								client.write(buffer);
								if (!buffer.hasRemaining()) {
									buffers.removeFirst();	
								}
							} else {
								buffers.removeFirst();
							}
							
						} catch (Exception cause) {
							cause.printStackTrace();
							try {
								key.channel().close();
							} catch (Exception ignored) {
							}
						}
					}
					keys.remove();
				}
			}
		}
	}

}