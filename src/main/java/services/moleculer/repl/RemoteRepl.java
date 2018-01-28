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
package services.moleculer.repl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * Telnet-based (remote) interactive console. To start console, type<br>
 * <br>
 * RemoteRepl r = new RemoteRepl();<br>
 * ServiceBroker broker = ServiceBroker.builder().repl(r).build();<br>
 * broker.start();<br>
 * broker.repl();
 * 
 * @see LocalRepl
 */
@Name("Remote REPL Console")
public class RemoteRepl extends LocalRepl {

	// --- TELNET HEADER ---

	protected static final byte[] TELNET_HEADER = { (byte) 255, (byte) 251, 1, (byte) 255, (byte) 254, 3, (byte) 255,
			(byte) 252, 33 };

	// --- VARIABLES ---

	/**
	 * Telnet port.
	 */
	protected int port = 23;

	/**
	 * Send echo character to the telnet client.
	 */
	protected boolean echo = true;

	/**
	 * Socket socketTimeout, in milliseconds (0 = no timeout).
	 */
	protected int socketTimeout;

	/**
	 * Maximum number of opened telnet maxSessions.
	 */
	protected int maxSessions = 8;

	/**
	 * Need username / password?
	 */
	protected boolean authenticated = true;

	/**
	 * Username.
	 */
	protected String username = "admin";

	/**
	 * The password. It is a good idea for you to change your password before
	 * the first usage by using the 'repl.setPassword("newPassword")' method.
	 */
	protected String password = "admin";

	protected final LinkedList<RemoteReader> buffers = new LinkedList<>();
	protected final LinkedList<SelectionKey> writableKeys = new LinkedList<>();

	protected ServerSocketChannel serverChannel;
	protected Selector selector;

	// --- START REPL INSTANCE ---

	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {
		super.start(broker, config);

		// Process config
		port = config.get("port", port);
		echo = config.get("echo", echo);
		socketTimeout = config.get("socketTimeout", socketTimeout);
		maxSessions = config.get("maxSessions", maxSessions);
		authenticated = config.get("authenticated", authenticated);
		username = config.get("username", username);
		password = config.get("password", password);

		// +packagesToScan from the superclass
	}

	// --- START TCP READER ---

	@Override
	protected void startReading() {
		try {
			serverChannel = ServerSocketChannel.open();
			ServerSocket serverSocket = serverChannel.socket();
			serverSocket.bind(new InetSocketAddress(port));
			serverChannel.configureBlocking(false);
			selector = Selector.open();
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			super.startReading();
			String host = "localhost";
			try {
				host = InetAddress.getLocalHost().getHostAddress();
			} catch (Exception ignored) {
			}
			logger.info("Telnet server listening on \"telnet://" + host + ":" + port + "\".");
		} catch (Exception cause) {
			logger.error("Unable to start telnet!", cause);
		}
	}

	// --- COMMAND READER LOOP ---

	@Override
	public void run() {
		SocketChannel channel = null;
		SelectionKey key, newKey;
		RemoteReader buffer;
		Iterator<SelectionKey> keys;
		int n;
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
									int openedSessions;
									synchronized (buffers) {
										openedSessions = buffers.size();
									}

									// Check the number of connections
									if (openedSessions >= maxSessions) {
										throw new Exception("too much opened maxSessions");
									}

									// Set socket socketTimeout
									Socket socket = channel.socket();
									if (socketTimeout > 0) {
										socket.setSoTimeout(socketTimeout);
									}

									// Registering connection
									channel.configureBlocking(false);
									newKey = channel.register(selector, 0);
									buffer = new RemoteReader(this, channel, newKey, authenticated, echo, username,
											password);
									newKey.attach(buffer);
									synchronized (buffers) {
										buffers.addLast(buffer);
									}

									// Send telnet header
									if (echo) {
										buffer.addBytes(TELNET_HEADER, true);
									}

								} catch (IOException e3) {
									try {
										channel.close();
									} catch (Exception ignored) {
									}
									keys.remove();
									continue;
								} catch (Exception e4) {
									try {
										String message = "Telnet connection refused: " + e4.getMessage();
										try {
											ByteBuffer msg = ByteBuffer
													.wrap(message.getBytes(StandardCharsets.US_ASCII));
											channel.write(msg);
											Thread.sleep(1000);
										} catch (Exception ignored) {
										}

										// Close channel
										channel.close();
									} catch (Exception ignored) {
									}
								}
							}
						} else {

							// Read data
							if (key.isReadable()) {
								buffer = (RemoteReader) key.attachment();
								if (buffer != null) {
									buffer.readPacket();
								}
							} else {

								// Write data
								if (key.isWritable()) {
									buffer = (RemoteReader) key.attachment();
									if (buffer != null) {
										buffer.writePacket();
									}
								}
							}
						}
					}
					keys.remove();
				}
			}

			// Switch to write mode
			synchronized (writableKeys) {
				if (!writableKeys.isEmpty()) {
					keys = writableKeys.iterator();
					while (keys.hasNext()) {
						key = (SelectionKey) keys.next();
						if (key != null && key.isValid()) {
							key.interestOps(SelectionKey.OP_WRITE);
						}
					}
					writableKeys.clear();
				}
			}
		}
	}

	protected void removeBuffer(RemoteReader buffer) {
		synchronized (buffers) {
			buffers.remove(buffer);
		}
	}

	protected void markAsWritable(SelectionKey key) {
		synchronized (writableKeys) {
			writableKeys.addLast(key);
		}
		if (null != selector) {
			selector.wakeup();
		}
	}

	// --- START TCP READER ---

	@Override
	protected void stopReading() {
		super.stopReading();

		// Close server-channel
		if (selector != null) {
			try {
				selector.wakeup();
				if (null != serverChannel) {
					serverChannel.close();
				}
				selector.close();
			} catch (Exception ignored) {
			}
			serverChannel = null;
			selector = null;
		}

		// Clear buffers
		synchronized (buffers) {
			if (!buffers.isEmpty()) {
				RemoteReader[] array = new RemoteReader[buffers.size()];
				buffers.toArray(array);
				for (int n = 0; n < array.length; n++) {
					array[n].close();
				}
			}
		}
		writableKeys.clear();
	}

	// --- GETTERS / SETTERS ---

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isEcho() {
		return echo;
	}

	public void setEcho(boolean echo) {
		this.echo = echo;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int timeout) {
		this.socketTimeout = timeout;
	}

	public int getMaxSessions() {
		return maxSessions;
	}

	public void setMaxSessions(int sessions) {
		this.maxSessions = sessions;
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}