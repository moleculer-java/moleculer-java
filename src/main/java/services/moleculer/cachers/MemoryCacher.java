package services.moleculer.cachers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import services.moleculer.utils.GlobMatcher;

public class MemoryCacher extends Cacher {

	// --- CACHE VARIABLES ---

	private final int capacityPerPartition;

	private final Lock readerLock;
	private final Lock writerLock;

	// --- PARTITIONS / CACHE REGIONS ---

	private final HashMap<String, MemoryPartition> partitions = new HashMap<>();

	// --- CONSTUCTORS ---

	public MemoryCacher() {
		this(DEFAULT_PREFIX, 1024);
	}

	public MemoryCacher(String prefix) {
		this(prefix, 1024);
	}

	public MemoryCacher(String prefix, int capacityPerPartition) {
		super(prefix);
		this.capacityPerPartition = capacityPerPartition;
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
		readerLock = lock.readLock();
		writerLock = lock.writeLock();
	}

	// --- CLOSE CACHE INSTANCE ---

	@Override
	public void close() {
		partitions.clear();
	}

	// --- CACHE METHODS ---

	@Override
	public Object get(String key) {
		int pos = key.indexOf('.');
		if (pos > 0) {
			String prefix = key.substring(0, pos);
			MemoryPartition partition;
			readerLock.lock();
			try {
				partition = partitions.get(prefix);
			} finally {
				readerLock.unlock();
			}
			if (partition == null) {
				return null;
			}
			return partition.get(key.substring(pos + 1));
		}
		return null;
	}

	@Override
	public void set(String key, Object value) {
		int pos = key.indexOf('.');
		if (pos > 0) {
			String prefix = key.substring(0, pos);
			MemoryPartition partition;
			writerLock.lock();
			try {
				partition = partitions.get(prefix);
				if (partition == null) {
					partition = new MemoryPartition(capacityPerPartition);
					partitions.put(prefix, partition);
				}
			} finally {
				writerLock.unlock();
			}
			partition.set(key.substring(pos + 1), value);
		}
	}

	@Override
	public void del(String key) {
		int pos = key.indexOf('.');
		if (pos > 0) {
			String prefix = key.substring(0, pos);
			MemoryPartition partition;
			readerLock.lock();
			try {
				partition = partitions.get(prefix);
			} finally {
				readerLock.unlock();
			}
			if (partition != null) {
				partition.del(key.substring(pos + 1));
			}
		}
	}

	@Override
	public void clean(String match) {
		int pos = match.indexOf('.');
		if (pos > 0) {
			
			// Remove items in partitions
			String prefix = match.substring(0, pos);
			MemoryPartition partition;
			readerLock.lock();
			try {
				partition = partitions.get(prefix);
			} finally {
				readerLock.unlock();
			}
			if (partition != null) {
				partition.clean(match.substring(pos + 1));
			}
			
		} else {
			
			// Remove entire partitions
			writerLock.lock();
			try {
				if (match.isEmpty() || match.startsWith("*")) {
					partitions.clear();
				} else if (match.indexOf('*') == -1) {
					partitions.remove(match);
				} else {
					Iterator<String> i = partitions.keySet().iterator();
					String key;
					while (i.hasNext()) {
						key = i.next();
						if (GlobMatcher.matches(key, match)) {
							i.remove();
						}
					}
				}
			} finally {
				writerLock.unlock();
			}
		}
	}

}