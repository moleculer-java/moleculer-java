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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.datatree.Promise;

public class PacketStream {

	// --- CONSTANTS ---

	protected static final int DEFAULT_BUFFER_SIZE = 1024 * 16;

	// --- DATA SOURCE ---

	protected final PacketSource source;

	// --- TRANSFERING VARIABLES ---

	protected OutgoingPacket handler;
	protected ScheduledExecutorService scheduler;
	protected long delay;

	// --- "ALL PACKETS TRANSFERED" MARKER ---

	protected final Promise finished = new Promise();

	// --- CONSTRUCTORS ---

	public PacketStream(File source) throws FileNotFoundException {
		this(new FileInputStream(source));
	}

	public PacketStream(InputStream source) {
		this(source, true, DEFAULT_BUFFER_SIZE);
	}

	public PacketStream(InputStream source, boolean closeStream, int bufferSize) {
		this(new PacketSource() {

			private final byte[] buffer = new byte[bufferSize];

			@Override
			public void sendNext(OutgoingPacket packet) throws Throwable {
				try {
					int len = source.read(buffer);
					if (len < 0) {
						try {
							packet.sendClose();
						} catch (Exception ignored) {
						}
						close();
						return;
					}
					byte[] copy = new byte[len];
					System.arraycopy(buffer, 0, copy, 0, len);
					packet.sendData(copy);
				} catch (Exception cause) {
					// TODO log exception
					try {
						packet.sendError(cause);
					} catch (Exception ignored) {
					}
					close();
				}
			}

			@Override
			protected void finalize() throws Throwable {
				close();
			}

			private void close() {
				if (closeStream) {
					try {
						source.close();
					} catch (Exception ignored) {
					}
				}
			}

		});
	}

	public PacketStream(NonBlockingQueue source) {
		this((PacketSource) source);
	}
	
	public PacketStream(PacketSource source) {
		this.source = Objects.requireNonNull(source);
	}

	// --- DESTINATIONS ---

	public Promise pipe(File destination) throws FileNotFoundException {
		return pipe(destination, false, DEFAULT_BUFFER_SIZE);
	}

	public Promise pipe(File destination, boolean append, int bufferSize) throws FileNotFoundException {
		return pipe(new FileOutputStream(destination, append), true, bufferSize);
	}

	public Promise pipe(OutputStream destination) {
		boolean buffered = destination instanceof BufferedOutputStream;
		return pipe(destination, true, buffered ? 0 : DEFAULT_BUFFER_SIZE);
	}

	public Promise pipe(OutputStream destination, boolean closeStream, int bufferSize) {
		final OutputStream out = bufferSize > 1 ? new BufferedOutputStream(destination, bufferSize) : destination;
		return pipe(new PacketReceiver() {

			@Override
			public void onData(byte[] bytes) throws Exception {
				out.write(bytes);
			}

			@Override
			public void onError(Throwable cause) throws Exception {

				// TODO log exception
				close();
			}

			@Override
			public void onClose() throws Exception {
				close();
			}

			@Override
			protected void finalize() throws Throwable {
				close();
			}

			private void close() {
				if (closeStream) {
					try {
						out.flush();
						out.close();
					} catch (Exception ignored) {
					}
				}
			}

		});
	}

	public Promise pipe(WritableByteChannel destination) {
		return pipe(destination, true, DEFAULT_BUFFER_SIZE);
	}

	public Promise pipe(WritableByteChannel destination, boolean closeChannel, int bufferSize) {
		if (bufferSize > 1) {
			return pipe(new OutputStream() {

				@Override
				public void write(int b) throws IOException {
					write(new byte[] { (byte) b });
				}

				@Override
				public void write(byte[] b) throws IOException {
					destination.write(ByteBuffer.wrap(b));
				}

				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					destination.write(ByteBuffer.wrap(b, off, len));
				}

				@Override
				public void flush() throws IOException {
				}

				@Override
				public void close() throws IOException {
					destination.close();
				}

			}, closeChannel, bufferSize);
		}
		return pipe(new PacketReceiver() {

			@Override
			public void onData(byte[] bytes) throws Exception {
				destination.write(ByteBuffer.wrap(bytes));
			}

			@Override
			public void onError(Throwable cause) throws Exception {

				// TODO log exception
				close();
			}

			@Override
			public void onClose() throws Exception {
				close();
			}

			@Override
			protected void finalize() throws Throwable {
				close();
			}

			private void close() {
				if (closeChannel) {
					try {
						destination.close();
					} catch (Exception ignored) {
					}
				}
			}

		});
	}

	public Promise pipe(PacketReceiver destination) {
		this.handler = new OutgoingPacket(this, destination, finished);
		return finished;
	}

	public Promise pipe() {
		return finished;
	}
	
	// --- TRANSFER PACKETS ---

	public Promise transfer(ScheduledExecutorService scheduler, long delay) {
		this.delay = delay;
		this.scheduler = Objects.requireNonNull(scheduler);
		transferNext();
		return finished;
	}

	protected final void transferNext() {
		if (!finished.isDone()) {
			scheduler.schedule(() -> {
				try {
					if (handler == null) {
						Thread.sleep(100);
						transferNext();
					} else {
						source.sendNext(handler);
					}
				} catch (Throwable cause) {
					handler.sendError(cause);
				}
			}, delay, TimeUnit.MILLISECONDS);
		}
	}

}