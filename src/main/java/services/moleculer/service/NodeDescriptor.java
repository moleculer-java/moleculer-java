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
package services.moleculer.service;

import static services.moleculer.util.CommonUtils.getHostOrIP;

import java.util.concurrent.atomic.AtomicReference;

import io.datatree.Tree;

public class NodeDescriptor {

	// --- FINAL NODE PROPERTIES ---

	public final String nodeID;
	public final Tree info;
	public final String host;
	public final int port;
	public final boolean local;

	protected final boolean preferHostname;
	
	// --- CHANGING PROPERTIES ---

	protected final AtomicReference<NodeStatus> status = new AtomicReference<>();

	protected final AtomicReference<CpuUsage> cpu = new AtomicReference<>(new CpuUsage(0, 0));

	// --- CONSTRUCTORS ---

	public NodeDescriptor(Tree info, boolean preferHostname, boolean local) {
		this(info, preferHostname, local, null, false);
	}

	public NodeDescriptor(Tree info, boolean preferHostname, boolean local, CpuUsage usage) {
		this(info, preferHostname, local, usage, false);
	}
	
	public NodeDescriptor(Tree info, boolean preferHostname, boolean local, CpuUsage usage, boolean offline) {
		this.nodeID = info.get("sender", "unknown");
		this.info = info;
		this.host = getHostOrIP(preferHostname, info);
		this.port = info.get("port", 0);
		this.local = local;
		this.preferHostname = preferHostname;
		this.status.set(new NodeStatus(info.get("seq", 0L), offline ? System.currentTimeMillis() : 0));
		this.cpu.set(usage == null ? new CpuUsage(0, 0) : usage);
	}

	public NodeDescriptor(String nodeID, String host, int port) {
		this.nodeID = nodeID;
		this.info = null;
		this.host = host;
		this.port = port;
		this.local = false;
		this.preferHostname = true;
		this.status.set(new NodeStatus(0, System.currentTimeMillis()));
	}

	// --- STATUS ---
	
	public NodeStatus getStatus() {
		return status.get();
	}
	
	// --- ONLINE ---
	
	public boolean isOnline() {
		return info != null && status.get().offlineSince == 0;
	}
	
	public NodeDescriptor switchToOnline(Tree info) {
		return switchToOnline(info, 0, 0);
	}
	
	public NodeDescriptor switchToOnline(Tree info, long sequence, int value) {
		NodeStatus current = status.get();
		if (current.offlineSince == 0 || current.sequence >= info.get("seq", 0L)) {
			return null;
		}
		CpuUsage usage = sequence == 0 ? cpu.get() : new CpuUsage(sequence, value);
		return new NodeDescriptor(info, this.preferHostname, false, usage);
	}
	
	// --- OFFLINE ---
	
	public boolean isOffline() {
		return info == null || status.get().offlineSince > 0;
	}

	public long getOfflineSince() {
		return status.get().offlineSince;
	}

	public long getOfflineSince(long defaultValue) {
		long offlineSince = status.get().offlineSince;
		if (offlineSince == 0) {
			return defaultValue;
		}
		return offlineSince;
	}
	
	public boolean switchToOffline() {
		NodeStatus next, current = status.get();
		long now = System.currentTimeMillis();
		while (true) {
			if (current.offlineSince > 0) {
				return false;
			}
			next = new NodeStatus(current.sequence + 1, now);
			if (status.compareAndSet(current, next)) {
				return true;
			}
			current = status.get();
		}
	}
	
	public boolean switchToOffline(long sequence) {
		NodeStatus next, current = status.get();
		long offlineSince, now = System.currentTimeMillis();
		while (true) {
			if (current.sequence >= sequence) {
				return false;
			}
			offlineSince = current.offlineSince;
			if (offlineSince == 0) {
				offlineSince = now;
			}
			next = new NodeStatus(sequence, offlineSince);
			if (status.compareAndSet(current, next)) {
				return true;
			}
			current = status.get();
		}		
	}

	// --- GET CURRENT SEQUENCE ---

	public long getSequence() {
		return status.get().sequence;
	}
	
	// --- GET CPU USAGE ---

	public CpuUsage getCpuUsage() {
		return cpu.get();
	}
	
	// --- GET HEARTBEAT TIMESTAMP ---
	
	public long getLastHeartbeatTime() {
		return cpu.get().when;
	}

	public long getLastHeartbeatTime(long defaultValue) {
		long when = cpu.get().when;
		if (when == 0) {
			return defaultValue;
		}
		return when;
	}

	// --- UPDATE CPU USAGE ---

	public void setCpuUsage(long sequence, int value) {
		CpuUsage next, current = cpu.get();
		while (true) {
			if (current.sequence >= sequence) {
				return;
			}
			next = new CpuUsage(sequence, value);
			if (cpu.compareAndSet(current, next)) {
				return;
			}
			current = cpu.get();
		}
	}

	public void setCpuUsage(int value) {
		CpuUsage next, current = cpu.get();
		while (true) {
			if (current.value == value) {
				return;
			}
			next = new CpuUsage(current.sequence + 1, value);
			if (cpu.compareAndSet(current, next)) {
				return;
			}
			current = cpu.get();
		}
	}

}