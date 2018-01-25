/**
 * MOLECULER MICROSERVICES FRAMEWORK<br>
 * <br>
 * This project is based on the idea of Moleculer Microservices
 * Framework for NodeJS (https://moleculer.services). Special thanks to
 * the Moleculer's project owner (https://github.com/icebob) for the
 * consultations.<br>
 * <br>
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
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
package services.moleculer.transporter.tcp;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.datatree.Tree;

public class TcpEndpoint {

	// --- PROPERTIES ---

	protected final String nodeID;
	protected final String host;
	protected final int port;
	protected final int hashCode;

	// --- STATUS ---

	/**
	 * Timestamp
	 */
	protected final AtomicLong when = new AtomicLong();

	/**
	 * CPU usage at "when" (-1 = offline)
	 */
	protected final AtomicInteger cpu = new AtomicInteger();

	// --- CONSTRUCTOR ---

	public TcpEndpoint(String nodeID, String host, int port) {

		// Save properties
		this.nodeID = nodeID;
		this.host = host;
		this.port = port;
		this.hashCode = nodeID.hashCode();

		// Set time
		when.set(System.currentTimeMillis());
	}

	// --- IS ONLINE ---
	
	public boolean isOnline() {
		return cpu.get() >  -1;
	}
	
	// --- MARK AS OFFLINE ---

	public void markAsOffline() {
		long update = System.currentTimeMillis();
		long current = when.get();
		if (update > current) {
			if (when.compareAndSet(current, update)) {
				cpu.set(-1);
			}
		}
	}

	// --- WRITE STATUS ---

	public void writeStatus(Tree target) {
		target.put("nodeID", nodeID);
		target.put("host", host);
		target.put("port", port);
		target.put("cpu", cpu.get());
		target.put("when", when.get());
	}

	// --- READ STATUS ---

	public void readStatus(Tree source) {
		long update = source.get("when", 0L);
		long current = when.get();
		if (update > current) {
			if (when.compareAndSet(current, update)) {
				cpu.set(source.get("cpu", 0));
			}
		}
	}

	// --- COLLECTION HELPERS ---

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TcpEndpoint other = (TcpEndpoint) obj;
		if (hashCode != other.hashCode) {
			return false;
		}
		return nodeID.equals(other.nodeID);
	}

}