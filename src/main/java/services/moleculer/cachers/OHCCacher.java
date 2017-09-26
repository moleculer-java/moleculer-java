package services.moleculer.cachers;

import java.util.Iterator;

import org.caffinitas.ohc.OHCache;
import org.caffinitas.ohc.OHCacheBuilder;

import services.moleculer.ServiceBroker;
import services.moleculer.eventbus.GlobMatcher;

/**
 * Off-heap cache implementation (it's similar to MemoryCacher, but stores
 * entries in the off-heap RAM). Requires "OHC" Windows/Linux/OSX off-heap
 * HashTable API (compile group: 'org.caffinitas.ohc', name: 'ohc-core-j8',
 * version: '0.6.1'). See this gitHub project for a more description:
 * https://github.com/snazy/ohc.
 */
public class OHCCacher extends Cacher {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	public String name() {
		return "Off-heap Cacher";
	}
	
	// --- PROPERTIES ---

	private final long capacity;
	private final int segmentCount;
	private final int hashTableSize;

	// --- OFF-HEAP CACHE INSTANCE ---

	private OHCache<String, String> cache;

	// --- CONSTRUCTORS ---

	public OHCCacher() {
		this(0, 0, 0);
	}

	public OHCCacher(long capacity, int segmentCount, int hashTableSize) {
		super(false);
		this.capacity = capacity;
		this.segmentCount = segmentCount;
		this.hashTableSize = hashTableSize;
	}

	// --- INIT CACHE INSTANCE ---

	/**
	 * Initializes cacher instance.
	 * 
	 * @param broker
	 */
	public final void init(ServiceBroker broker) throws Exception {
		OHCacheBuilder<String, String> builder = OHCacheBuilder.newBuilder();
		if (capacity > 0) {
			builder.capacity(capacity);
		}
		if (segmentCount > 0) {
			builder.segmentCount(segmentCount);
		}
		if (hashTableSize > 0) {
			builder.hashTableSize(hashTableSize);
		}
		cache = builder.build();
	}

	// --- CLOSE CACHE INSTANCE ---

	@Override
	public void close() {
		if (cache != null) {
			try {
				cache.close();
			} catch (Exception ignored) {
			}
			cache = null;
		}
	}

	// --- IMPLEMENTED CACHE METHODS ---

	@Override
	public Object get(String key) {
		return deserialize(cache.get(key));
	}

	@Override
	public void set(String key, Object value) {
		if (value == null) {
			cache.remove(key);
		} else {
			cache.put(key, serialize(value));
		}
	}

	@Override
	public void del(String key) {
		cache.remove(key);
	}

	@Override
	public void clean(String match) {
		if (match.isEmpty() || match.startsWith("*")) {
			cache.clear();
		} else if (match.indexOf('*') == -1) {
			cache.remove(match);
		} else {
			Iterator<String> i = cache.keyIterator();
			String key;
			while (i.hasNext()) {
				key = i.next();
				if (GlobMatcher.matches(key, match)) {
					i.remove();
				}
			}
		}
	}

}