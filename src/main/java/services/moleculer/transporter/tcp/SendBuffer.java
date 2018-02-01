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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Attachment of TcpWriter's channels.
 */
public final class SendBuffer {

	// --- PROPERTIES ---

	public final String nodeID;
	public final String host;
	public final int port;

	private ByteBuffer first;
	private final LinkedList<ByteBuffer> buffers = new LinkedList<>();
	private final AtomicLong lastUsed;
	private final AtomicReference<SelectionKey> key = new AtomicReference<>();

	// --- CONSTRUCTOR ---

	SendBuffer(String nodeID, String host, int port, byte[] packet) {
		this.nodeID = nodeID;
		this.host = host;
		this.port = port;
		this.first = ByteBuffer.wrap(packet);
		this.lastUsed = new AtomicLong(System.currentTimeMillis());
	}

	// --- SELECTION KEY ---

	final void key(SelectionKey registeredKey) {
		key.compareAndSet(null, registeredKey);
	}

	final SelectionKey key() {
		return key.get();
	}

	// --- INVALIDATE / TOUCH ---

	final boolean use() {
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

	final void invalidate() {
		lastUsed.set(0);
	}
	
	final boolean invalidate(long limit) {
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

	final void append(byte[] packet) {
		ByteBuffer buffer = ByteBuffer.wrap(packet);
		synchronized (buffers) {
			buffers.addLast(buffer);
		}
	}

	// --- WRITE BYTES ---

	final boolean write(SocketChannel channel) throws Exception {
		shift();
		int bytes = channel.write(first);
		System.out.println(bytes + " bytes submitted.");
		return shift();
	}

	private final boolean shift() {
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

	public final byte[] getCurrentPacket() {
		synchronized (buffers) {
			if (first == null) {
				if (buffers.isEmpty()) {
					return null;
				}
				return buffers.getFirst().array();
			}
			return first.array();
		}
	}
	
	// --- COLLECTION HELPERS ---
	
	@Override
	public final int hashCode() {
		return nodeID.hashCode();
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SendBuffer other = (SendBuffer) obj;
		return nodeID.equals(other.nodeID);
	}	

}