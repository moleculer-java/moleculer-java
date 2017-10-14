package services.moleculer.cachers;

import java.util.Iterator;

import org.caffinitas.ohc.OHCache;
import org.caffinitas.ohc.OHCacheBuilder;

import io.datatree.Tree;
import io.datatree.dom.TreeReaderRegistry;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.eventbus.GlobMatcher;
import services.moleculer.services.Name;

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

	private final long capacity;
	private final int segmentCount;
	private final int hashTableSize;

	private String format;

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

		// Autodetect the fastest serialization format
		if (format == null) {
			try {
				TreeReaderRegistry.getReader("smile");
				format = "smile";
			} catch (Throwable notFound) {
				format = "json";
			}
		}

		String formatName = format == null ? "JSON" : format.toUpperCase();
		logger.info("Off-heap Memory Cacher will use " + formatName + " data serializer.");

		// Create cache
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
		builder.executorService(broker.components().scheduler());
		cache = builder.build();
	}

	// --- CLOSE CACHE INSTANCE ---

	@Override
	public void stop() {
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
	public Promise get(String key) {
		Promise promise = null;
		try {
			byte[] bytes = cache.get(key);
			if (bytes == null) {
				return null;
			}
			Tree value = new Tree(bytes, format);
			return Promise.resolve(value);
		} catch (Throwable cause) {
			logger.warn("Unable to get data from MemoryCacher!", cause);
		}
		return promise;
	}

	@Override
	public void set(String key, Tree value) {
		if (value == null) {
			cache.remove(key);
		} else {
			byte[] bytes = value.toBinary(format, true);
			cache.put(key, bytes);
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