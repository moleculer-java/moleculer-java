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

import static services.moleculer.util.CommonUtils.compress;
import static services.moleculer.util.CommonUtils.decompress;
import static services.moleculer.util.CommonUtils.nameOf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.zip.Deflater;

import org.caffinitas.ohc.CacheSerializer;
import org.caffinitas.ohc.OHCache;
import org.caffinitas.ohc.OHCacheBuilder;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.eventbus.Matcher;
import services.moleculer.serializer.JsonSerializer;
import services.moleculer.serializer.Serializer;
import services.moleculer.service.Name;
import services.moleculer.util.CheckedTree;

/**
 * Off-heap cache implementation (it's similar to MemoryCacher, but stores
 * entries in the off-heap RAM). Supports global and entry-level TTL
 * configuration. See this gitHub project for a more description:
 * https://github.com/snazy/ohc.<br>
 * Configuration properties:
 * <ul>
 * <li>ttl: expire time of entries in memory, in seconds (default: 0 = never
 * expires)
 * <li>capacity: capacity for data over the whole cache in MEGABYTES
 * <li>segmentCount: number of segments (must be a power of 2), defaults to
 * number-of-cores * 2
 * <li>hashTableSize: hash table size (must be a power of 2), defaults to 8192
 * <li>compressAbove: compress key and/or value above this size (BYTES)
 * <li>format: serializator type ("json", "smile", etc.)
 * </ul>
 * Performance (per thread / core):<br>
 * <br>
 * <b>Small uncompressed data</b><br>
 * In SMILE format: 392 000 gets / second<br>
 * In JSON format: 380 000 gets / second<br>
 * <br>
 * <b>Compressed large data in 20% compression ratio</b><br>
 * In SMILE format: 20 000 gets / second<br>
 * In JSON format: 16 000 gets / second<br>
 * <br>
 * This cache is fundamentally slower than MemoryCacher, but it can store
 * compressed entries in the off-heap RAM. OHCacher is the solution to store
 * huge amount of data in memory; if you plan to store few thousands (or less)
 * entries in the cache, use the faster MemoryCacher, otherwise use OHCacher.
 * <br>
 * <br>
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/org.caffinitas.ohc/ohc-core-j8<br>
 * compile group: 'org.caffinitas.ohc', name: 'ohc-core-j8', version: '0.6.1'
 * <br>
 * <br>
 * 
 * @see MemoryCacher
 * @see RedisCacher
 */
@Name("Off-heap Memory Cacher")
public class OHCacher extends Cacher {

	// --- CONTENT CONTAINER NAME ---

	protected static final String CONTENT = "_";

	// --- PROPERTIES ---

	/**
	 * Maximum capacity of whole cache in MEGABYTES
	 */
	protected long capacity;

	/**
	 * Number of segments (must be a power of 2), defaults to number-of-cores *
	 * 2
	 */
	protected int segmentCount;

	/**
	 * Hash table size (must be a power of 2), defaults to 8192
	 */
	protected int hashTableSize;

	/**
	 * Expire time, in seconds (0 = never expires)
	 */
	protected int ttl;

	/**
	 * Compress key and/or value above this size (BYTES), 0 = disable
	 * compression
	 */
	protected int compressAbove = 1024;

	/**
	 * Compression level (best speed = 1, best compression = 9)
	 */
	protected int compressionLevel = Deflater.BEST_SPEED;

	/**
	 * Unused parameter (is only interpreted by distributed cachers, like Redis).
	 */
	protected int maxParamsLength;

	// --- SERIALIZER / DESERIALIZER ---

	protected Serializer serializer = new JsonSerializer();

	// --- OFF-HEAP CACHE INSTANCE ---

	protected OHCache<byte[], byte[]> cache;

	// --- CONSTRUCTORS ---

	/**
	 * Creates Off-heap Cacher with the default settings.
	 */
	public OHCacher() {
		this(0, 0, 0, 0, 1024);
	}

	/**
	 * Creates Off-heap Cacher.
	 *
	 * @param maxCapacity
	 *            capacity for data over the whole cache in MEGABYTES
	 * @param defaultTtl
	 *            expire time of entries in memory, in seconds (default: 0 =
	 *            never expires)
	 */
	public OHCacher(long maxCapacity, int defaultTtl) {
		this(maxCapacity, defaultTtl, 0, 0, 1024);
	}

