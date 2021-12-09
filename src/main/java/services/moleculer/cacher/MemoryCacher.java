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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.error.MoleculerServerError;
import services.moleculer.eventbus.Matcher;
import services.moleculer.metrics.MetricCounter;
import services.moleculer.metrics.StoppableTimer;
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
	protected int cleanup;

	/**
	 * Do you need to make a copy of the returned values? Cloning the values is
	 * much safer, but little bit slower. If the services work with common
	 * objects, they can modify the cached object. If you turn off cloning, the
	 * cache will be faster, but you need to be careful NOT TO CHANGE the values
	 * from the cache!
	 */
	protected boolean useCloning = true;

	// --- READ/WRITE LOCK ---

	protected final ReadLock readLock;
	protected final WriteLock writeLock;

	// --- PARTITIONS / CACHE REGIONS ---

	protected final HashMap<String, MemoryPartition> partitions = new HashMap<>();

	// --- CLEANUP TIMER ---

	/**
	 * Cancelable timer
	 */
	protected volatile ScheduledFuture<?> timer;

	protected AtomicBoolean timerStarted = new AtomicBoolean();

	protected AtomicBoolean timerStopped = new AtomicBoolean();

	// --- COUNTERS ---

	protected MetricCounter counterExpired;
	protected MetricCounter counterGet;
	protected MetricCounter counterSet;
	protected MetricCounter counterDel;
	protected MetricCounter counterClean;
	protected MetricCounter counterFound;

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

		// Create locks
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
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
		startTimer(0);

		// Log capacity
		logger.info("Maximum number of cached entries is " + capacity + " per partition.");

		// Create counters
		if (metrics != null) {
			counterExpired = metrics.increment(MOLECULER_CACHER_EXPIRED_TOTAL, MOLECULER_CACHER_EXPIRED_TOTAL_DESC, 0);
			counterGet = metrics.increment(MOLECULER_CACHER_GET_TOTAL, MOLECULER_CACHER_GET_TOTAL_DESC, 0);
			counterSet = metrics.increment(MOLECULER_CACHER_SET_TOTAL, MOLECULER_CACHER_SET_TOTAL_DESC, 0);
			counterDel = metrics.increment(MOLECULER_CACHER_DEL_TOTAL, MOLECULER_CACHER_DEL_TOTAL_DESC, 0);
			counterClean = metrics.increment(MOLECULER_CACHER_CLEAN_TOTAL, MOLECULER_CACHER_CLEAN_TOTAL_DESC, 0);
			counterFound = metrics.increment(MOLECULER_CACHER_FOUND_TOTAL, MOLECULER_CACHER_FOUND_TOTAL_DESC, 0);
		}
	}

	// --- REMOVE OLD ENTRIES ---

	protected void startTimer(int entryTTL) {
		int delay = cleanup > 0 ? cleanup : entryTTL;
		if (delay < 1) {
			return;
		}
		if (timerStarted.compareAndSet(false, true) && !timerStopped.get()) {
			timer = broker.getConfig().getScheduler().scheduleWithFixedDelay(this, delay, delay, TimeUnit.SECONDS);
		} else {
			return;
		}
		if (ttl > 0) {
			logger.info("Entries in cache expire after " + delay + " seconds.");
		}
	}

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
		timerStopped.set(true);
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
		readLock.lock();
		try {
			partition = partitions.get(prefix);
		} finally {
			readLock.unlock();
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
				partition = new MemoryPartition(this);
				MemoryPartition previous;
				writeLock.lock();
				try {
					previous = partitions.putIfAbsent(prefix, partition);
				} finally {
					writeLock.unlock();
				}
				if (previous != null) {
					partition = previous;
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

			// Start cleanup process
			if (entryTTL > 0 && !timerStarted.get()) {
				startTimer(entryTTL);
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

	protected int partitionPosition(String key, boolean throwErrorIfMissing) {
		int i = key.indexOf('.');
		if (i == -1 && throwErrorIfMissing) {
			throw new MoleculerServerError("Invalid cache key, a point is missing from the key (" + key + ")!", null,
					broker.getNodeID(), "INVALID_CACHE_KEY", "key", key);
		}
		return i;
	}

	/**
	 * Lists all keys of cached entries.
	 * 
	 * @return a Tree object with a "keys" array. 
	 */
	@Override
	public Promise getCacheKeys() {
		Tree result = new Tree();
		Tree keys = result.putList("keys");
		writeLock.lock();
		try {
			Iterator<Map.Entry<String, MemoryPartition>> i = partitions.entrySet()
					.iterator();
			Map.Entry<String, MemoryPartition> entry;
			while (i.hasNext()) {
				entry = i.next();
				entry.getValue().addKeysTo(keys, entry.getKey());
			}
		} finally {
			writeLock.unlock();
		}
		return Promise.resolve(result); 
	}
	
	// --- MEMORY PARTITION ---

	protected static class MemoryPartition {

		// --- PARENT ---

		protected final MemoryCacher parent;

		// --- READ/WRITE LOCK ---

		protected final ReadLock readLock;
		protected final WriteLock writeLock;

		// --- MEMORY CACHE PARTITION ---

		protected final LinkedHashMap<String, PartitionEntry> cache;

		// --- CONSTUCTORS ---

		protected MemoryPartition(MemoryCacher parent) {

			// Insertion-order is thread safe, access-order is not
			cache = new LinkedHashMap<String, PartitionEntry>(parent.capacity, 1.0f, false) {

				private static final long serialVersionUID = 5994447707758047152L;

				protected final boolean removeEldestEntry(Map.Entry<String, PartitionEntry> entry) {
					boolean remove = size() > parent.capacity;

					// Metrics
					if (remove && parent.counterExpired != null) {
						parent.counterExpired.increment();
					}

					return remove;
				};
			};
			this.parent = parent;

			// Create locks
			ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
			readLock = lock.readLock();
			writeLock = lock.writeLock();
		}

		// --- REMOVE OLD ENTRIES ---

		protected void removeOldEntries(long now) {
			LinkedList<String> expiredKeys = new LinkedList<>();
			readLock.lock();
			try {
				collectExpiredEntries(now, expiredKeys);
			} finally {
				readLock.unlock();
			}
			if (expiredKeys.isEmpty()) {
				return;
			}

			// Metrics
			if (parent.counterExpired != null) {
				parent.counterExpired.increment(expiredKeys.size());
			}

			// Remove elements
			writeLock.lock();
			try {
				for (String String : expiredKeys) {
					cache.remove(String);
				}
			} finally {
				writeLock.unlock();
			}
		}

		protected void collectExpiredEntries(long now, LinkedList<String> expiredKeys) {
			PartitionEntry partitionEntry;
			for (Map.Entry<String, PartitionEntry> entry : cache.entrySet()) {
				partitionEntry = entry.getValue();
				if (partitionEntry.expireAt > 0 && partitionEntry.expireAt <= now) {
					expiredKeys.addLast(entry.getKey());
				}
			}
		}

		// --- CACHE METHODS ---

		protected Tree get(String key) throws Exception {

			// Metrics
			StoppableTimer getTimer = null;
			if (parent.metrics != null) {
				parent.counterGet.increment();
				getTimer = parent.metrics.timer(MOLECULER_CACHER_GET_TIME, MOLECULER_CACHER_GET_TIME_DESC);
			}

			PartitionEntry entry = null;

			readLock.lock();
			try {
				entry = cache.get(key);
			} finally {
				readLock.unlock();
			}
			try {
				if (entry == null || entry.value == null) {
					return null;
				}
				if (parent.counterFound != null) {
					parent.counterFound.increment();
				}
				if (parent.useCloning && entry.value != null) {
					return entry.value.clone();
				}
				return entry.value;
			} finally {
				if (getTimer != null) {
					getTimer.stop();
				}
			}
		}

		protected void set(String key, Tree value, int ttl) {

			// Metrics
			StoppableTimer setTimer = null;
			if (parent.metrics != null) {
				parent.counterSet.increment();
				setTimer = parent.metrics.timer(MOLECULER_CACHER_SET_TIME, MOLECULER_CACHER_SET_TIME_DESC);
			}

			writeLock.lock();
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
				writeLock.unlock();
				if (setTimer != null) {
					setTimer.stop();
				}
			}
		}

		protected void del(String key) {

			// Metrics
			StoppableTimer delTimer = null;
			if (parent.metrics != null) {
				parent.counterDel.increment();
				delTimer = parent.metrics.timer(MOLECULER_CACHER_DEL_TIME, MOLECULER_CACHER_DEL_TIME_DESC);
			}

			writeLock.lock();
			try {
				cache.remove(key);
			} finally {
				writeLock.unlock();
				if (delTimer != null) {
					delTimer.stop();
				}
			}
		}

		protected void clean(String match) {

			// Metrics
			StoppableTimer cleanTimer = null;
			if (parent.metrics != null) {
				parent.counterClean.increment();
				cleanTimer = parent.metrics.timer(MOLECULER_CACHER_CLEAN_TIME, MOLECULER_CACHER_CLEAN_TIME_DESC);
			}

			writeLock.lock();
			try {
				if (match.isEmpty() || "**".equals(match)) {
					cache.clear();
				} else if (match.indexOf('*') == -1) {
					cache.remove(match);
				} else {
					Iterator<String> i = cache.keySet().iterator();
					LinkedList<String> r = new LinkedList<>();
					String key;
					while (i.hasNext()) {
						key = i.next();
						if (Matcher.matches(key, match)) {
							// i.remove();
							r.add(key);
						}
					}
					for (String rx: r) {
						cache.remove(rx);
					}
				}
			} finally {
				writeLock.unlock();
				if (cleanTimer != null) {
					cleanTimer.stop();
				}
			}
		}

		protected void addKeysTo(Tree list, String prefix) {
			writeLock.lock();
			try {
				for (String key : cache.keySet()) {
					list.add(prefix + '.' + key);
				}
			} finally {
				writeLock.unlock();
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