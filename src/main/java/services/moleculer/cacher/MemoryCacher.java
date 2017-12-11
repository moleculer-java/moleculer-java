/**
 * This software is licensed under MIT license.<br>
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
package services.moleculer.cacher;

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
import services.moleculer.service.Name;

/**
 * On-heap memory cache. MemoryCacher is the fastest cache implementation in
 * Moleculer. This can be a "distributed" cache, the content of the other node's
 * cache is removable via events. Configuration properties:
 * <ul>
 * <li>capacity: Maximum capacity per partition (must be a power of 2), defaults
 * to 2048
 * <li>ttl: Expire time of entries in memory, in seconds (default: 0 = never
 * expires)
 * <li>cleanup: Cleanup period, in seconds
 * </ul>
 * Performance (small and large data): 5.5 million gets / second
 * 
 * @see OHCacher
 * @see RedisCacher
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

	/**
	 * Cleanup period time, in seconds (0 = disable cleanup process)
	 */
	private int cleanup = 5;

	// --- LOCKS ---

	private final Lock readLock;
	private final Lock writeLock;

	// --- PARTITIONS / CACHE REGIONS ---

	private final HashMap<String, MemoryPartition> partitions = new HashMap<>();

	// --- CONSTUCTORS ---

	public MemoryCacher() {
		this(2048, 0, 0);
	}

	public MemoryCacher(int capacity, int ttl) {
		this(capacity, ttl, ttl > 0 ? 5 : 0);
	}

	public MemoryCacher(int capacity, int ttl, int cleanup) {

		// Check variables
		if (capacity < 16) {
			capacity = 16;
		}

		// Set properties
		this.capacity = capacity;
		this.ttl = ttl;
		this.cleanup = cleanup;

		// Init locks
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
		readLock = lock.readLock();
		writeLock = lock.writeLock();
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
		cleanup = config.get("cleanup", cleanup);

		// Start timer
		if (cleanup > 0) {
			timer = broker.components().scheduler().scheduleWithFixedDelay(this, cleanup, cleanup, TimeUnit.SECONDS);
		}
		if (ttl > 0) {
			logger.info("Entries in cache expire after " + ttl + " seconds.");
		}
		logger.info("Maximum number of cached entries is " + capacity + " per partition.");
	}

	// --- REMOVE OLD ENTRIES ---

	@Override
	public final void run() {
		long now = System.currentTimeMillis();
		readLock.lock();
		try {
			for (MemoryPartition partition : partitions.values()) {
				partition.removeOldEntries(now);
			}
		} finally {
			readLock.unlock();
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
		writeLock.lock();
		try {
			partitions.clear();
		} finally {
			writeLock.unlock();
		}
	}

	// --- CACHE METHODS ---

	@Override
	public final Promise get(String key) {
		try {
			int pos = partitionPosition(key, true);
			String prefix = key.substring(0, pos);
			MemoryPartition partition;
			readLock.lock();
			try {
				partition = partitions.get(prefix);
			} finally {
				readLock.unlock();
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
	public final void set(String key, Tree value, int ttl) {
		int pos = partitionPosition(key, true);
		String prefix = key.substring(0, pos);
		MemoryPartition partition;
		writeLock.lock();
		try {
			partition = partitions.get(prefix);
			if (partition == null) {
				partition = new MemoryPartition(capacity);
				partitions.put(prefix, partition);
			}
		} finally {
			writeLock.unlock();
		}
		int entryTTL;
		if (ttl > 0) {

			// Entry-level TTL (in seconds)
			entryTTL = ttl;
		} else {

			// Use the default TTL
			entryTTL = this.ttl;
		}
		partition.set(key.substring(pos + 1), value, entryTTL);
	}

	@Override
	public final void del(String key) {
		int pos = partitionPosition(key, true);
		String prefix = key.substring(0, pos);
		MemoryPartition partition;
		readLock.lock();
		try {
			partition = partitions.get(prefix);
		} finally {
			readLock.unlock();
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
			readLock.lock();
			try {
				partition = partitions.get(prefix);
			} finally {
				readLock.unlock();
			}
			if (partition != null) {
				partition.clean(match.substring(pos + 1));
			}

		} else {

			// Remove entire partitions
			writeLock.lock();
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
				writeLock.unlock();
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

		private final void removeOldEntries(long now) {
			writerLock.lock();
			try {
				Iterator<Map.Entry<String, PartitionEntry>> i = cache.entrySet().iterator();
				Map.Entry<String, PartitionEntry> mEntry;
				PartitionEntry pEntry;
				while (i.hasNext()) {
					mEntry = i.next();
					pEntry = mEntry.getValue();
					if (pEntry.expireAt > 0 && pEntry.expireAt <= now) {
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

		private final void set(String key, Tree value, int ttl) {
			writerLock.lock();
			try {
				if (value == null) {
					cache.remove(key);
				} else {
					long expireAt;
					if (ttl > 0) {
						expireAt = ttl * 1000L + System.currentTimeMillis();
					} else {
						expireAt = 0;
					}
					cache.put(key, new PartitionEntry(value, expireAt));
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
		private final long expireAt;

		private PartitionEntry(Tree value, long expireAt) {
			this.value = value;
			this.expireAt = expireAt;
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

	public final int getCleanup() {
		return cleanup;
	}

	public final void setCleanup(int cleanup) {
		this.cleanup = cleanup;
	}

}