package services.moleculer.cachers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import services.moleculer.utils.GlobMatcher;

public class MemoryCacher extends Cacher {

	// --- PROPERTIES ---

	private final int initialCapacityPerPartition;
	private final int maximumCapacityPerPartition;

	// --- LOCKS ---

	private final Lock readerLock;
	private final Lock writerLock;

	// --- PARTITIONS / CACHE REGIONS ---

	private final HashMap<String, MemoryPartition> partitions = new HashMap<>();

	// --- CONSTUCTORS ---

	public MemoryCacher() {
		this(512, 2048);
	}

	public MemoryCacher(int initialCapacityPerPartition, int maximumCapacityPerPartition) {
		super(false);

		// Check variables
		if (initialCapacityPerPartition < 1) {
			throw new IllegalArgumentException(
					"Zero or negative initialCapacityPerPartition property (" + initialCapacityPerPartition + ")!");
		}
		if (maximumCapacityPerPartition < 1) {
			throw new IllegalArgumentException(
					"Zero or negative maximumCapacityPerPartition property (" + maximumCapacityPerPartition + ")!");
		}
		if (initialCapacityPerPartition > maximumCapacityPerPartition) {
			int tmp = initialCapacityPerPartition;
			initialCapacityPerPartition = maximumCapacityPerPartition;
			maximumCapacityPerPartition = tmp;
		}

		// Set properties
		this.initialCapacityPerPartition = initialCapacityPerPartition;
		this.maximumCapacityPerPartition = maximumCapacityPerPartition;

		// Init locks
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
		int pos = partitionPosition(key, true);
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

	@Override
	public void set(String key, Object value) {
		int pos = partitionPosition(key, true);
		String prefix = key.substring(0, pos);
		MemoryPartition partition;
		writerLock.lock();
		try {
			partition = partitions.get(prefix);
			if (partition == null) {
				partition = new MemoryPartition(initialCapacityPerPartition, maximumCapacityPerPartition);
				partitions.put(prefix, partition);
			}
		} finally {
			writerLock.unlock();
		}
		partition.set(key.substring(pos + 1), value);
	}

	@Override
	public void del(String key) {
		int pos = partitionPosition(key, true);
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

	@Override
	public void clean(String match) {
		int pos = partitionPosition(match, false);
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

	private static final int partitionPosition(String key, boolean throwErrorIfMissing) {
		int i = key.indexOf(':');
		if (i == -1) {
			i = key.lastIndexOf('.');
		} else {
			i = key.lastIndexOf('.', i);
		}
		if (i == -1 && throwErrorIfMissing) {
			throw new IllegalArgumentException("Invalid cache key, a point is missing from the key (" + key + ")!");
		}
		return i;
	}

}