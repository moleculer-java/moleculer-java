package services.moleculer.cachers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.eventbus.GlobMatcher;
import services.moleculer.services.Name;

/**
 * On-heap memory cache. MemoryCacher is the fastest cache implementation in
 * Moleculer. This is a distributed cache, the content of the cache is
 * synchronized between Moleculer nodes via events. Configuration properties:
 * <ul>
 * <li>capacity: Maximum capacity per partition (must be a power of 2), defaults
 * to 2048
 * <li>ttl: Expire time of entries in memory, in seconds (default: 0 = never
 * expires)
 * <li>cleanup: Cleanup period, in seconds
 * </ul>
 * Performance (small and large data): 5.5 million gets / second
 */
@Name("On-heap Memory Cacher")
public final class MemoryCacher extends Cacher implements Runnable {

	// --- PROPERTIES ---

	/**
	 * Maximum number of entries per partition
	 */
	private int capacity;

	/**
	 * Expire time, in seconds (0 = never expires)
	 */
	private int ttl;

	// --- LOCKS ---

	private final Lock readerLock;
	private final Lock writerLock;

	// --- PARTITIONS / CACHE REGIONS ---

	private final HashMap<String, MemoryPartition> partitions = new HashMap<>();

	// --- CONSTUCTORS ---

	public MemoryCacher() {
		this(2048, 0);
	}

	public MemoryCacher(int capacity, int ttl) {

		// Check variables
		if (capacity < 16) {
			capacity = 16;
		}

		// Set properties
		this.capacity = capacity;
		this.ttl = ttl;

		// Init locks
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
		readerLock = lock.readLock();
		writerLock = lock.writeLock();
	}

	// --- START CACHER ---

	/**
	 * Cancelable timer
	 */
	private volatile ScheduledFuture<?> timer;

	/**
	 * Initializes cacher instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public final void start(ServiceBroker broker, Tree config) throws Exception {

		// Process config
		capacity = config.get("capacity", capacity);
		ttl = config.get("ttl", ttl);

		// Start timer
		if (ttl > 0) {
			int cleanup = Math.max(5, ttl / 2);
			timer = broker.components().scheduler().scheduleWithFixedDelay(this, cleanup, cleanup, TimeUnit.SECONDS);
			logger.info("Entries in cache expire after " + ttl + " seconds.");
		}
		logger.info("Maximum number of cached entries is " + capacity + " per partition.");
	}

	// --- REMOVE OLD ENTRIES ---

	@Override
	public final void run() {
		long limit = System.currentTimeMillis() - (1000L * ttl);
		readerLock.lock();
		try {
			for (MemoryPartition partition : partitions.values()) {
				partition.removeOldEntries(limit);
			}
		} finally {
			readerLock.unlock();
		}
	}

	// --- STOP CACHER ---

	@Override
	public final void stop() {

		// Stop timer
		if (timer != null) {
			timer.cancel(false);
			timer = null;
		}

		// Clear partitions
		writerLock.lock();
		try {
			partitions.clear();
		} finally {
			writerLock.unlock();
		}
	}

	// --- CACHE METHODS ---

	@Override
	public final Promise get(String key) {
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
			logger.warn("Unable to get data from cache!", cause);
		}
		return null;
	}

	@Override
	public final void set(String key, Tree value) {
		int pos = partitionPosition(key, true);
		String prefix = key.substring(0, pos);
		MemoryPartition partition;
		writerLock.lock();
		try {
			partition = partitions.get(prefix);
			if (partition == null) {
				partition = new MemoryPartition(capacity);
				partitions.put(prefix, partition);
			}
		} finally {
			writerLock.unlock();
		}
		partition.set(key.substring(pos + 1), value);
	}

	@Override
	public final void del(String key) {
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
	public final void clean(String match) {
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

		private final LinkedHashMap<String, PartitionEntry> cache;

		// --- CONSTUCTORS ---

		private MemoryPartition(int capacity) {

			// Create lockers
			ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
			readerLock = lock.readLock();
			writerLock = lock.writeLock();

			// Create cache partition
			cache = new LinkedHashMap<String, PartitionEntry>(capacity + 1, 1.0f, true) {

				private static final long serialVersionUID = 5994447707758047152L;

				protected final boolean removeEldestEntry(Map.Entry<String, PartitionEntry> entry) {
					if (size() > capacity) {
						return true;
					}
					return false;
				};
			};
		}

		// --- REMOVE OLD ENTRIES ---

		private final void removeOldEntries(long limit) {
			writerLock.lock();
			try {
				Iterator<Map.Entry<String, PartitionEntry>> i = cache.entrySet().iterator();
				Map.Entry<String, PartitionEntry> mEntry;
				PartitionEntry pEntry;
				while (i.hasNext()) {
					mEntry = i.next();
					pEntry = mEntry.getValue();
					if (pEntry.timestamp <= limit) {
						i.remove();
					}
				}
			} finally {
				writerLock.unlock();
			}
		}

		// --- CACHE METHODS ---

		private final Tree get(String key) throws Exception {
			PartitionEntry entry;
			readerLock.lock();
			try {
				entry = cache.get(key);
			} finally {
				readerLock.unlock();
			}
			if (entry == null) {
				return null;
			}
			return entry.value;
		}

		private final void set(String key, Tree value) {
			writerLock.lock();
			try {
				if (value == null) {
					cache.remove(key);
				} else {
					cache.put(key, new PartitionEntry(value));
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

	// --- PARTITION ENTRY ---
	
	private static final class PartitionEntry {

		private final Tree value;
		private final long timestamp = System.currentTimeMillis();

		private PartitionEntry(Tree value) {
			this.value = value;
		}

	}

	// --- GETTERS / SETTERS ---
	
	public final int getCapacity() {
		return capacity;
	}

	public final void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public final int getTtl() {
		return ttl;
	}

	public final void setTtl(int ttl) {
		this.ttl = ttl;
	}
	
}