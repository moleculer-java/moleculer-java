package services.moleculer.cachers;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import services.moleculer.ServiceBroker;
import services.moleculer.utils.GlobMatcher;

public class MemoryCacher extends Cacher {

	// --- CACHE VARIABLES ---

	private int capacity = 2048;

	private Lock readerLock;
	private Lock writerLock;

	private LinkedHashMap<String, Object> cache;

	// --- CONSTUCTORS ---

	public MemoryCacher() {
		super();
	}

	public MemoryCacher(String prefix) {
		super(prefix);
	}

	public MemoryCacher(String prefix, long ttl) {
		super(prefix, ttl);
	}

	public MemoryCacher(String prefix, long ttl, int capacity) {
		super(prefix, ttl);
		this.capacity = capacity;
	}

	// --- INIT CACHE INSTANCE ---

	/**
	 * Initializes cacher instance.
	 * 
	 * @param broker
	 */
	public void init(ServiceBroker broker) {
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
		readerLock = lock.readLock();
		writerLock = lock.writeLock();
		cache = new LinkedHashMap<String, Object>(capacity + 1, 1.0f, true) {

			private static final long serialVersionUID = 5994447707758047152L;

			protected final boolean removeEldestEntry(Map.Entry<String, Object> entry) {
				if (this.size() > capacity) {
					return true;
				}
				return false;
			};
		};
	}

	// --- CLOSE CACHE INSTANCE ---

	@Override
	public void close() {
		cache.clear();
	}

	// --- CACHE METHODS ---

	@Override
	public Object get(String key) {
		readerLock.lock();
		try {
			return cache.get(key);
		} finally {
			readerLock.unlock();
		}
	}

	@Override
	public void set(String key, Object value) {
		writerLock.lock();
		try {
			cache.put(key, value);
		} finally {
			writerLock.unlock();
		}
	}

	@Override
	public void del(String key) {
		writerLock.lock();
		try {
			cache.remove(key);
		} finally {
			writerLock.unlock();
		}
	}

	@Override
	public void clean(String match) {
		writerLock.lock();
		try {
			if (match == null) {
				cache.clear();
			} else {
				match = match.trim();
				if (match.isEmpty() || "*".equals(match) || "**".equals(match)) {
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
			}
		} finally {
			writerLock.unlock();
		}
	}

}