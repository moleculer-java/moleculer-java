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

import java.util.concurrent.atomic.AtomicReference;

import io.datatree.Tree;

public class NodeDescriptor {

	// --- NODE INFO ---

	public final String nodeID;
	public final boolean useHostname;
	public final Tree info;
	public final String host;
	public final int port;
	public final long when;
	public final boolean local;

	// --- OFFLINE STATUS ---

	protected final AtomicReference<OfflineSnapshot> offline = new AtomicReference<>();

	// --- CPU STATUS ---

	protected final AtomicReference<CpuSnapshot> cpu = new AtomicReference<>();

	// --- STATIC CONSTRUCTORS ---

	public static NodeDescriptor offline(String nodeID, boolean useHostname, String host, int port) {
		NodeDescriptor node = byHost(nodeID, useHostname, host, port, null);
		node.offline.set(new OfflineSnapshot());
		return node;
	}
	
	public static NodeDescriptor offline(String nodeID, boolean useHostname, Tree info) {
		NodeDescriptor node = byInfo(nodeID, useHostname, info, null);
		node.offline.set(new OfflineSnapshot());
		return node;
	}
	
	public static NodeDescriptor byHost(String nodeID, boolean useHostname, String host, int port) {
		return byHost(nodeID, useHostname, host, port, null);
	}

	public static NodeDescriptor byHost(String nodeID, boolean useHostname, String host, int port, CpuSnapshot snapshot) {
		return new NodeDescriptor(nodeID, useHostname, null, false, host, port, snapshot);
	}

	public static NodeDescriptor byInfo(String nodeID, boolean useHostname, Tree info) {
		return byInfo(nodeID, useHostname, info, null);
	}
	
	public static NodeDescriptor byInfo(String nodeID, boolean useHostname, Tree info, CpuSnapshot snapshot) {
		return new NodeDescriptor(nodeID, useHostname, info, false, null, 0, snapshot);
	}

	public static NodeDescriptor local(String nodeID, boolean useHostname, Tree info) {
		return local(nodeID, useHostname, info, null);
	}

	public static NodeDescriptor local(String nodeID, boolean useHostname, Tree info, CpuSnapshot snapshot) {
		return new NodeDescriptor(nodeID, useHostname, info, true, null, 0, snapshot);
	}
	
	// --- CONSTRUCTOR ---

	protected NodeDescriptor(String nodeID, boolean useHostname, Tree info, boolean local, String host, int port, CpuSnapshot snapshot) {

		// Set node info
		this.nodeID = nodeID;
		this.useHostname = useHostname;
		this.info = info;
		this.local = local;
		if (info == null) {
			this.host = host;
			this.port = port;
			this.when = 0;
		} else {
			String hostOrIP = null;
			if (useHostname) {
				hostOrIP = getHost(info);
				if (hostOrIP == null) {
					hostOrIP = getIP(info);
				}
			} else {
				hostOrIP = getIP(info);
				if (hostOrIP == null) {
					hostOrIP = getHost(info);
				}
			}
			this.host = hostOrIP;
			this.port = info.get("port", 0);
			this.when = info.get("when", 0L);
		}

		// Store CPU data
		cpu.set(snapshot);
	}

	protected String getHost(Tree info) {
		String host = info.get("hostname", (String) null);
		if (host != null && !host.isEmpty()) {
			return host;
		}
		return null;
	}

	protected String getIP(Tree info) {
		Tree ipList = info.get("ipList");
		if (ipList != null && ipList.size() > 0) {
			String ip = ipList.get(0).asString();
			if (ip != null && !ip.isEmpty()) {
				return ip;
			}
		}
		return null;
	}

	// --- SWITCH TO OFFLINE (DISCONNECTED / NETWORK ERROR) ---

	public boolean switchToOffline() {
		OfflineSnapshot o = offline.get();
		long now = System.currentTimeMillis();
		if (o == null) {
			return offline.compareAndSet(null, new OfflineSnapshot(now, now));
		}
		return offline.compareAndSet(o, new OfflineSnapshot(now, o.since));
	}

	// --- SWITCH TO OFFLINE (NODE IS OFFLINE IN A GOSSIP MESSAGE) ---

	public boolean switchToOffline(long when, long since) {
		if (this.when > when) {
			return false;
		}
		OfflineSnapshot o = offline.get();
		if (o == null) {
			return offline.compareAndSet(null, new OfflineSnapshot(when, since));
		}
		if (o.when >= when) {
			return false;
		}
		if (o.since > since) {
			offline.compareAndSet(o, new OfflineSnapshot(o.when, since));
		}
		return false;
	}

	// --- SWITCH TO ONLINE ---

	public NodeDescriptor switchToOnline(Tree info) {
		if (info.get("when", 0L) > when || offline.get() != null) {
			return new NodeDescriptor(nodeID, useHostname, info, local, host, port, cpu.get());
		}
		return null;
	}

	// --- SET CPU USAGE ---

	public boolean updateCpu(long when, int value) {
		CpuSnapshot c = cpu.get();
		if (c == null || c.when < when) {
			return cpu.compareAndSet(c, new CpuSnapshot(when, value));
		}
		return false;
	}

	// --- GET OFFLINE SNAPSHOT ---

	public OfflineSnapshot getOfflineSnapshot() {
		return offline.get();
	}

	// --- GET CPU SNAPSHOT ---

	public CpuSnapshot getCpuSnapshot() {
		return cpu.get();
	}

	// --- GET CPU USAGE ---

	public int getCpuUsage(int defaultValue) {
		CpuSnapshot c = cpu.get();
		return c == null ? defaultValue : c.value;
	}

	// --- GET OFFLINE SINCE TIMESTAMP ---

	public long getOfflineSince(long defaultValue) {
		OfflineSnapshot o = offline.get();
		return o == null ? defaultValue : o.since;
	}

	// --- GET HEARTBEAT TIMESTAMP ---

	public long getHeartbeatWhen(long defaultValue) {
		CpuSnapshot c = cpu.get();
		return c == null ? defaultValue : c.when;
	}

	// --- IS ONLINE ---

	public boolean isOnline() {
		return offline.get() == null;
	}

	public boolean isOffline() {
		return offline.get() != null;
	}

}