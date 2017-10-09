package services.moleculer.cachers;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.caffinitas.ohc.OHCache;
import org.caffinitas.ohc.OHCacheBuilder;

import services.moleculer.ServiceBroker;
import services.moleculer.eventbus.GlobMatcher;
import services.moleculer.services.Name;
import services.moleculer.utils.Serializer;

/**
 * Off-heap cache implementation (it's similar to MemoryCacher, but stores
 * entries in the off-heap RAM). Requires "OHC" Windows/Linux/OSX off-heap
 * HashTable API (compile group: 'org.caffinitas.ohc', name: 'ohc-core-j8',
 * version: '0.6.1'). See this gitHub project for a more description:
 * https://github.com/snazy/ohc.
 */
@Name("Off-heap Memory Cacher")
public class OHCCacher extends Cacher {

	// --- PROPERTIES ---

	private final String format;
	
	private final long capacity;
	private final int segmentCount;
	private final int hashTableSize;

	// --- OFF-HEAP CACHE INSTANCE ---

	private OHCache<String, byte[]> cache;

	// --- CONSTRUCTORS ---

	public OHCCacher() {
		this(null, 0, 0, 0);
	}

	public OHCCacher(String format, long capacity, int segmentCount, int hashTableSize) {
		this.format = format;
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
		OHCacheBuilder<String, byte[]> builder = OHCacheBuilder.newBuilder();
		if (capacity > 0) {
			builder.capacity(capacity);
		}
		if (segmentCount > 0) {
			builder.segmentCount(segmentCount);
		}
		if (hashTableSize > 0) {
			builder.hashTableSize(hashTableSize);
		}
		builder.executorService(Executors.newSingleThreadScheduledExecutor());
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
	public CompletableFuture<Object> get(String key) {
		byte[] bytes = cache.get(key);
		if (bytes == null) {
			return null;
		}
		Object value = Serializer.deserialize(cache.get(key), format);
		return CompletableFuture.completedFuture(value);
	}

	@Override
	public void set(String key, Object value) {
		if (value == null) {
			cache.remove(key);
		} else {
			cache.put(key, Serializer.serialize(value, format));
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