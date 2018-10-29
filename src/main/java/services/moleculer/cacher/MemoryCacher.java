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
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.error.MoleculerServerError;
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

	/**
	 * Do you need to make a copy of the returned values? Cloning the values is
	 * much safer, but little bit slower. If the services work with common
	 * objects, they can modify the cached object. If you turn off cloning, the
	 * cache will be faster, but you need to be careful NOT TO CHANGE the values
	 * from the cache!
	 */
	protected boolean useCloning = true;

	// --- READ/WRITE LOCK ---

	protected final StampedLock lock = new StampedLock();

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

		// Set capacity
		if (capacityPerPartition < 16) {
			this.capacity = 16;
		} else {
			this.capacity = capacityPerPartition;
		}

		// Set other properties
		this.ttl = defaultTtl;
		this.cleanup = cleanupSeconds;
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
		final long stamp = lock.readLock();
		try {
			for (MemoryPartition partition : partitions.values()) {
				partition.removeOldEntries(now);
			}
		} finally {
			lock.unlockRead(stamp);
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
		final long stamp = lock.writeLock();
		try {
			partitions.clear();
		} finally {
			lock.unlockWrite(stamp);
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
			MemoryPartition partition = getPartition(prefix);
			if (partition != null) {
				return Promise.resolve(partition.get(key.substring(pos + 1)));
			}
		} catch (Throwable cause) {
			logger.warn("Unable to get data from the cache!", cause);
		}
		return Promise.resolve((Object) null);
	}

	protected MemoryPartition getPartition(String prefix) {
		MemoryPartition partition = null;
		long stamp = lock.tryOptimisticRead();
		if (stamp != 0) {
			try {
				partition = partitions.get(prefix);
			} catch (Exception modified) {
				stamp = 0;
			}
		}
		if (!lock.validate(stamp) || stamp == 0) {
			stamp = lock.readLock();
			try {
				partition = partitions.get(prefix);
			} finally {
				lock.unlockRead(stamp);
			}
		}
		return partition;
	}

	@Override
	public Promise set(String key, Tree value, int ttl) {
		try {
			int pos = partitionPosition(key, true);

			// Prefix is the name of the partition / region (eg.
			// "user" from the "user.name" cache key)
			String prefix = key.substring(0, pos);
			MemoryPartition partition = getPartition(prefix);
			if (partition == null) {
				partition = new MemoryPartition(capacity, useCloning);
				final long stamp = lock.writeLock();
				try {
					partitions.put(prefix, partition);
				} finally {
					lock.unlockWrite(stamp);
				}
			}
			int entryTTL;
			if (ttl > 0) {

				// Entry-level TTL (in seconds)
				entryTTL = ttl;
			} else {

				// Use the default TTL
				entryTTL = this.ttl;
			}

			// Create another, cloned instance
			Tree v = useCloning ? value.clone() : value;

			// Store value
			partition.set(key.substring(pos + 1), v, entryTTL);
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
			MemoryPartition partition = getPartition(prefix);
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
				MemoryPartition partition = getPartition(prefix);
				if (partition != null) {
					partition.clean(match.substring(pos + 1));
				}

			} else {

				// Remove entire partitions
				final long stamp = lock.writeLock();
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
					lock.unlockWrite(stamp);
				}
			}
		} catch (Throwable cause) {
			logger.warn("Unable to clean cache!", cause);
		}
		return Promise.resolve();
	}

	protected int partitionPosition(String key, boolean throwErrorIfMissing) {
		int i = key.indexOf('.');
		if (i == -1 && throwErrorIfMissing) {
			throw new MoleculerServerError("Invalid cache key, a point is missing from the key (" + key + ")!", null,
					broker.getNodeID(), "INVALID_CACHE_KEY", "key", key);
		}
		return i;
	}

	// --- MEMORY PARTITION ---

	protected static class MemoryPartition {

		// --- CLONE VALUES ---

		/**
		 * Do you need to make a copy of the returned values? Cloning the values
		 * is much SAFER, but little bit slower. If the services work with
		 * common objects, they can modify the cached object. If you turn off
		 * cloning, the cache will be faster, but you need to be careful NOT TO
		 * CHANGE the values from the cache!
		 */
		protected boolean useCloning = true;

		// --- READ/WRITE LOCK ---

		protected final StampedLock lock = new StampedLock();

		// --- MEMORY CACHE PARTITION ---

		protected final LinkedHashMap<String, PartitionEntry> cache;

		// --- CONSTUCTORS ---

		protected MemoryPartition(int capacity, boolean useCloning) {
			cache = new LinkedHashMap<String, PartitionEntry>(capacity, 1.0f, true) {

				private static final long serialVersionUID = 5994447707758047152L;

				protected final boolean removeEldestEntry(Map.Entry<String, PartitionEntry> entry) {
					return size() > capacity;
				};
			};
			this.useCloning = useCloning;
		}

		// --- REMOVE OLD ENTRIES ---

		protected void removeOldEntries(long now) {
			LinkedList<String> expiredKeys = new LinkedList<>();
			long stamp = lock.tryOptimisticRead();
			if (stamp != 0) {
				try {
					collectExpiredEntries(now, expiredKeys);
				} catch (Exception modified) {
					stamp = 0;
					expiredKeys.clear();
				}
			}
			if (!lock.validate(stamp) || stamp == 0) {
				stamp = lock.readLock();
				try {
					collectExpiredEntries(now, expiredKeys);
				} finally {
					lock.unlockRead(stamp);
				}
			}
			if (expiredKeys.isEmpty()) {
				return;
			}
			stamp = lock.writeLock();
			try {
				for (String key : expiredKeys) {
					cache.remove(key);
				}
			} finally {
				lock.unlockWrite(stamp);
			}
		}

		protected void collectExpiredEntries(long now, LinkedList<String> expiredKeys) {
			PartitionEntry pEntry;
			for (Map.Entry<String, PartitionEntry> entry : cache.entrySet()) {
				pEntry = entry.getValue();
				if (pEntry.expireAt > 0 && pEntry.expireAt <= now) {
					expiredKeys.addLast(entry.getKey());
				}
			}
		}

		// --- CACHE METHODS ---

		protected Tree get(String key) throws Exception {
			PartitionEntry entry = null;
			long stamp = lock.tryOptimisticRead();
			if (stamp != 0) {
				try {
					entry = cache.get(key);
				} catch (Exception modified) {
					stamp = 0;
				}
			}
			if (!lock.validate(stamp) || stamp == 0) {
				stamp = lock.readLock();
				try {
					entry = cache.get(key);
				} finally {
					lock.unlockRead(stamp);
				}
			}
			if (entry == null) {
				return null;
			}
			if (useCloning) {
				return entry.value.clone();
			}
			return entry.value;
		}

		protected void set(String key, Tree value, int ttl) {
			final long stamp = lock.writeLock();
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
				lock.unlockWrite(stamp);
			}
		}

		protected void del(String key) {
			final long stamp = lock.writeLock();
			try {
				cache.remove(key);
			} finally {
				lock.unlockWrite(stamp);
			}
		}

		protected void clean(String match) {
			final long stamp = lock.writeLock();
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
				lock.unlockWrite(stamp);
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

	public boolean isUseCloning() {
		return useCloning;
	}

	public void setUseCloning(boolean useCloning) {
		this.useCloning = useCloning;
	}

}