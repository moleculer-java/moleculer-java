package services.moleculer.stream;

import java.io.IOException;
import java.io.OutputStream;

import services.moleculer.error.MoleculerError;

public final class PacketOutputStream extends OutputStream {

	private final PacketStream packetStream;
	
	public PacketOutputStream(PacketStream packetStream) {
		this.packetStream = packetStream;		
	}
	
	@Override
	public final void write(int b) throws IOException {
		checkError();
		packetStream.sendData(new byte[] { (byte) b });
	}

	@Override
	public final void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public final void write(byte[] b, int off, int len) throws IOException {
		checkError();
		byte[] copy = new byte[len];
		System.arraycopy(b, 0, copy, 0, len);
		packetStream.sendData(copy);
	}

	@Override
	public final void flush() {

		// Do nothing
	}

	@Override
	public final void close() {
		packetStream.sendClose();
	}

	private final void checkError() throws IOException {
		if (packetStream.cause != null) {
			if (packetStream.cause instanceof MoleculerError) {
				throw (MoleculerError) packetStream.cause;
			}
			if (packetStream.cause instanceof IOException) {
				throw (IOException) packetStream.cause;
			}
			throw new IOException(packetStream.cause);
		}
	}

	public final int getBufferSize() {
		return packetStream.getBufferSize();
	}
	
}
