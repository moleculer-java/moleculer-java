package services.moleculer.cachers;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import services.moleculer.utils.GlobMatcher;

final class MemoryPartition {

	// --- CACHE VARIABLES ---

	private final Lock readerLock;
	private final Lock writerLock;

	private final LinkedHashMap<String, Object> cache;

	// --- CONSTUCTORS ---

	MemoryPartition(int capacity) {
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

	// --- CACHE METHODS ---

	final Object get(String key) {
		readerLock.lock();
		try {
			return cache.get(key);
		} finally {
			readerLock.unlock();
		}
	}

	final void set(String key, Object value) {
		writerLock.lock();
		try {
			cache.put(key, value);
		} finally {
			writerLock.unlock();
		}
	}

	final void del(String key) {
		writerLock.lock();
		try {
			cache.remove(key);
		} finally {
			writerLock.unlock();
		}
	}

	final void clean(String match) {
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