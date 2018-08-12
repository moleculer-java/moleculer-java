/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2018 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
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
package services.moleculer.stream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class NonBlockingQueue implements PacketSource {

	// --- CONSTANTS ---

	protected static final int DEFAULT_BUFFER_SIZE = 1024 * 16;

	protected static final Object CLOSE_MARKER = new Object();

	// --- VARIABLES ---

	protected final LinkedList<Object> dataQueue = new LinkedList<>();

	protected final LinkedList<OutgoingPacket> packetQueue = new LinkedList<>();

	protected final AtomicBoolean closed = new AtomicBoolean();

	// --- ACT AS OUTPUT STREAM ---

	public OutputStream asOutputStream() {
		return asOutputStream(DEFAULT_BUFFER_SIZE);
	}

	public OutputStream asOutputStream(int packetSize) {
		OutputStream out = new OutputStream() {

			@Override
			public final void write(int b) throws IOException {
				sendData(new byte[] { (byte) b });
			}

			@Override
			public final void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}

			@Override
			public final void write(byte[] b, int off, int len) throws IOException {
				byte[] copy = new byte[len];
				System.arraycopy(b, len, copy, 0, len);
				sendData(copy);
			}

			@Override
			public final void flush() throws IOException {
			}

			@Override
			public final void close() throws IOException {
				sendClose();
			}

		};
		if (packetSize < 2) {
			return out;
		}
		return new BufferedOutputStream(out, packetSize);
	}

	// --- ACT AS WRITABLE BYTE CHANNEL ---

	public WritableByteChannel asByteChannel() {
		return asByteChannel(DEFAULT_BUFFER_SIZE);
	}

	public WritableByteChannel asByteChannel(int packetSize) {
		if (packetSize < 2) {
			return new WritableByteChannel() {

				@Override
				public boolean isOpen() {
					return !closed.get();
				}

				@Override
				public void close() throws IOException {
					sendClose();
				}

				@Override
				public int write(ByteBuffer src) throws IOException {
					if (src.hasArray()) {
						byte[] b = src.array();
						sendData(b);
						return b.length;
					}
					src.rewind();
					byte[] b = new byte[src.remaining()];
					src.get(b);
					sendData(b);
					return b.length;
				}

			};
		}
		OutputStream out = asOutputStream(packetSize);
		return new WritableByteChannel() {

			@Override
			public boolean isOpen() {
				return !closed.get();
			}

			@Override
			public void close() throws IOException {
				out.flush();
				out.close();
			}

			@Override
			public int write(ByteBuffer src) throws IOException {
				if (src.hasArray()) {
					byte[] b = src.array();
					out.write(b, 0, b.length);
					return b.length;
				}
				src.rewind();
				byte[] b = new byte[src.remaining()];
				src.get(b);
				out.write(b, 0, b.length);
				return b.length;
			}

		};
	}

	// --- BASIC SEND METHODS ---

	public void sendData(byte[] bytes) {
		if (!closed.get() && bytes != null && bytes.length > 0) {
			synchronized (this) {
				dataQueue.addLast(bytes);
			}
			transfer();
		}
	}

	public void sendError(Throwable cause) {
		Objects.requireNonNull(cause);
		if (!closed.get()) {
			synchronized (this) {
				dataQueue.addLast(cause);
			}
			transfer();
		}
	}

	public void sendClose() {
		if (!closed.get()) {
			synchronized (this) {
				dataQueue.addLast(CLOSE_MARKER);
			}
			transfer();
		}
	}

	// --- INTERNAL METHODS ---

	@Override
	public void sendNext(OutgoingPacket packet) throws Throwable {
		Objects.requireNonNull(packet);
		synchronized (this) {			
			packetQueue.addLast(packet);
		}
		transfer();
	}

	protected void transfer() {
		OutgoingPacket nextPacket = null;
		Object nextData = null;
		synchronized (this) {
			if (packetQueue.isEmpty() || dataQueue.isEmpty()) {
				return;
			}
			nextPacket = packetQueue.removeFirst();
			nextData = dataQueue.removeFirst();
		}
		if (nextData instanceof byte[]) {
			nextPacket.sendData((byte[]) nextData);
			return;
		}
		if (nextData instanceof Throwable) {
			nextPacket.sendError((Throwable) nextData);
			return;
		}
		if (nextData == CLOSE_MARKER) {
			nextPacket.sendClose();
			return;
		}
		throw new IllegalArgumentException("Invalid data type (" + nextData + ")!");
	}

}