	/**
	 * Creates Off-heap Cacher.
	 *
	 * @param maxCapacity
	 *            capacity for data over the whole cache in MEGABYTES
	 * @param segmentCount
	 *            mumber of segments (must be a power of 2), defaults to
	 *            number-of-cores * 2
	 * @param hashTableSize
	 *            hash table size (must be a power of 2), defaults to 8192
	 * @param defaultTtl
	 *            expire time of entries in memory, in seconds (default: 0 =
	 *            never expires)
	 * @param compressAbove
	 *            compress key and/or value above this size (in BYTES)
	 */
	public OHCacher(long maxCapacity, int defaultTtl, int segmentCount, int hashTableSize, int compressAbove) {
		this.capacity = maxCapacity;
		this.ttl = defaultTtl;
		this.segmentCount = segmentCount;
		this.hashTableSize = hashTableSize;
		this.compressAbove = compressAbove;
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
		serializer.started(broker);
		logger.info(nameOf(this, true) + " will use " + nameOf(serializer, true) + '.');

		// Create cache
		OHCacheBuilder<byte[], byte[]> builder = OHCacheBuilder.newBuilder();
		if (capacity > 0) {

			// Capacity specified in MEGABYTES
			builder.capacity(capacity * 1024 * 1024);
		}
		if (segmentCount > 0) {
			builder.segmentCount(segmentCount);
		}
		if (hashTableSize > 0) {
			builder.hashTableSize(hashTableSize);
		}
		builder.timeouts(true);
		if (ttl > 0) {
			builder.defaultTTLmillis(ttl * 1000L);
			logger.info("Entries in cache expire after " + ttl + " seconds.");
		}
		logger.info("Maximum size of the cache is " + capacity + " Mbytes.");

		// Set serializers
		final ArraySerializer serializer = new ArraySerializer();
		builder.keySerializer(serializer);
		builder.valueSerializer(serializer);

		// Set scheduler
		builder.executorService(broker.getConfig().getScheduler());

		// Create cache
		cache = builder.throwOOME(true).build();
	}

	// --- CLOSE CACHE INSTANCE ---

	@Override
	public void stopped() {
		if (cache != null) {
			try {
				cache.close();
			} catch (Throwable ignored) {
			}
			cache = null;
		}
	}

	// --- IMPLEMENTED CACHE METHODS ---

	@Override
	public Promise get(String key) {
		try {
			byte[] bytes = cache.get(keyToBytes(key));
			if (bytes != null) {
				return Promise.resolve(bytesToValue(bytes));
			}
		} catch (Throwable cause) {
			logger.warn("Unable to read data from off-heap cache!", cause);
		}
		return Promise.resolve((Object) null);
	}

	@Override
	public Promise set(String key, Tree value, int ttl) {
		try {
			if (value == null) {
				cache.remove(keyToBytes(key));
			} else {
				if (ttl > 0) {

					// Entry-level TTL (in seconds)
					long expireAt = ttl * 1000L + System.currentTimeMillis();
					cache.put(keyToBytes(key), valueToBytes(value), expireAt);
				} else {

					// Use the default TTL
					cache.put(keyToBytes(key), valueToBytes(value));
				}
			}
		} catch (Throwable cause) {
			logger.warn("Unable to write data to off-heap cache!", cause);
		}
		return Promise.resolve();
	}

	@Override
	public Promise del(String key) {
		try {
			cache.remove(keyToBytes(key));
		} catch (Throwable cause) {
			logger.warn("Unable to delete data from off-heap cache!", cause);
		}
		return Promise.resolve();
	}

	@Override
	public Promise clean(String match) {
		try {
			if (match.isEmpty() || match.startsWith("*")) {
				cache.clear();
			} else {
				String pattern;
				if (match.indexOf('.') == -1 && (match.endsWith("*") && !match.endsWith("**"))) {
					pattern = match + '*';
				} else {
					pattern = match;
				}
				Iterator<byte[]> i = cache.keyIterator();
				String key;
				while (i.hasNext()) {
					key = bytesToKey(i.next());
					if (Matcher.matches(key, pattern)) {
						i.remove();
					}
				}
			}
		} catch (Throwable cause) {
			logger.warn("Unable to clean off-heap cache!", cause);
		}
		return Promise.resolve();
	}

	// --- CACHE SERIALIZER ---

