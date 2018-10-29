/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.datatree.Promise;

/**
 * !!! This package are in development phase !!!
 */
public class PacketStream {

	// --- CONSTANTS ---

	protected static final int DEFAULT_MIN_PACKET_SIZE = 1024 * 16;

	protected static final byte[] CLOSE_MARKER = new byte[0];

	// --- COMPONENTS ---

	protected final ScheduledExecutorService scheduler;

	// --- VARIABLES ---

	protected final AtomicBoolean closed = new AtomicBoolean();

	protected final LinkedList<byte[]> buffer = new LinkedList<>();

	protected final AtomicLong transferedBytes = new AtomicLong();

	protected PacketListener[] listeners;

	protected Throwable cause;

	protected int packetSize = DEFAULT_MIN_PACKET_SIZE;

	protected long packetDelay = 10;

	// --- CONSTRUCTOR ---

	public PacketStream(ScheduledExecutorService scheduler) {
		this.scheduler = scheduler;
	}

	// --- SET EVENT LISTENER ---

	public boolean onPacket(PacketListener listener) throws IOException {
		if (listener == null) {
			return false;
		}
		if (listeners == null) {
			listeners = new PacketListener[] { listener };
		} else {
			for (PacketListener test : listeners) {
				if (test == listener) {
					return false;
				}
			}
			PacketListener[] copy = new PacketListener[listeners.length];
			System.arraycopy(listeners, 0, copy, 0, listeners.length);
			copy[listeners.length] = listener;
			listeners = copy;
		}
		if (cause == null) {
			for (byte[] bytes : buffer) {
				if (bytes == CLOSE_MARKER) {
					listener.onPacket(null, null, true);
				} else {
					listener.onPacket(bytes, null, false);
				}
			}
		} else {
			listener.onPacket(null, cause, true);
		}
		return true;
	}

	// --- SEND BYTES ---
	
	public boolean sendData(byte[] bytes) throws IOException {
		if (bytes != null && bytes.length > 0 && !closed.get()) {
			if (listeners == null) {
				buffer.addLast(bytes);
			} else if (listeners.length == 1) {
				listeners[0].onPacket(bytes, null, false);
			} else {
				for (PacketListener listener : listeners) {
					listener.onPacket(bytes, null, false);
				}
			}
			transferedBytes.addAndGet(bytes.length);
			return true;
		}
		return false;
	}

	// --- SEND ERROR ---

	public boolean sendError(Throwable cause) throws IOException {
		if (cause == null) {
			throw new IllegalArgumentException("Unable to send \"null\" as Exception!");
		}
		if (closed.compareAndSet(false, true)) {
			this.cause = cause;
			if (listeners != null) {
				if (listeners.length == 1) {
					listeners[0].onPacket(null, cause, true);
				} else {
					for (PacketListener listener : listeners) {
						listener.onPacket(null, cause, true);
					}
				}
			}
			return true;
		}
		return false;
	}

	// --- SEND CLOSE MARKER ---

	public boolean sendClose() throws IOException {
		if (closed.compareAndSet(false, true)) {
			if (listeners == null) {
				buffer.addLast(CLOSE_MARKER);
			} else if (listeners.length == 1) {
				listeners[0].onPacket(null, cause, true);
			} else {
				for (PacketListener listener : listeners) {
					listener.onPacket(null, cause, true);
				}
			}
			return true;
		}
		return false;
	}

	// --- ACT AS OUTPUT STREAM ---

	public OutputStream asOutputStream() {
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
				System.arraycopy(b, 0, copy, 0, len);
				sendData(copy);
			}

			@Override
			public final void flush() throws IOException {
				
				// Do nothing
			}

