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
package services.moleculer.cacher;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.eventbus.Matcher;
import services.moleculer.service.Name;

/**
 * On-heap memory cache. MemoryCacher is the fastest cache implementation in
 * Moleculer. This can be a "distributed" cache, the content of the other node's
 * cache is removable via events; the developer can send distributed events that
 * erase the cache on the other nodes - but this is not automatic! Supports
 * global and entry-level TTL configuration. Configuration properties:
 * <ul>
 * <li>capacity: Maximum capacity per partition (must be a power of 2), defaults
 * to 2048
 * <li>ttl: Expire time of entries in memory, in seconds (default: 0 = never
 * expires)
 * <li>cleanup: Cleanup period, in seconds
 * </ul>
 * Performance (small and large data): 5.5 million gets / second (per thread /
 * core) <br>
 * <br>
 *
 * @see RedisCacher
 * @see OHCacher
 */
@Name("On-heap Memory Cacher")
public class MemoryCacher extends Cacher implements Runnable {

	// --- PROPERTIES ---

	/**
	 * Maximum number of entries per partition
	 */
	protected int capacity;

	/**
	 * Expire time, in SECONDS (0 = never expires)
	 */
	protected int ttl;

	/**
	 * Cleanup period time, in SECONDS (0 = disable cleanup process)
	 */
	protected int cleanup = 5;

	// --- LOCKS ---

	protected final Lock readLock;
	protected final Lock writeLock;

	// --- PARTITIONS / CACHE REGIONS ---

	protected final HashMap<String, MemoryPartition> partitions = new HashMap<>();

	// --- TIMERS ---

	/**
	 * Cancelable timer
	 */
	protected volatile ScheduledFuture<?> timer;

	// --- CONSTUCTORS ---

	public MemoryCacher() {
		this(2048, 0, 0);
	}

	public MemoryCacher(int capacityPerPartition, int defaultTtl) {
		this(capacityPerPartition, defaultTtl, defaultTtl > 0 ? 5 : 0);
	}

	public MemoryCacher(int capacityPerPartition, int defaultTtl, int cleanupSeconds) {

		// Check variables
		if (capacityPerPartition < 16) {
			capacityPerPartition = 16;
		}

		// Set properties
		this.capacity = capacityPerPartition;
		this.ttl = defaultTtl;
		this.cleanup = cleanupSeconds;

		// Init locks
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
		readLock = lock.readLock();
		writeLock = lock.writeLock();
	}

	// --- START CACHER ---

	/**
	 * Initializes cacher instance.
	 *
	 * @param broker
	 *            parent ServiceBroker
	 */
	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Start timer
		if (cleanup > 0) {
			timer = broker.getConfig().getScheduler().scheduleWithFixedDelay(this, cleanup, cleanup, TimeUnit.SECONDS);
		}
		if (ttl > 0) {
			logger.info("Entries in cache expire after " + ttl + " seconds.");
		}
		logger.info("Maximum number of cached entries is " + capacity + " per partition.");
	}

	// --- REMOVE OLD ENTRIES ---

	@Override
	public void run() {
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
	public void stopped() {

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
	public Promise get(String key) {
		try {
			int pos = partitionPosition(key, true);

			// Prefix is the name of the partition / region (eg.
			// "user" from the "user.name" cache key)
			String prefix = key.substring(0, pos);
			MemoryPartition partition;
			readLock.lock();
			try {
				partition = partitions.get(prefix);
			} finally {
				readLock.unlock();
			}
			if (partition != null) {
				return Promise.resolve(partition.get(key.substring(pos + 1)));
			}
		} catch (Throwable cause) {
			logger.warn("Unable to get data from the cache!", cause);
		}
		return Promise.resolve((Object) null);
	}

	@Override
	public Promise set(String key, Tree value, int ttl) {
		try {
			int pos = partitionPosition(key, true);

			// Prefix is the name of the partition / region (eg.
			// "user" from the "user.name" cache key)
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
			partition.set(key.substring(pos + 1), value.clone(), entryTTL);
		} catch (Throwable cause) {
			logger.warn("Unable to set data to the cache!", cause);
		}
		return Promise.resolve();
	}

	@Override
	public Promise del(String key) {
		try {
			int pos = partitionPosition(key, true);

			// Prefix is the name of the partition / region (eg.
			// "user" from the "user.name" cache key)
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
		} catch (Throwable cause) {
			logger.warn("Unable to delete data from the cache!", cause);
		}
		return Promise.resolve();
	}

	@Override
	public Promise clean(String match) {
		try {

			// Prefix is the name of the partition / region (eg.
			// "user" from the "user.name" cache key)
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

						// Not supported method
						logger.warn("This pattern is not supported: " + match);

					} else {
						Iterator<String> i = partitions.keySet().iterator();
						String key;
						while (i.hasNext()) {
							key = i.next();
							if (Matcher.matches(key, match)) {
								i.remove();
							}
						}
					}
				} finally {
					writeLock.unlock();
				}
			}
		} catch (Throwable cause) {
			logger.warn("Unable to clean cache!", cause);
		}
		return Promise.resolve();
	}

	protected static final int partitionPosition(String key, boolean throwErrorIfMissing) {
		int i = key.indexOf('.');
		if (i == -1 && throwErrorIfMissing) {
			throw new IllegalArgumentException("Invalid cache key, a point is missing from the key (" + key + ")!");
		}
		return i;
	}

	// --- MEMORY PARTITION ---

	protected static class MemoryPartition {

		// --- LOCKS ---

		protected final Lock readerLock;
		protected final Lock writerLock;

		// --- MEMORY CACHE PARTITION ---

		protected final LinkedHashMap<String, PartitionEntry> cache;

		// --- CONSTUCTORS ---

		protected MemoryPartition(int capacity) {

			// Create lockers
			ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
			readerLock = lock.readLock();
			writerLock = lock.writeLock();

			// Create cache partition
			cache = new LinkedHashMap<String, PartitionEntry>(capacity + 1, 1.0f, true) {

				private static final long serialVersionUID = 5994447707758047152L;

				protected final boolean removeEldestEntry(Map.Entry<String, PartitionEntry> entry) {
					return size() > capacity;
				};
			};
		}

		// --- REMOVE OLD ENTRIES ---

		protected void removeOldEntries(long now) {
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

		protected Tree get(String key) throws Exception {
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
			return entry.value.clone();
		}

		protected void set(String key, Tree value, int ttl) {
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

		protected void del(String key) {
			writerLock.lock();
			try {
				cache.remove(key);
			} finally {
				writerLock.unlock();
			}
		}

		protected void clean(String match) {
			writerLock.lock();
			try {
				if (match.isEmpty() || "**".equals(match)) {
					cache.clear();
				} else if (match.indexOf('*') == -1) {
					cache.remove(match);
				} else {
					Iterator<String> i = cache.keySet().iterator();
					String key;
					while (i.hasNext()) {
						key = i.next();
						if (Matcher.matches(key, match)) {
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

	protected static class PartitionEntry {

		protected final Tree value;
		protected final long expireAt;

		protected PartitionEntry(Tree value, long expireAt) {
			this.value = value;
			this.expireAt = expireAt;
		}

	}

	// --- GETTERS / SETTERS ---

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public int getTtl() {
		return ttl;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public int getCleanup() {
		return cleanup;
	}

	public void setCleanup(int cleanup) {
		this.cleanup = cleanup;
	}

}