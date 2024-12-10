package services.moleculer.stream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import services.moleculer.error.MoleculerError;

public final class PacketByteChannel implements WritableByteChannel {

	private final PacketStream packetStream;
	
	public PacketByteChannel(PacketStream packetStream) {
		this.packetStream = packetStream;		
	}

	@Override
	public final boolean isOpen() {
		return !packetStream.closed.get();
	}

	@Override
	public final void close() {
		packetStream.sendClose();
	}

	@Override
	public final int write(ByteBuffer src) throws IOException {
		if (packetStream.cause != null) {
			if (packetStream.cause instanceof MoleculerError) {
				throw (MoleculerError) packetStream.cause;
			}
			if (packetStream.cause instanceof IOException) {
				throw (IOException) packetStream.cause;
			}
			throw new IOException(packetStream.cause);
		}
		try {
			int len = src.remaining();
			if (len > 0) {
				byte[] packet = new byte[len];
				src.get(packet);
				packetStream.sendData(packet);
			}
			return len;
		} catch (Throwable cause) {
			try {
				packetStream.sendError(cause);
			} catch (Throwable ignored) {
			}
			throw cause;
		}
	}

	@Override
	protected final void finalize() throws Throwable {
		close();
	}
	
	public final int getBufferSize() {
		return packetStream.getBufferSize();
	}
	
}
