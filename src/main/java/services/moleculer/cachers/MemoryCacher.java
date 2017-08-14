package services.moleculer.cachers;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryCacher extends Cacher {

	// --- CONSTANTS ---

	public static final int DEFAULT_CAPACITY = 2048;
	public static final float DEFAULT_LOAD_FACTOR = 0.75F;
	public static final int DEFAULT_CONCURRENCY_LEVEL = 16;

	// --- CACHE INSTANCE ---

	protected final ConcurrentHashMap<String, Object> cache;

	// --- CONSTUCTORS ---

	public MemoryCacher() {
		this(EMPTY_PREFIX, UNLIMITED_TTL, DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
	}

	public MemoryCacher(String prefix) {
		this(prefix, UNLIMITED_TTL, DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
	}

	public MemoryCacher(String prefix, long ttl) {
		this(prefix, ttl, DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
	}

	public MemoryCacher(String prefix, long ttl, int initialCapacity, float loadFactor, int concurrencyLevel) {
		super(prefix, ttl);
		
		// Create memory cache
		cache = new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
	}

	// --- CLOSE CACHE INSTANCE ---
	
	@Override
	public void close() {
		cache.clear();
	}
	
	// --- CACHE METHODS ---

	@Override
	public Object get(String key) {
		return cache.get(key);
	}

	@Override
	public void set(String key, Object value) {
		cache.put(key, value);
	}

	@Override
	public void del(String key) {
		cache.remove(key);
	}

	@Override
	public void clean(String match) {
		if (match == null) {
			cache.clear();
		} else {
			match = match.trim();
			if (match.isEmpty() || ALL_ITEMS.equals(match)) {
				cache.clear();
			} else {
				Iterator<String> i = cache.keySet().iterator();
				String key;
				while (i.hasNext()) {
					key = i.next();

					// TODO implement matcher
					if (key != null) {
						i.remove();
					}
				}
			}
		}
	}

}