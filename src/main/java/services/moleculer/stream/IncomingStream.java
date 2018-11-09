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

import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import io.datatree.Tree;
import services.moleculer.error.MoleculerError;
import services.moleculer.error.MoleculerErrorUtils;

public class IncomingStream {

	// --- PROPERTIES ---

	protected final String nodeID;

	protected final PacketStream stream;

	protected volatile long lastUsed = System.currentTimeMillis();

	protected volatile long prevSeq = -1;

	protected final HashMap<Long, Tree> pool = new HashMap<>();

	protected AtomicBoolean inited = new AtomicBoolean();

	// --- CONSTRUCTOR ---

	public IncomingStream(String nodeID, ScheduledExecutorService scheduler) {
		this.nodeID = nodeID;
		this.stream = new PacketStream(nodeID, scheduler);
	}

	// --- RESET ---

	/**
	 * Used for testing. Resets internal variables.
	 */
	public synchronized void reset() {
		lastUsed = System.currentTimeMillis();
		prevSeq = -1;
		pool.clear();
		stream.closed.set(false);
		stream.buffer.clear();
		stream.cause = null;
		stream.transferedBytes.set(0);
		inited.set(false);
	}

	// --- ERROR ---

	public void error(Throwable cause) {
		stream.sendError(cause);
	}

	// --- INIT ---

	public boolean inited() {
		return !inited.compareAndSet(false, true);
	}

	// --- RECEIVE PACKET ---

	public synchronized boolean receive(Tree message) {

		// Update timestamp
		lastUsed = System.currentTimeMillis();

		// Check sequence number
		long seq = message.get("seq", -1);
		if (seq > -1) {
			if (seq - 1 == prevSeq) {
				prevSeq = seq;
			} else {

				// Process later
				pool.put(seq, message);
				return false;
			}
		} else {
			prevSeq = -1;
		}

		// Process current message
		boolean close = processMessage(message);

		// Process pooled messages
		long nextSeq = prevSeq;
		while (true) {
			Tree nextMessage = pool.remove(++nextSeq);
			if (nextMessage == null) {
				break;
			}
			prevSeq = nextSeq;
			if (processMessage(nextMessage)) {
				close = true;
			}
		}

		// True = remove stream from registry
		return close;
	}

	protected boolean processMessage(Tree message) {

		// Stream closed
		if (stream.isClosed()) {
			return true;
		}

		// Create processing variables
		byte[] bytes = null;
		Throwable cause = null;
		boolean close = false;

		// Parse incoming message
		try {
			boolean success = message.get("success", true);
			if (success) {
				Tree params = message.get("params");
				if (params != null) {
					Tree data = params.get("data");
					if (data != null && data.isEnumeration()) {
						bytes = new byte[data.size()];
						int idx = 0;
						for (Tree item : data) {
							bytes[idx++] = (byte) item.asInteger().intValue();
						}
					}
				}
				close = !message.get("stream", false);
			} else {
				Tree error = message.get("error");
				if (error == null) {
					cause = new MoleculerError("Remote invocation failed!", null, "MoleculerError", nodeID, false, 500,
							"UNKNOWN_ERROR", message);
				} else {
					cause = MoleculerErrorUtils.create(error);
				}
				close = true;
			}
		} catch (Throwable error) {
			cause = error;
		}

		// Bytes
		if (bytes != null) {
			stream.sendData(bytes);
		}

		// Error
		if (cause != null) {
			stream.sendError(cause);
			return true;
		}

		// Close
		if (close) {
			stream.sendClose();
			return true;
		}

		// Do not remove from stream registry
		return false;
	}

	// --- PROPERTY GETTERS ---

	public long getLastUsed() {
		return lastUsed;
	}

	public PacketStream getPacketStream() {
		return stream;
	}

}