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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.error.QueueIsFullError;

public class PacketStream {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(PacketStream.class);

	// --- CONSTANTS ---

	protected static final int DEFAULT_CAPACITY = 1024;
	protected static final int DEFAULT_PACKET_SIZE = 1024 * 32;
	protected static final int DEFAULT_PACKET_DELAY = 32;

	protected static final byte[] CLOSE_MARKER = new byte[0];

	// --- VARIABLES ---

	protected final String nodeID;

	protected final LinkedBlockingQueue<byte[]> queue;

	protected final AtomicReference<Throwable> error = new AtomicReference<>();

	protected final AtomicBoolean closed = new AtomicBoolean();

	protected final AtomicBoolean paused = new AtomicBoolean();

	// --- ACTION NAME ---

	protected String action = "unknown";

	// --- HANDLERS ---

	protected final HashSet<PacketListener> listeners = new HashSet<>();

	// --- PIPE'S SCHEDULER ---

	protected ScheduledExecutorService scheduler;

	// --- CONSTRUCTORS ---

	public PacketStream(ServiceBroker broker) {
		this(broker, DEFAULT_CAPACITY);
	}
	
	public PacketStream(ServiceBroker broker, int capacity) {
		queue = new LinkedBlockingQueue<>(capacity);
		scheduler = broker.getConfig().getScheduler();
		nodeID = broker.getNodeID();
	}

	// --- SEND ---

	public void write(byte[] bytes) {
		write(bytes, 0, bytes.length);
	}

	public void write(byte[] bytes, int off, int len) {

		// Check state
		ensureOpen();

		// Check size
		if (len == 0) {
			return;
		}

		// Create copy
		byte[] copy = new byte[len];
		System.arraycopy(bytes, off, copy, 0, len);

		// Add byte array to queue
		if (queue.offer(copy)) {
			invokeListeners();
		} else {
			throw new QueueIsFullError(nodeID, action);
		}
	}

	public void close() {

		// Add the Close Marker to queue
		if (!closed.get()) {
			if (queue.offer(CLOSE_MARKER)) {
				closed.set(true);
				invokeListeners();
			} else {
				throw new QueueIsFullError(nodeID, action);
			}
		}
	}

	public void error(Throwable transferableError) {

		// Check state
		ensureOpen();

		// Store error
		if (error.compareAndSet(null, transferableError)) {
			closed.set(true);
			invokeListeners();
		}
	}

	protected void ensureOpen() {
		if (closed.get()) {
			throw new IllegalStateException("Queue is closed.");
		}
	}

	// --- ACT AS OUTPUT STREAM ---

	public OutputStream asOutputStream() {
		return asOutputStream(DEFAULT_PACKET_SIZE);
	}

	public OutputStream asOutputStream(int packetSize) {
		final PacketStream self = this;
		OutputStream out = new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				self.write(new byte[] { (byte) b });
			}

			@Override
			public void write(byte[] b) throws IOException {
				self.write(b, 0, b.length);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				self.write(b, off, len);
			}

			@Override
			public void flush() throws IOException {

				// Do nothing
			}

