package services.moleculer.transporter.tcp;

import static services.moleculer.util.CommonUtils.getHostOrIP;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Tree;

public class NodeDescriptor {

	// --- FINAL PROPERTIES ----

	public final String nodeID;
	public final boolean local;

	protected final boolean preferHostname;

	// --- NON-FINAL PROPERTIES ----

	public volatile String host = "";
	public volatile int port;

	public volatile Tree info = new Tree();
	public volatile long seq;
	public volatile long offlineSince;

	public volatile int cpu;
	public volatile long cpuSeq;
	public volatile long cpuWhen;

	// --- LOCKS ---

	public final Lock readLock;
	public final Lock writeLock;

	// --- CONSTUCTORS ---

	public NodeDescriptor(String nodeID, boolean preferHostname, boolean local) {

		// Init final properties
		this.nodeID = nodeID;
		this.preferHostname = preferHostname;
		this.local = local;

		// Init locks
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
		readLock = lock.readLock();
		writeLock = lock.writeLock();
	}

	public NodeDescriptor(String nodeID, boolean preferHostname, String host, int port) {
		this(nodeID, preferHostname, false);

		// Set non-final properties
		this.host = host;
		this.port = port;
	}

	public NodeDescriptor(String nodeID, boolean preferHostname, boolean local, Tree info) {
		this(nodeID, preferHostname, local);

		// Set non-final properties
		host = getHostOrIP(preferHostname, info);
		port = info.get("port", 0);
		seq = info.get("seq", 0L);
	}

	// --- UPDATE CPU ---

	public void updateCpu(int cpu) {
		if (cpu >= 0 && cpu <= 100) {
			if (this.cpu != cpu) {
				this.cpu = cpu;
				cpuSeq++;
			}
			cpuWhen = System.currentTimeMillis();
		}
	}

	public void updateCpu(long cpuSeq, int cpu) {
		if (cpu >= 0 && cpu <= 100 && cpuSeq > 0 && this.cpuSeq < cpuSeq) {
			this.cpuSeq = cpuSeq;
			this.cpu = cpu;
			cpuWhen = System.currentTimeMillis();
		}
	}

	// --- MARK AS OFFLINE ---

	public boolean markAsOffline() {
		if (offlineSince == 0) {
			offlineSince = System.currentTimeMillis();
			seq++;
			info.put("seq", seq);
			return true;
		}
		return false;
	}

	public boolean markAsOffline(long seq) {
		if (this.seq < seq) {
			this.seq = seq;
			info.put("seq", seq);
			if (offlineSince == 0) {
				offlineSince = System.currentTimeMillis();
				return true;
			}
		}
		return false;
	}

	// --- MARK AS ONLINE ---

	public boolean markAsOnline(Tree info) {
		long seq = info.get("seq", 0L);
		if (this.seq < seq) {
			this.seq = seq;
			this.info = info;
			this.offlineSince = 0;
			this.host = getHostOrIP(preferHostname, info);
			this.port = info.get("port", 0);
		}
		return false;
	}

}