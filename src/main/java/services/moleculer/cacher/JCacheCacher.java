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
package services.moleculer.cacher;

import static services.moleculer.util.CommonUtils.nameOf;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.cache.Cache.Entry;
import javax.cache.CacheManager;
import javax.cache.Caching;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.eventbus.Matcher;
import services.moleculer.serializer.JsonSerializer;
import services.moleculer.serializer.Serializer;
import services.moleculer.service.Name;
import services.moleculer.util.CheckedTree;

/**
 * JSR-107 JCache is a standardized caching API that is Java 6 compatible and
 * introduced in JEE 8. WARNING! Core JCache API does NOT support entry-level
 * TTL parameter! If you need this feature use {@link RedisCacher},
 * {@link MemoryCacher}, or {@link OHCacher}. JCache is implemented by various
 * caching solutions:
 * <ul>
 * <li>Apache Ignite
 * <li>Hazelcast
 * <li>Oracle Coherence
 * <li>Couchbase (https://github.com/couchbaselabs/couchbase-java-cache)
 * <li>Terracotta Ehcache
 * <li>Infinispan
 * <li>Blazing Cache
 * <li>Cache2k
 * <li>Caffeine
 * <li>WebSphere eXtreme Scale
 * </ul>
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/javax.cache/cache-api<br>
 * compile group: 'javax.cache', name: 'cache-api', version: '1.0.0' <br>
 * <br>
 * 
 * @see MemoryCacher
 * @see OHCacher
 * @see RedisCacher
 */
@Name("JCache-based Cacher")
public class JCacheCacher extends DistributedCacher {

	// --- PARTITIONS / CACHE REGIONS ---

	protected final HashMap<String, javax.cache.Cache<String, byte[]>> partitions = new HashMap<>();

	// --- CACHE MANAGER ---

	protected CacheManager cacheManager;

	// --- SERIALIZER / DESERIALIZER ---

	protected Serializer serializer = new JsonSerializer();

	// --- LOCKS ---

	protected final Lock readLock;
	protected final Lock writeLock;

	// --- CONSTUCTORS ---

	public JCacheCacher() {
		this(Caching.getCachingProvider().getCacheManager());
	}

	public JCacheCacher(URI uri) {
		this(Caching.getCachingProvider().getCacheManager(uri, JCacheCacher.class.getClassLoader()));
	}

	public JCacheCacher(CacheManager cacheManager) {
		this.cacheManager = cacheManager;

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
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);
		logger.info(nameOf(this, true) + " will use " + nameOf(serializer, true) + '.');
	}

	// --- STOP CACHER ---

	@Override
	public void stopped() {

		// Close the cache manager
		if (cacheManager != null) {
			try {
				cacheManager.close();
			} catch (Exception ignored) {
			}
			cacheManager = null;
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
			String prefix = key.substring(0, pos);
			javax.cache.Cache<String, byte[]> partition;
			readLock.lock();
			try {
				partition = partitions.get(prefix);
			} finally {
				readLock.unlock();
			}
			if (partition != null) {
				byte[] bytes = partition.get(key.substring(pos + 1));
				if (bytes != null) {
					Tree root = serializer.read(bytes);
					Tree content = root.get(CONTENT);
					if (content != null) {
						return Promise.resolve(content);
					}
					return Promise.resolve(root);
				}
			}
		} catch (Throwable cause) {
			logger.warn("Unable to get data from JCache!", cause);
		}
		return Promise.resolve();
	}

	@Override
	public Promise set(String key, Tree value, int ttl) {
		try {
			int pos = partitionPosition(key, true);
			String prefix = key.substring(0, pos);
			javax.cache.Cache<String, byte[]> partition;
			writeLock.lock();
			try {
				partition = partitions.get(prefix);
				if (partition == null) {
					partition = cacheManager.getCache(prefix, String.class, byte[].class);
					partitions.put(prefix, partition);
				}
			} finally {
				writeLock.unlock();
			}
			if (value == null) {
				partition.remove(key);
			} else {
				Tree root = new CheckedTree(Collections.singletonMap(CONTENT, value.asObject()));
				byte[] bytes = serializer.write(root);
				partition.put(key.substring(pos + 1), bytes);
			}
		} catch (Throwable cause) {
			logger.warn("Unable to write data to JCache!", cause);
		}
		return Promise.resolve();
	}

	@Override
	public Promise del(String key) {
		int pos = partitionPosition(key, true);
		String prefix = key.substring(0, pos);
		javax.cache.Cache<String, byte[]> partition;
		readLock.lock();
		try {
			partition = partitions.get(prefix);
		} finally {
			readLock.unlock();
		}
		if (partition != null) {
			partition.remove(key.substring(pos + 1));
		}
		return Promise.resolve();
	}

	@Override
	public Promise clean(String match) {
		try {
			int pos = partitionPosition(match, false);
			if (pos > 0) {

				// Remove items in partitions
				String prefix = match.substring(0, pos);
				javax.cache.Cache<String, byte[]> partition;
				readLock.lock();
				try {
					partition = partitions.get(prefix);
				} finally {
					readLock.unlock();
				}
				if (partition != null) {
					clean(partition, match.substring(pos + 1));
				}

			} else {

				// Remove entire partitions
				writeLock.lock();
				try {
					if (match.isEmpty() || match.startsWith("*")) {
						for (javax.cache.Cache<String, byte[]> partition : partitions.values()) {
							partition.close();
						}
						partitions.clear();
					} else if (match.indexOf('*') == -1) {
						javax.cache.Cache<String, byte[]> partition = partitions.remove(match);
						if (partition != null) {
							partition.close();
						}
					} else {
						Iterator<Map.Entry<String, javax.cache.Cache<String, byte[]>>> i = partitions.entrySet()
								.iterator();
						Map.Entry<String, javax.cache.Cache<String, byte[]>> entry;
						while (i.hasNext()) {
							entry = i.next();
							if (Matcher.matches(entry.getKey(), match)) {
								javax.cache.Cache<String, byte[]> partition = entry.getValue();
								if (partition != null) {
									partition.close();
								}
								i.remove();
							}
						}
					}
				} finally {
					writeLock.unlock();
				}
			}
		} catch (Throwable cause) {
			logger.warn("Unable to clean JCache!", cause);
		}
		return Promise.resolve();
	}

	protected static final void clean(javax.cache.Cache<String, byte[]> partition, String match) throws Exception {
		Iterator<Entry<String, byte[]>> i = partition.iterator();
		Entry<String, byte[]> entry;
		while (i.hasNext()) {
			entry = i.next();
			if (Matcher.matches(entry.getKey(), match)) {
				i.remove();
			}
		}
	}

	protected static final int partitionPosition(String key, boolean throwErrorIfMissing) {
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

	// --- GETTERS / SETTERS ---

	public CacheManager getCacheManager() {
		return cacheManager;
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = Objects.requireNonNull(cacheManager);
	}

}