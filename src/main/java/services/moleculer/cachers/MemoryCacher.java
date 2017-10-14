package services.moleculer.cachers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.eventbus.GlobMatcher;
import services.moleculer.services.Name;

@Name("On-heap Memory Cacher")
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
	public void stop() {
		partitions.clear();
	}

	// --- CACHE METHODS ---

	@Override
	public Promise get(String key) {
		try {
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
			Tree value = partition.get(key.substring(pos + 1));
			if (value == null) {
				return null;
			}
			return Promise.resolve(value);
		} catch (Throwable cause) {
			logger.warn("Unable to get data from MemoryCacher!", cause);
		}
		return null;
	}

	@Override
	public void set(String key, Tree value) {
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

	// --- MEMORY PARTITION ---

	private static final class MemoryPartition {

		// --- LOCKS ---

		private final Lock readerLock;
		private final Lock writerLock;

		// --- MEMORY CACHE PARTITION ---

		private final LinkedHashMap<String, Tree> cache;

		// --- CONSTUCTORS ---

		private MemoryPartition(int initialCapacityPerPartition, int maximumCapacityPerPartition) {
			ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
			readerLock = lock.readLock();
			writerLock = lock.writeLock();
			cache = new LinkedHashMap<String, Tree>(initialCapacityPerPartition, 1.0f, true) {

				private static final long serialVersionUID = 5994447707758047152L;

				protected final boolean removeEldestEntry(Map.Entry<String, Tree> entry) {
					if (this.size() > maximumCapacityPerPartition) {
						return true;
					}
					return false;
				};
			};
		}

		// --- CACHE METHODS ---

		private final Tree get(String key) throws Exception {
			Tree value;
			readerLock.lock();
			try {
				value = cache.get(key);
			} finally {
				readerLock.unlock();
			}
			return value;
		}

		private final void set(String key, Tree value) {
			writerLock.lock();
			try {
				if (value == null) {
					cache.remove(key);
				} else {
					cache.put(key, value);
				}
			} finally {
				writerLock.unlock();
			}
		}

		private final void del(String key) {
			writerLock.lock();
			try {
				cache.remove(key);
			} finally {
				writerLock.unlock();
			}
		}

		private final void clean(String match) {
			writerLock.lock();
			try {
				if (match.isEmpty() || match.startsWith("*")) {
					cache.clear();
				} else if (match.indexOf('*') == -1) {
					cache.remove(match);
				} else {
					Iterator<String> i = cache.keySet().iterator();
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