	protected byte[] keyToBytes(String key) throws Exception {
		int i = key.indexOf(':');

		byte[] part1;
		byte[] part2;
		boolean compressed;

		if (i == -1) {
			part1 = key.getBytes(StandardCharsets.UTF_8);
			part2 = new byte[0];
			compressed = false;
		} else {
			part1 = key.substring(0, i).getBytes(StandardCharsets.UTF_8);
			part2 = key.substring(i + 1).getBytes(StandardCharsets.UTF_8);
			if (compressAbove > 0 && part2.length > compressAbove) {
				part2 = compress(part2, compressionLevel);
				compressed = true;
			} else {
				compressed = false;
			}
		}

		// Write key packet
		ByteArrayOutputStream out = new ByteArrayOutputStream(part1.length + part2.length + 16);
		DataOutputStream dos = new DataOutputStream(out);
		dos.writeInt(part1.length);
		dos.write(part1);
		dos.writeInt(part2.length);
		dos.write(part2);
		dos.writeBoolean(compressed);
		dos.flush();

		// Return key as partly compressed bytes
		return out.toByteArray();
	}

	protected String bytesToKey(byte[] bytes) throws Exception {

		// Read key packet
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		DataInputStream dis = new DataInputStream(in);
		int len = dis.readInt();
		byte[] part1 = new byte[len];
		if (len > 0) {
			dis.readFully(part1);
		}

		// Return the first part of the key
		return new String(part1, StandardCharsets.UTF_8);
	}

	protected byte[] valueToBytes(Tree tree) throws Exception {

		// Compress content
		Tree root = new CheckedTree(Collections.singletonMap(CONTENT, tree.asObject()));
		byte[] bytes = serializer.write(root);
		boolean compressed;
		if (compressAbove > 0 && bytes.length > compressAbove) {
			bytes = compress(bytes, compressionLevel);
			compressed = true;
		} else {
			compressed = false;
		}
		byte[] copy = new byte[bytes.length + 1];
		System.arraycopy(bytes, 0, copy, 1, bytes.length);
		if (compressed) {

			// Compressed -> first byte = 1
			copy[0] = (byte) 1;
		}
		return copy;
	}

	protected Tree bytesToValue(byte[] bytes) throws Exception {

		// Decompress content
		byte[] copy = new byte[bytes.length - 1];
		System.arraycopy(bytes, 1, copy, 0, bytes.length - 1);
		if (bytes[0] == 1) {

			// First byte == 1 -> compressed
			copy = decompress(copy);
		}
		Tree root = serializer.read(copy);
		Tree content = root.get(CONTENT);
		if (content != null) {
			return content;
		}
		return root;
	}

	protected static class ArraySerializer implements CacheSerializer<byte[]> {

		@Override
		public int serializedSize(byte[] value) {
			return value.length;
		}

		@Override
		public void serialize(byte[] value, ByteBuffer buf) {
			buf.put(value);
		}

		@Override
		public byte[] deserialize(ByteBuffer buf) {
			int len = buf.remaining();
			byte[] bytes = new byte[len];
			buf.get(bytes, 0, len);
			return bytes;
		}

	}

	// --- GETTERS / SETTERS ---

	public long getCapacity() {
		return capacity;
	}

	public void setCapacity(long capacity) {
		this.capacity = capacity;
	}

	public int getSegmentCount() {
		return segmentCount;
	}

	public void setSegmentCount(int segmentCount) {
		this.segmentCount = segmentCount;
	}

	public int getHashTableSize() {
		return hashTableSize;
	}

	public void setHashTableSize(int hashTableSize) {
		this.hashTableSize = hashTableSize;
	}

	public int getTtl() {
		return ttl;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public int getCompressAbove() {
		return compressAbove;
	}

	public void setCompressAbove(int compressAbove) {
		this.compressAbove = compressAbove;
	}

	public Serializer getSerializer() {
		return serializer;
	}

	public void setSerializer(Serializer serializer) {
		this.serializer = Objects.requireNonNull(serializer);
	}

	public int getCompressionLevel() {
		return compressionLevel;
	}

	public void setCompressionLevel(int compressionLevel) {
		this.compressionLevel = compressionLevel;
	}

	public int getMaxParamsLength() {
		return maxParamsLength;
	}

	public void setMaxParamsLength(int maxParamsLength) {
		this.maxParamsLength = maxParamsLength;
	}
	
}