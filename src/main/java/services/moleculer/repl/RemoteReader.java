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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import services.moleculer.ServiceBroker;

/**
 * TCP reader / writer for the telnet based REPL console.
 */
public class RemoteReader {

	// --- SHARED BUFFER ---

	private static final ByteBuffer requestBuffer = ByteBuffer.allocate(1);

	// --- CONSTANTS ---

	protected static final byte[] LINE_BREAK = { 10, 13 };
	protected static final byte[] BACKSPACE = { (byte) '\b', (byte) ' ', (byte) '\b' };
	protected static final byte[] HIGHLIGHT = "\u001B[1m".getBytes();
	protected static final byte[] NORMAL = "\u001B[0m".getBytes();

	// --- INITIAL PARAMETERS ---

	protected final RemoteRepl remoteRepl;
	protected final SocketChannel channel;
	protected final SelectionKey key;

	protected final boolean echo;
	protected final String username;
	protected final String password;

	// --- VARIABLES ---

	protected boolean maskPassword;
	protected boolean loggedOn;
	protected byte skip;

	// --- CONSTRUCTOR ---

	public RemoteReader(RemoteRepl remoteRepl, SocketChannel channel, SelectionKey key, boolean authenticated,
			boolean echo, String username, String password) {
		this.remoteRepl = remoteRepl;
		this.channel = channel;
		this.key = key;
		this.echo = echo;
		this.username = username;
		this.password = password;
		this.loggedOn = !authenticated;
		String header = "Moleculer Microservice Framework V" + ServiceBroker.SOFTWARE_VERSION;
		int len = (79 - header.length()) / 2;
		String spaces = "                                                                               ";
		String title = spaces.substring(0, len) + header + spaces.substring(0, len);
		if (title.length() < 79) {
			title += " ";
		}
		header = "\u001B[7m" + spaces + "\r\n" + title + "\r\n" + spaces + "\u001B[0m\r\n\r\n";
		if (!loggedOn) {
			header += "Username:";
		} else {
			header += "Welcome anonymous!\r\n" + "Type \"help\" for more information.\r\n\r\n";
			header += "mol $ ";
		}
		addString(header, true);
	}

	// --- READING FROM SOCKET ---

	protected static final int MAX_LEN = 1000;

	protected char[] request = new char[MAX_LEN];

	protected int counter = 0;

