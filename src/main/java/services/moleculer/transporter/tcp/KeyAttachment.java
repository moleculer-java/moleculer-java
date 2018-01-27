package services.moleculer.transporter.tcp;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;

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