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

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Attachment of TcpWriter's SelectionKeys.
 */
public class SendBuffer {

	// --- OUTGOING QUEUE ---

	protected final ConcurrentLinkedQueue<ByteBuffer> queue = new ConcurrentLinkedQueue<>();

	// --- USED / NOT USED ---

	protected static final ByteBuffer BUFFER_IS_CLOSED = ByteBuffer.allocate(1);

	protected AtomicReference<ByteBuffer> blockerBuffer = new AtomicReference<>();

	// --- PROPERTIES ---

	public final String nodeID;
	public final String host;
	public final int port;

	// --- CONSTRUCTOR ---

	protected SendBuffer(String nodeID, String host, int port) {
		this.nodeID = nodeID;
		this.host = host;
		this.port = port;
	}

	// --- CONNECTED ---

	protected SocketChannel channel;
	protected SelectionKey key;

	protected void connected(SelectionKey key, SocketChannel channel) {
		this.channel = channel;
		this.key = key;
	}

	// --- ADD BYTES ---

	/**
	 * Adds a packet to the buffer's queue.
	 * 
	 * @param packet
	 *            packet to write
	 * 
	 * @return true, if success (false = buffer is closed)
	 */
	protected boolean append(byte[] packet) {
		ByteBuffer buffer = ByteBuffer.wrap(packet);
		ByteBuffer blocker;
		while (true) {
			blocker = blockerBuffer.get();
			if (blocker == BUFFER_IS_CLOSED) {
				return false;
			}
			if (blockerBuffer.compareAndSet(blocker, buffer)) {
				queue.add(buffer);
				return true;
			}
		}
	}

	// --- CLOSE IF UNUSED ---

	/**
	 * Tries to close this buffer.
	 * 
	 * @return true, is closed (false = buffer is not empty)
	 */
	protected boolean tryClose() {
		ByteBuffer blocker = blockerBuffer.get();
		if (blocker == BUFFER_IS_CLOSED) {
			return true;
		}
		if (blocker != null) {
			return false;
		}
		boolean closed = blockerBuffer.compareAndSet(null, BUFFER_IS_CLOSED);
		if (closed) {
			closeResources();
			return true;
		}
		return false;
	}

	// --- CLOSE BUFFER ---

	void close() {
		blockerBuffer.set(BUFFER_IS_CLOSED);
		closeResources();
	}

	protected void closeResources() {
		if (key != null) {
			try {
				key.cancel();
			} catch (Exception ignored) {
			}
			key = null;
		}
		queue.clear();
		if (channel != null) {
			try {
				channel.close();
			} catch (Exception ignored) {
			}
			channel = null;
		}
	}

	// --- WRITE BYTES ---

	/**
	 * Writes N bytes to the target channel.
	 * 
	 * @throws Exception
	 *             any I/O exception
	 *             
	 * @return unsuccessful write (false = wrote any bytes)
	 */
	protected boolean write() throws Exception {
		ByteBuffer buffer = queue.peek();
		if (buffer == null) {
			if (key != null) {
				key.interestOps(0);
			}
			return true;
		}
		if (channel != null) {
			int count = channel.write(buffer);
			if (count == 0 && buffer.hasRemaining()) {
				if (key != null) {
					key.interestOps(SelectionKey.OP_WRITE);
				}
				return true;
			} else if (count == -1) {
				throw new EOFException();
			}
			if (!buffer.hasRemaining()) {
				queue.poll();
			}
			if (queue.isEmpty()) {
				if (blockerBuffer.compareAndSet(buffer, null)) {
					if (key != null) {
						key.interestOps(0);
					}
				}
			}
			return count == 0;
		}
		return true;
	}

	// --- GET CURRENT PACKET ---

	public byte[] getCurrentPacket() {
		ByteBuffer buffer = queue.peek();
		if (buffer == null) {
			return null;
		}
		return buffer.array();
	}

}