	protected void readPacket() {
		try {
			synchronized (requestBuffer) {
				requestBuffer.clear();
				int len = channel.read(requestBuffer);
				if (len == -1) {
					throw new IOException();
				}
				if (len == 1) {
					int read = requestBuffer.array()[0] & 0xff;
					if (skip != 0) {
						skip--;
						return;
					}
					if (read == 255) {
						skip = 2;
						return;
					}
					char c = (char) read;
					if ((c > 31 && c < 128) || c == '\r' || c == '\n' || c == '\b') {

						// Enter?
						if (c == '\n') {
							return;
						}
						if (c == '\r') {

							// New line
							if (echo) {
								addBytes(LINE_BREAK, true);
							}

							if (counter != 0) {

								// Command line
								String commandLine = new String(request, 0, counter);
								commandLine = commandLine.trim();

								// Execute command
								if (commandLine.length() != 0) {
									processCommand(commandLine);
								}
							}

							// Clear counter
							counter = 0;
						} else {

							// Handle backspace
							if (c == '\b') {
								if (counter > 0) {

									// Send back the backspace
									if (echo) {
										addBytes(BACKSPACE, true);
									}

									// Decrease counter
									counter--;
								}
							} else {

								// Send back the character
								if (echo) {
									addBytes(HIGHLIGHT, true);
									if (maskPassword) {
										addChar('*');
									} else {
										addChar(c);
									}
									addBytes(NORMAL, true);
								}

								// Increase counter
								request[counter] = c;
								counter++;
								if (counter == MAX_LEN) {

									// Too long row!
									addString("Too long row!", true);
									counter = 0;
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {

			// Close channel
			close();
		}
	}

	// --- WRITING TO SOCKET ---

	protected final LinkedList<ByteBuffer> responseBuffers = new LinkedList<ByteBuffer>();

	protected ByteBuffer currentBuffer;

	protected void writePacket() {
		try {
			if (currentBuffer == null) {
				synchronized (responseBuffers) {
					if (responseBuffers.isEmpty()) {
						key.interestOps(SelectionKey.OP_READ);
						return;
					}
					currentBuffer = (ByteBuffer) responseBuffers.removeFirst();
				}
			}

			// Write data
			channel.write(currentBuffer);

			// Clear buffer
			if (currentBuffer.hasRemaining()) {
				synchronized (responseBuffers) {
					if (responseBuffers.isEmpty()) {
						key.interestOps(SelectionKey.OP_READ);
					}
				}
				return;
			}
			currentBuffer = null;

		} catch (Exception e) {

			// Close channel
			close();
		}
	}

	protected void addChar(char c) {
		byte[] bytes = new byte[1];
		bytes[0] = (byte) c;
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		synchronized (responseBuffers) {
			responseBuffers.addLast(buffer);
			key.interestOps(SelectionKey.OP_WRITE);
		}
	}

	protected void addBytes(byte[] bytes, boolean directSet) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		synchronized (responseBuffers) {
			responseBuffers.addLast(buffer);
			if (directSet) {
				key.interestOps(SelectionKey.OP_WRITE);
			} else {
				remoteRepl.markAsWritable(key);
			}
		}
	}

	protected void addString(String string, boolean directSet) {
		byte[] bytes = null;
		try {
			bytes = string.getBytes(StandardCharsets.US_ASCII);
		} catch (Exception e1) {
			bytes = string.getBytes();
		}
		if (bytes != null) {
			try {
				ByteBuffer buffer = ByteBuffer.wrap(bytes);
				synchronized (responseBuffers) {
					responseBuffers.addLast(buffer);
					if (directSet) {
						key.interestOps(SelectionKey.OP_WRITE);
					} else {
						remoteRepl.markAsWritable(key);
					}
				}
			} catch (Exception ignored) {
			}
		}
	}

	// --- CLOSE CHANNEL ---

	protected void close() {
		if (channel != null) {
			try {
				channel.close();
			} catch (Exception ignored) {
			}
		}
		remoteRepl.removeBuffer(this);
	}

	// --- INVOKE COMMAND PROCESSOR ---

	protected String enteredName;
	protected String lastCommand = "help";

	protected void processCommand(String command) throws Exception {

		// Check password
		if (!loggedOn) {
			if (enteredName == null) {
				enteredName = command;
				maskPassword = true;
				addString("Password:", true);
				return;
			}
			maskPassword = false;

			// Check username and password
			String error = null;
			if (!enteredName.equals(username)) {
				error = "Unknown user!";
			} else {
				if (!password.equals(command)) {
					error = "Invalid password for " + enteredName + "!";
				}
			}
			if (error != null) {
				error = "\r\n\r\nAccess denied! " + error + (char) 7;
				channel.write(ByteBuffer.wrap(error.getBytes(StandardCharsets.US_ASCII)));
				Thread.sleep(1000);
				close();
				return;
			}

			// Ok, user is authenticated
			addString("\r\nWelcome " + enteredName + "!\r\nType \"help\" for more information.\r\nmol $ ", true);
			loggedOn = true;
			return;
		}

		// Exit from telnet
		if (command.equalsIgnoreCase("close")) {
			close();
			return;
		}

		// Invoke processor
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos, true, "US-ASCII");
		if ("r".equalsIgnoreCase(command) || "repeat".equalsIgnoreCase(command)) {
			command = lastCommand;
		}
		if (command.equals("help") || command.startsWith("nodes") || command.startsWith("actions")) {
			command += " telnet";
		}
		remoteRepl.onCommand(out, command);
		out.print("mol $ ");
		lastCommand = command;

		// Send back response
		addBytes(baos.toByteArray(), true);		
	}
	
}