			@Override
			public final void close() throws IOException {
				sendClose();
			}

		};

		// Bufferless/direct output
		if (packetSize < 2) {
			return out;
		}

		// Buffered output
		return new BufferedOutputStream(out, packetSize);
	}

	// --- ACT AS BYTE CHANNEL ---

	public WritableByteChannel asWritableByteChannel() {

		// Bufferless/direct output
		if (packetSize < 2) {
			return new WritableByteChannel() {

				@Override
				public final boolean isOpen() {
					return !closed.get();
				}

				@Override
				public final void close() throws IOException {
					sendClose();
				}

				@Override
				public final int write(ByteBuffer src) throws IOException {
					try {
						int len = src.remaining();
						if (len > 0) {
							byte[] packet = new byte[len];
							src.get(packet);
							sendData(packet);
						}
						return len;
					} catch (Throwable cause) {
						try {
							sendError(cause);
						} catch (Throwable ignored) {
						}
						throw cause;
					}
				}

				@Override
				protected final void finalize() throws Throwable {
					close();
				}

			};
		}

		// Buffered output
		final OutputStream out = asOutputStream();
		return new WritableByteChannel() {

			@Override
			public final boolean isOpen() {
				return !closed.get();
			}

			@Override
			public final void close() throws IOException {
				out.flush();
				out.close();
			}

			@Override
			public final int write(ByteBuffer src) throws IOException {
				try {
					int len = src.remaining();
					if (len > 0) {
						byte[] packet = new byte[len];
						src.get(packet);
						out.write(packet);
					}
					return len;
				} catch (Throwable cause) {
					try {
						sendError(cause);
					} catch (Throwable ignored) {
					}
					throw cause;
				}
			}

		};
	}

	// --- "TRANSFER FROM" METHODS ---

	public Promise transferFrom(File source) {
		try {
			return transferFrom(new FileInputStream(source));
		} catch (Throwable cause) {
			return Promise.reject(cause);
		}
	}

	public Promise transferFrom(InputStream source) {
		byte[] packet = new byte[packetSize < 1 ? DEFAULT_MIN_PACKET_SIZE : packetSize];
		Promise promise = new Promise();
		OutputStream destination = asOutputStream();
		scheduleNextPacket(source, destination, promise, packet);
		return promise;
	}

	public Promise transferFrom(ReadableByteChannel source) {
		ByteBuffer packet = ByteBuffer.allocate(packetSize < 1 ? DEFAULT_MIN_PACKET_SIZE : packetSize);
		Promise promise = new Promise();
		OutputStream destination = asOutputStream();
		scheduleNextPacket(source, destination, promise, packet);
		return promise;
	}

	protected void scheduleNextPacket(ReadableByteChannel source, OutputStream destination, Promise promise,
			ByteBuffer packet) {
		scheduler.schedule(() -> {
			try {
				packet.rewind();
				int len = source.read(packet);
				if (len < 0) {
					try {
						try {
							source.close();
						} catch (Throwable ignored) {
						}
						try {
							destination.close();
						} catch (Throwable ignored) {
						}
					} finally {
						promise.complete();
					}
				} else if (len == 0) {
					scheduleNextPacket(source, destination, promise, packet);
				} else {
					byte[] copy = new byte[len];
					System.arraycopy(packet.array(), 0, copy, 0, len);
					destination.write(copy);
					scheduleNextPacket(source, destination, promise, packet);
				}
			} catch (IOException cause) {
				try {
					try {
						source.close();
					} catch (Throwable ignored) {
					}
					try {
						destination.close();
					} catch (Throwable ignored) {
					}
					try {
						sendError(cause);
					} catch (Throwable ignored) {
					}
				} finally {
					promise.complete(cause);
				}
			}
		}, packetDelay, TimeUnit.MILLISECONDS);
	}

	protected void scheduleNextPacket(InputStream source, OutputStream destination, Promise promise, byte[] packet) {
		scheduler.schedule(() -> {
			try {
				int len = source.read(packet);
				if (len < 0) {
					try {
						try {
							source.close();
						} catch (Throwable ignored) {
						}
						try {
							destination.close();
						} catch (Throwable ignored) {
						}
					} finally {
						promise.complete();
					}
				} else if (len == 0) {
					scheduleNextPacket(source, destination, promise, packet);
				} else {
					byte[] copy = new byte[len];
					System.arraycopy(packet, 0, copy, 0, len);
					destination.write(copy);
					scheduleNextPacket(source, destination, promise, packet);
				}
			} catch (IOException cause) {
				try {
					try {
						source.close();
					} catch (Throwable ignored) {
					}
					try {
						destination.close();
					} catch (Throwable ignored) {
					}
					try {
						sendError(cause);
					} catch (Throwable ignored) {
					}
				} finally {
					promise.complete(cause);
				}
			}
		}, packetDelay, TimeUnit.MILLISECONDS);
	}

	// --- "TRANSFER TO" METHODS ---

	public Promise transferTo(File destination) {
		try {
			return transferTo(new FileOutputStream(destination));
		} catch (Throwable cause) {
			return Promise.reject(cause);
		}
	}

	public Promise transferTo(OutputStream destination) {
		return new Promise(res -> {
			onPacket((bytes, cause, close) -> {

				// Data received
				if (bytes != null) {
					destination.write(bytes);
				}

				// Close received
				if (close) {
					try {
						destination.flush();
						destination.close();
						if (cause == null) {
							res.resolve();
						}
					} catch (Throwable error) {
						cause = error;
					}
				}

				// Error received
				if (cause != null) {
					res.reject(cause);
				}

			});
		});
	}

	public Promise transferTo(WritableByteChannel destination) {
		return new Promise(res -> {
			onPacket((bytes, cause, close) -> {

				// Data received
				if (bytes != null) {
					ByteBuffer buffer = ByteBuffer.wrap(bytes);
					int pos = 0;
					while (buffer.hasRemaining()) {
						pos += destination.write(buffer);
						buffer.position(pos);
					}
				}

				// Close received
				if (close) {
					try {
						destination.close();
						if (cause == null) {
							res.resolve();
						}
					} catch (Throwable error) {
						cause = error;
					}
				}

				// Error received
				if (cause != null) {
					res.reject(cause);
				}

			});
		});
	}
	
	// --- GETTERS AND SETTERS ---

	public int getPacketSize() {
		return packetSize;
	}

	public void setPacketSize(int packetSize) {
		this.packetSize = packetSize < 0 ? 0 : packetSize;
	}

	public long getPacketDelay() {
		return packetDelay;
	}

	public void setPacketDelay(long packetDelay) {
		this.packetDelay = packetDelay < 0 ? 0 : packetDelay;
	}

	public boolean isClosed() {
		return closed.get();
	}

	public long getTransferedBytes() {
		return transferedBytes.get();
	}

}