			@Override
			public void close() throws IOException {
				self.close();
			}

		};
		if (packetSize < 2) {
			return out;
		}
		return new BufferedOutputStream(out, packetSize);
	}

	// --- ACT AS INPUT STREAM ---

	public InputStream asInputStream() throws IOException {
		return asInputStream(DEFAULT_PACKET_SIZE);
	}
	
	public InputStream asInputStream(int packetSize) throws IOException {
		PipedInputStream in = new PipedInputStream(packetSize);
		PipedOutputStream out = new PipedOutputStream(in);
		addPacketListener(new PacketListener() {
			
			@Override
			public void onError(Throwable cause) throws Exception {
				out.close();
				// TODO log exception
			}
			
			@Override
			public void onData(byte[] bytes) throws Exception {
				out.write(bytes);
			}
			
			@Override
			public void onClose() throws Exception {
				out.close();
			}
			
		});
		return in;
	}

	// --- ADD / REMOVE LISTENERS ---

	public boolean addPacketListener(PacketListener listener) {
		if (listeners.add(listener)) {
			invokeListeners();
			return true;
		}
		return false;
	}

	public boolean removePacketListener(PacketListener listener) {
		return listeners.remove(listener);
	}

	// --- INVOKE LISTENERS ---

	protected void invokeListeners() {

		// Check "paused" state
		if (paused.get()) {
			return;
		}

		// Check listeners
		if (listeners.isEmpty()) {
			return;
		}

		// Transfer data
		byte[] packet;
		while ((packet = queue.poll()) != null) {
			byte[] bytes = (byte[]) packet;
			if (bytes == CLOSE_MARKER) {
				for (PacketListener listener : listeners) {
					try {
						listener.onClose();
					} catch (Throwable cause) {
						// TODO: log exception
					}
				}
				listeners.clear();
				break;
			}
			for (PacketListener listener : listeners) {
				try {
					listener.onData(packet);
				} catch (Throwable cause) {
					// TODO: log exception
				}
			}
			if (paused.get()) {
				break;
			}
		}

		// Transfer error
		Throwable transferableError = error.get();
		if (transferableError != null) {
			for (PacketListener listener : listeners) {
				try {
					listener.onError(transferableError);
				} catch (Throwable cause) {
					// TODO: log exception
				}
			}
			listeners.clear();
			return;
		}
	}

	// --- PAUSE / RESUME ---

	/**
	 * The pause() method will cause a stream in flowing mode to stop emitting
	 * 'data' events, switching out of flowing mode. Any data that becomes
	 * available will remain in the internal buffer.
	 * 
	 * @return
	 */
	public boolean pause() {

		// Check state
		ensureOpen();

		// Switch to "paused" state
		return paused.compareAndSet(false, true);
	}

	/**
	 * The pause() method will cause a stream in flowing mode to stop emitting
	 * 'data' events, switching out of flowing mode. Any data that becomes
	 * available will remain in the internal buffer.
	 * 
	 * @return
	 */
	public boolean resume() {

		// Switch to "paused" state
		if (paused.compareAndSet(true, false)) {
			invokeListeners();
			return true;
		}
		return false;
	}

	// --- "TRANSFER FROM" CHANNEL METHODS ---

	public Promise transferFrom(ReadableByteChannel in) {
		return transferFrom(in, DEFAULT_PACKET_SIZE, DEFAULT_PACKET_DELAY, true);
	}

	public Promise transferFrom(ReadableByteChannel in, int packetSize) {
		return transferFrom(in, packetSize, DEFAULT_PACKET_DELAY, true);
	}

	public Promise transferFrom(ReadableByteChannel in, int packetSize, long packetDelay) {
		return transferFrom(in, packetSize, packetDelay, true);
	}

	public Promise transferFrom(ReadableByteChannel in, int packetSize, long packetDelay, boolean closeStreams) {
		Promise promise = new Promise();
		ByteBuffer packet = ByteBuffer.allocate(packetSize < 1 ? DEFAULT_PACKET_SIZE : packetSize);

		// Use the shared executor or create a temporary one
		boolean stopScheduler = false;
		ScheduledExecutorService taskScheduler = scheduler;
		if (taskScheduler == null) {
			taskScheduler = Executors.newSingleThreadScheduledExecutor();
			stopScheduler = true;
		}
		OutputStream out = asOutputStream(packetSize);

		// Write from scheduled tasks
		scheduledChannelWrite(taskScheduler, promise, in, out, packet, packetDelay < 0 ? 0 : packetDelay, stopScheduler,
				closeStreams);
		return promise;
	}

	protected void scheduledChannelWrite(ScheduledExecutorService taskScheduler, Promise promise,
			ReadableByteChannel in, OutputStream out, ByteBuffer packet, long packetDelay, boolean stopScheduler,
			boolean closeStreams) {
		try {
			if (directChannelWrite(in, out, packet)) {

				// Has more bytes
				taskScheduler.schedule(() -> {
					scheduledChannelWrite(taskScheduler, promise, in, out, packet, packetDelay, stopScheduler,
							closeStreams);
				}, packetDelay, TimeUnit.MILLISECONDS);

			} else {

				// End of channel
				out.flush();
				if (closeStreams) {
					out.close();
					try {
						if (in != null) {
							in.close();
						}
					} catch (Exception ignored) {
					}
				}
				if (stopScheduler) {
					taskScheduler.shutdown();
				}
				promise.complete();
			}
		} catch (Throwable cause) {
			try {
				out.flush();
			} catch (Exception ignored) {
			}
			error(cause);
			if (stopScheduler) {
				taskScheduler.shutdown();
			}
			promise.complete(cause);
		}
	}

	protected boolean directChannelWrite(ReadableByteChannel in, OutputStream out, ByteBuffer packet) throws Throwable {
		int len = in.read(packet);
		if (len == 0) {
			return true;
		}
		if (len == -1) {

			// End of channel
			return false;
		}
		out.write(packet.array(), 0, len);
		return true;
	}

	// --- "TRANSFER FROM" STREAM METHODS ---

	public Promise transferFrom(InputStream in) {
		return transferFrom(in, DEFAULT_PACKET_SIZE);
	}

	public Promise transferFrom(InputStream in, int packetSize) {
		return transferFrom(in, packetSize, DEFAULT_PACKET_DELAY);
	}

	public Promise transferFrom(InputStream in, int packetSize, long packetDelay) {
		return transferFrom(in, packetSize, packetDelay, true);
	}

	public Promise transferFrom(InputStream in, int packetSize, long packetDelay, boolean closeStreams) {
		Promise promise = new Promise();
		byte[] packet = new byte[packetSize < 1 ? DEFAULT_PACKET_SIZE : packetSize];

		// Use the shared executor or create a temporary one
		boolean stopScheduler = false;
		ScheduledExecutorService taskScheduler = scheduler;
		if (taskScheduler == null) {
			taskScheduler = Executors.newSingleThreadScheduledExecutor();
			stopScheduler = true;
		}
		OutputStream out = asOutputStream(packetSize);

		// Write from scheduled tasks
		scheduledStreamWrite(taskScheduler, promise, in, out, packet, packetDelay < 0 ? 0 : packetDelay, stopScheduler,
				closeStreams);
		return promise;
	}

	protected void scheduledStreamWrite(ScheduledExecutorService taskScheduler, Promise promise, InputStream in,
			OutputStream out, byte[] packet, long packetDelay, boolean stopScheduler, boolean closeStreams) {
		try {
			if (directStreamWrite(in, out, packet)) {

				// Has more bytes
				taskScheduler.schedule(() -> {
					scheduledStreamWrite(taskScheduler, promise, in, out, packet, packetDelay, stopScheduler,
							closeStreams);
				}, packetDelay, TimeUnit.MILLISECONDS);

			} else {

				// End of stream
				out.flush();
				if (closeStreams) {
					out.close();
					try {
						if (in != null) {
							in.close();
						}
					} catch (Exception ignored) {
					}
				}
				if (stopScheduler) {
					taskScheduler.shutdown();
				}
				promise.complete();
			}
		} catch (Throwable cause) {
			try {
				out.flush();
			} catch (Exception ignored) {
			}
			error(cause);
			if (stopScheduler) {
				taskScheduler.shutdown();
			}
			promise.complete(cause);
		}
	}

	protected boolean directStreamWrite(InputStream in, OutputStream out, byte[] packet) throws Throwable {
		int len = in.read(packet);
		if (len == 0) {
			return true;
		}
		if (len == -1) {

			// End of stream
			return false;
		}
		out.write(packet, 0, len);
		return true;
	}

	// --- "TRANSFER FROM" FILE METHODS ---

	public Promise transferFrom(File in) {
		return transferFrom(in, DEFAULT_PACKET_SIZE);
	}

	public Promise transferFrom(File in, int packetSize) {
		return transferFrom(in, packetSize, DEFAULT_PACKET_DELAY);
	}

	public Promise transferFrom(File in, int packetSize, long packetDelay) {
		try {
			return transferFrom(new FileInputStream(in), packetSize, packetDelay);
		} catch (Exception cause) {
			return Promise.reject(cause);
		}
	}

	// --- "TRANSFER FROM" URL METHODS ---

	public Promise transferFrom(URL in) {
		return transferFrom(in, DEFAULT_PACKET_SIZE);
	}

	public Promise transferFrom(URL in, int packetSize) {
		return transferFrom(in, packetSize, DEFAULT_PACKET_DELAY);
	}

	public Promise transferFrom(URL in, int packetSize, long packetDelay) {
		try {
			return transferFrom(in.openStream(), packetSize, packetDelay);
		} catch (Exception cause) {
			return Promise.reject(cause);
		}
	}

	// --- "TRANSFER TO" STREAM METHODS ---

	public Promise transferTo(OutputStream out) {
		return transferTo(out, true);
	}

	public Promise transferTo(OutputStream out, boolean closeStream) {
		return new Promise(res -> {
			addPacketListener(new PacketListener() {

				@Override
				public void onError(Throwable cause) throws Exception {
					if (closeStream) {
						out.close();
					}
					res.reject(cause);
				}

				@Override
				public void onData(byte[] bytes) throws Exception {
					out.write(bytes);
				}

				@Override
				public void onClose() throws Exception {
					if (closeStream) {
						out.close();
					}
					res.resolve();
				}

			});
		});
	}

	// --- "TRANSFER TO" CHANNEL METHODS ---

	public Promise transferTo(WritableByteChannel out) {
		return transferTo(out, true);
	}

	public Promise transferTo(WritableByteChannel out, boolean closeChannel) {
		return new Promise(res -> {
			addPacketListener(new PacketListener() {

				@Override
				public void onError(Throwable cause) throws Exception {
					if (closeChannel) {
						out.close();
					}
					res.reject(cause);
				}

				@Override
				public void onData(byte[] bytes) throws Exception {
					out.write(ByteBuffer.wrap(bytes));
				}

				@Override
				public void onClose() throws Exception {
					if (closeChannel) {
						out.close();
					}
					res.resolve();
				}

			});
		});
	}

	// --- "TRANSFER TO" FILE METHODS ---

	public Promise transferTo(File out) {
		try {
			return transferTo(new FileOutputStream(out));
		} catch (Exception cause) {
			return Promise.reject(cause);
		}
	}

	// --- GETTERS / SETTERS ---

	/**
	 * The isPaused() method returns the current operating state of the
	 * PacketStream.
	 * 
	 * @return
	 */
	public boolean isPaused() {
		return paused.get();
	}

	public boolean isEmpty() {
		return queue.isEmpty();
	}

	public boolean isClosed() {
		return closed.get();
	}

	public boolean isRejected() {
		return error.get() != null;
	}

	public boolean isCompleted() {
		return isClosed() && !isRejected();
	}

	public int size() {
		return queue.size();
	}

	public ScheduledExecutorService getScheduler() {
		return scheduler;
	}

	public void setScheduler(ScheduledExecutorService scheduler) {
		this.scheduler = scheduler;
	}

}