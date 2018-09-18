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

import io.datatree.Tree;
import services.moleculer.error.MoleculerError;
import services.moleculer.error.MoleculerErrorFactory;

public class IncomingStream {

	// --- PROPERTIES ---

	protected final String nodeID;
	
	protected final PacketStream stream;

	protected volatile long lastUsed = System.currentTimeMillis();

	protected volatile long lastSeq;

	protected final HashMap<Long, Tree> pool = new HashMap<>();
	
	// --- CONSTRUCTOR ---

	public IncomingStream(String nodeID, ScheduledExecutorService scheduler) {
		this.nodeID = nodeID;
		this.stream = new PacketStream(scheduler);
	}

	// --- RECEIVE PACKET ---

	public synchronized boolean receive(Tree message) {

		// Update timestamp
		lastUsed = System.currentTimeMillis();

		// Check sequence number
		long seq = 0;
		Tree meta = message.get("meta");
		if (meta != null) {
			seq = meta.get("seq", 0L);
		}
		if (seq > 0) {
			if (seq - 1 == lastSeq) {				
				lastSeq = seq;
			} else {
				
				// Process later
				pool.put(seq, message);
				return false;
			}
		} else {
			lastSeq = 0;
		}
		
		// Process current message
		processMessage(message);

		// Process pooled messages
		boolean close = false;
		long nextSeq = lastSeq;
		while (true) {
			Tree nextMessage = pool.remove(++nextSeq);
			if (nextMessage == null) {
				break;
			}
			if (processMessage(nextMessage)) {
				close = true;
			}
		}
		
		// True = remove stream from registry
		return close;
	}

	protected boolean processMessage(Tree message) {

		// Create processing variables
		byte[] bytes = null;
		Throwable cause = null;
		boolean close = false;

		// Parse incoming message
		try {
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
			if (bytes == null) {
				boolean success = message.get("success", true);
				if (!success) {
					Tree error = message.get("error");
					if (error == null) {
						cause = new MoleculerError("Remote invocation failed!", null, "MoleculerError", nodeID,
								false, 500, "UNKNOWN_ERROR", message);
					} else {
						cause = MoleculerErrorFactory.create(error);
					}
				}
			}
			close = !message.get("stream", false);
		} catch (Throwable error) {
			cause = error;
		}

		// Bytes
		if (bytes != null) {
			try {
				stream.sendData(bytes);
			} catch (Throwable error) {
				cause = error;
			}
		}

		// Error
		if (cause != null) {
			try {
				stream.sendError(cause);
			} catch (Throwable ignored) {
			}
			return true;
		}

		// Close
		if (close) {
			try {
				stream.sendClose();
			} catch (Throwable ignored) {
			}
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