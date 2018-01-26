package services.moleculer.transporter.tcp;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

public class KeyAttachment {

	// --- PROPERTIES ---
	
	public final String nodeID;
	public final String host;
	public final int port;
	
	protected ByteBuffer first;
	protected final LinkedList<ByteBuffer> buffers = new LinkedList<>();

	
	// --- CONSTRUCTOR ---
	
	public KeyAttachment(String nodeID, String host, int port, byte[] packet) {
		this.nodeID = nodeID;
		this.host = host;
		this.port = port;
		this.first = ByteBuffer.wrap(packet);
	}

	// --- ADD BYTES ---
	
	public void append(byte[] packet) {
		ByteBuffer buffer = ByteBuffer.wrap(packet);
		synchronized (buffers) {
			buffers.addLast(buffer);
		}
	}
	
	// --- UNUSED? ---
	
	public boolean isUnused() {
		if (first == null) {
			synchronized (buffers) {
				return buffers.isEmpty();
			}
		}
		return false;
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