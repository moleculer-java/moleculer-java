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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.datatree.Promise;

/**
 * !!! These package are in development phase !!!
 */
public class OutgoingStream {

	// --- CONSTANTS ---

	protected static final int DEFAULT_PACKET_SIZE = 1024 * 16;

	// --- VARIABLES ---

	protected HashSet<IncomingStream> streams = new HashSet<>();

	protected AtomicBoolean closed = new AtomicBoolean();

	// --- CONNECT / DISCONNECT PEER ---

	public boolean connect(IncomingStream stream) {
		return streams.add(stream);
	}

	public boolean disconnect(IncomingStream stream) {
		return streams.remove(stream);
	}

	// --- SEND METHODS ---

	public OutgoingStream sendData(byte[] bytes) throws IOException {
		if (closed.get()) {
			throw new IOException("Stream closed!");
		}
		if (streams.isEmpty()) {
			throw new IOException("IncomingStream not connected!");
		}
		IOException cause = null;
		for (IncomingStream stream : streams) {
			for (DataListener listener : stream.dataListeners) {
				try {
					listener.onData(bytes);
				} catch (IOException ioe) {
					cause = ioe;
				}
			}
		}
		if (cause != null) {
			sendError(cause);
			throw cause;
		}
		return this;
	}

	public OutgoingStream sendError(Throwable cause) {
		if (closed.compareAndSet(false, true)) {
			for (IncomingStream stream : streams) {
				for (ErrorListener listener : stream.errorListeners) {
					listener.onError(cause);
				}
			}
		}
		return this;
	}

	public OutgoingStream sendClose() {
		if (closed.compareAndSet(false, true)) {
			for (IncomingStream stream : streams) {
				for (CloseListener listener : stream.closeListeners) {
					listener.onClose();
				}
			}
		}
		return this;
	}

	// --- GETTERS ---

	public boolean isClosed() {
		return closed.get();
	}

	public boolean isConnected() {
		return !streams.isEmpty();
	}

	// --- ACT AS OUTPUT STREAM ---

	public OutputStream asOutputStream() {
		return asOutputStream(DEFAULT_PACKET_SIZE);
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
				System.arraycopy(b, 0, copy, 0, len);
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

	// --- ACT AS BYTE CHANNEL ---

	public WritableByteChannel asByteChannel() {
		return asByteChannel(DEFAULT_PACKET_SIZE);
	}

	public WritableByteChannel asByteChannel(int packetSize) {
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
						if (src.hasArray()) {
							byte[] bytes = src.array();
							if (bytes.length > 0) {
								sendData(bytes);
							}
							return bytes.length;
						}
						int len = Math.min(packetSize, src.remaining());
						if (len > 0) {
							byte[] packet = new byte[len];
							src.get(packet);
							sendData(packet);
						}
						return len;
					} catch (IOException cause) {
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
		OutputStream out = asOutputStream(packetSize);
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
					if (src.hasArray()) {
						byte[] bytes = src.array();
						if (bytes.length > 0) {
							out.write(bytes);
						}
						return bytes.length;
					}
					int len = Math.min(packetSize, src.remaining());
					if (len > 0) {
						byte[] packet = new byte[len];
						src.get(packet);
						out.write(packet);
					}
					return len;
				} catch (IOException cause) {
					try {
						sendError(cause);
					} catch (Throwable ignored) {
					}
					throw cause;
				}
			}

		};
	}

	// --- TRANSFER METHODS ---

	public Promise transferFrom(File source) {
		try {
			return transferFrom(new FileInputStream(source));
		} catch (Throwable cause) {
			return Promise.reject(cause);
		}
	}

	public Promise transferFrom(File source, int packetSize) {
		try {
			return transferFrom(new FileInputStream(source), packetSize);
		} catch (Throwable cause) {
			return Promise.reject(cause);
		}
	}

	public Promise transferFrom(File source, ScheduledExecutorService scheduler, int packetSize, long packetDelay) {
		try {
			return transferFrom(new FileInputStream(source), scheduler, packetSize, packetDelay);
		} catch (Throwable cause) {
			return Promise.reject(cause);
		}
	}

	public Promise transferFrom(InputStream source) {
		return transferFrom(source, DEFAULT_PACKET_SIZE);
	}

	public Promise transferFrom(InputStream source, int packetSize) {
		return transferFrom(source, new ScheduledExecutorService() {

			@Override
			public final void execute(Runnable command) {
			}

			@Override
			public final <T> Future<T> submit(Runnable task, T result) {
				return null;
			}

			@Override
			public final Future<?> submit(Runnable task) {
				return null;
			}

			@Override
			public final <T> Future<T> submit(Callable<T> task) {
				return null;
			}

			@Override
			public final List<Runnable> shutdownNow() {
				return null;
			}

			@Override
			public final void shutdown() {
			}

			@Override
			public final boolean isTerminated() {
				return false;
			}

			@Override
			public final boolean isShutdown() {
				return false;
			}

			@Override
			public final <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				return null;
			}

			@Override
			public final <T> T invokeAny(Collection<? extends Callable<T>> tasks)
					throws InterruptedException, ExecutionException {
				return null;
			}

			@Override
			public final <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
					TimeUnit unit) throws InterruptedException {
				return null;
			}

			@Override
			public final <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
					throws InterruptedException {
				return null;
			}

			@Override
			public final boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
				return false;
			}

			@Override
			public final ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
					TimeUnit unit) {
				return null;
			}

			@Override
			public final ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
					TimeUnit unit) {
				return null;
			}

			@Override
			public final <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
				return null;
			}

			@Override
			public final ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
				ForkJoinPool.commonPool().execute(command);
				return null;
			}

		}, packetSize, 0);
	}

	public Promise transferFrom(InputStream source, ScheduledExecutorService scheduler, int packetSize,
			long packetDelay) {
		byte[] packet = new byte[packetSize < 2 ? DEFAULT_PACKET_SIZE : packetSize];
		Promise promise = new Promise();
		OutputStream destination = asOutputStream(packetSize);
		scheduleNextPacket(source, destination, scheduler, packetDelay, promise, packet);
		return promise;
	}

	protected void scheduleNextPacket(InputStream source, OutputStream destination, ScheduledExecutorService scheduler,
			long packetDelay, Promise promise, byte[] packet) {
		scheduler.schedule(() -> {
			try {
				int len = source.read(packet);
				if (len < 0) {
					try {
						source.close();
					} catch (Throwable ignored) {
					}
					try {
						destination.close();
					} finally {
						promise.complete();
					}
				} else if (len == 0) {
					scheduleNextPacket(source, destination, scheduler, packetDelay, promise, packet);
				} else {
					byte[] copy = new byte[len];
					System.arraycopy(packet, 0, copy, 0, len);
					destination.write(copy);					
					scheduleNextPacket(source, destination, scheduler, packetDelay, promise, packet);
				}
			} catch (IOException cause) {
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
				} finally {
					promise.complete(cause);
				}
			}
		}, packetDelay, TimeUnit.MILLISECONDS);
	}

}