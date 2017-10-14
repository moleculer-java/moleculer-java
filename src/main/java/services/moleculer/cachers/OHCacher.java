package services.moleculer.cachers;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.caffinitas.ohc.CacheSerializer;
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
 * https://github.com/snazy/ohc.<br>
 * Configuration properties:
 * <ul>
 * <li>ttl: Expire time of entries in memory, in seconds (default: 0 = never
 * expires)
 * <li>capacity: Capacity for data over the whole cache
 * <li>segmentCount: Number of segments (must be a power of 2), defaults to
 * number-of-cores * 2
 * <li>hashTableSize: hash table size (must be a power of 2), defaults to 8192
 * <li>format: Serializator type (json, smile, etc.)
 * </ul>
 * Performance: 10 000 000 gets within 6047 milliseconds
 */
@Name("Off-heap Memory Cacher")
public final class OHCacher extends Cacher {

	// --- PROPERTIES ---

	private long capacity;
	private int segmentCount;
	private int hashTableSize;
	private String format;

	/**
	 * Expire time, in seconds (0 = never expires)
	 */
	private int ttl;

	// --- OFF-HEAP CACHE INSTANCE ---

	private OHCache<String, byte[]> cache;

	// --- CONSTRUCTORS ---

	public OHCacher() {
		this(null, 0, 0, 0, 0);
	}

	public OHCacher(String format, long capacity, int segmentCount, int hashTableSize, int ttl) {
		this.format = format;
		this.capacity = capacity;
		this.segmentCount = segmentCount;
		this.hashTableSize = hashTableSize;
		this.ttl = ttl;
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
	public final void start(ServiceBroker broker, Tree config) throws Exception {

		// Process config
		capacity = config.get("capacity", capacity);
		segmentCount = config.get("segmentCount", segmentCount);
		hashTableSize = config.get("hashTableSize", hashTableSize);
		format = config.get("format", format);
		ttl = config.get("ttl", ttl);

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
		if (ttl > 0) {
			builder.defaultTTLmillis(ttl * 1000L);
			builder.timeouts(true);
		}
		
		// Cache key encoded as US-ASCII
		builder.keySerializer(new CacheSerializer<String>() {
			
			@Override
			public final int serializedSize(String value) {
				return value.length();
			}
			
			@Override
			public final void serialize(String value, ByteBuffer buf) {
				buf.put(value.getBytes(StandardCharsets.US_ASCII));
			}
			
			@Override
			public final String deserialize(ByteBuffer buf) {
				int len = buf.remaining();
				byte[] bytes = new byte[len];
				buf.get(bytes, 0, len);
				return new String(bytes, StandardCharsets.US_ASCII);
			}
			
		});
		builder.valueSerializer(new CacheSerializer<byte[]>() {
			
			@Override
			public final int serializedSize(byte[] value) {
				return value.length;
			}
			
			@Override
			public final void serialize(byte[] value, ByteBuffer buf) {
				buf.put(value);			
			}
			
			@Override
			public final byte[] deserialize(ByteBuffer buf) {
				int len = buf.remaining();
				byte[] bytes = new byte[len];
				buf.get(bytes, 0, len);
				return bytes;
			}
			
		});
		builder.executorService(broker.components().scheduler());
		cache = builder.build();
	}

	// --- CLOSE CACHE INSTANCE ---

	@Override
	public final void stop() {
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
	public final Promise get(String key) {
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
	public final void set(String key, Tree value) {
		if (value == null) {
			cache.remove(key);
		} else {
			byte[] bytes = value.toBinary(format, true);
			cache.put(key, bytes);
		}
	}

	@Override
	public final void del(String key) {
		cache.remove(key);
	}

	@Override
	public final void clean(String match) {
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