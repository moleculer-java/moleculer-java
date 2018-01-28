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

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;

/**
* Attachment of TcpWriter's channels.
*/
public class KeyAttachment {

	// --- PROPERTIES ---

	public final String nodeID;
	public final String host;
	public final int port;

	protected ByteBuffer first;
	protected final LinkedList<ByteBuffer> buffers = new LinkedList<>();

	protected final AtomicLong lastUsed;

	// --- CONSTRUCTOR ---

	public KeyAttachment(String nodeID, String host, int port, byte[] packet) {
		this.nodeID = nodeID;
		this.host = host;
		this.port = port;
		this.first = ByteBuffer.wrap(packet);
		this.lastUsed = new AtomicLong(System.currentTimeMillis());
	}

	// --- INVALIDATE / TOUCH ---

	public boolean use() {
		long current;
		long now = System.currentTimeMillis();
		while (true) {
			current = lastUsed.get();
			if (current == 0) {
				return false;
			}
			if (current > now) {
				return true;
			}
			if (lastUsed.compareAndSet(current, now)) {
				return true;
			}
		}
	}

	public boolean invalidate(long limit) {
		long current = lastUsed.get();
		if (current < limit && first == null) {
			synchronized (buffers) {
				if (buffers.isEmpty()) {
					return lastUsed.compareAndSet(current, 0);
				}
			}
		}
		return false;
	}

	// --- ADD BYTES ---

	public void append(byte[] packet) {
		ByteBuffer buffer = ByteBuffer.wrap(packet);
		synchronized (buffers) {
			buffers.addLast(buffer);
		}
	}

	// --- WRITE BYTES ---

	public boolean write(SocketChannel channel) throws Exception {
		shift();
		channel.write(first);
		return shift();
	}

	protected boolean shift() {
		if (first == null) {
			synchronized (buffers) {
				if (buffers.isEmpty()) {
					return false;
				}
				first = buffers.removeFirst();
			}
			return true;
		}
		if (first.hasRemaining()) {
			return true;
		}
		first = null;
		synchronized (buffers) {
			if (buffers.isEmpty()) {
				return false;
			}
			first = buffers.removeFirst();
		}
		return true;
	}

	// --- GET CURRENT PACKET ---

	public byte[] getCurrentPacket() {
		if (first == null) {
			synchronized (buffers) {
				if (buffers.isEmpty()) {
					return null;
				}
				return buffers.getFirst().array();
			}
		}
		return first.array();
	}

}