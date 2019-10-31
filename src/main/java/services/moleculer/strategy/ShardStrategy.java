/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
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
package services.moleculer.strategy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.datatree.dom.Cache;
import services.moleculer.ServiceBroker;
import services.moleculer.context.Context;
import services.moleculer.service.Endpoint;
import services.moleculer.service.Name;

/**
 * Sharding invocation strategy. Using consistent-hashing.<br>
 * More info: https://www.toptal.com/big-data/consistent-hashing
 * 
 * @see RoundRobinStrategy
 * @see NanoSecRandomStrategy
 * @see XorShiftRandomStrategy
 * @see SecureRandomStrategy
 * @see CpuUsageStrategy
 * @see NetworkLatencyStrategy
 */
@Name("Shard Strategy")
public class ShardStrategy<T extends Endpoint> extends XorShiftRandomStrategy<T> {

	// --- CONSTANTS ---

	protected static final char[] HEX = "0123456789abcdef".toCharArray();

	// --- PROPERTIES ---

	protected final String shardKey;
	protected final int vnodes;
	protected final Integer ringSize;
	protected final boolean useMeta;

	protected AtomicBoolean needRebuild = new AtomicBoolean(true);

	// --- RING ---

	protected AtomicReference<HashMap<Endpoint, long[]>> limitMapRef = new AtomicReference<>();

	// --- CACHE ---

	protected final Cache<Long, Endpoint> cache;

	// --- MD5 HASHERS ---

	protected final ThreadLocal<MessageDigest> hashers = new ThreadLocal<>();

	// --- CONSTRUCTOR ---

	public ShardStrategy(ServiceBroker broker, boolean preferLocal, String shardKey, int vnodes, Integer ringSize,
			int cacheSize) {
		super(broker, preferLocal);

		// Set properties
		if (shardKey != null && shardKey.startsWith("#")) {
			this.shardKey = shardKey.substring(1);
			useMeta = true;
		} else {
			this.shardKey = shardKey == null || shardKey.isEmpty() ? null : shardKey;
			useMeta = false;
		}
		this.vnodes = vnodes;
		this.ringSize = ringSize;

		// Init cache
		cache = new Cache<>(cacheSize);
	}

	// --- GET NEXT ENDPOINT ---

	@Override
	public Endpoint next(Context ctx, Endpoint[] array) {
		if (shardKey != null) {
			String key = getKeyFromContext(ctx);
			if (key != null) {
				if (needRebuild.compareAndSet(true, false)) {
					rebuild();
				}
				long hash = getHash(key);
				Endpoint next = cache.get(hash);
				if (next != null) {
					return next;
				}
				HashMap<Endpoint, long[]> limitMap = limitMapRef.get();
				if (limitMap != null) {
					for (int j = 0; j < vnodes; j++) {
						for (int i = 0; i < array.length; i++) {
							next = array[i];
							long[] limits = limitMap.get(next);
							if (limits == null) {
								continue;
							}
							if (hash <= limits[j]) {
								cache.put(hash, next);
								return next;
							}
						}
					}
				}
			}
		}
		return super.next(ctx, array);
	}

	// --- INTERNAL METHODS ---

	protected String getKeyFromContext(Context ctx) {
		if (ctx == null || ctx.params == null) {
			return null;
		}
		if (useMeta) {
			return ctx.params.getMeta().get(shardKey, (String) null);
		}
		return ctx.params.get(shardKey, (String) null);
	}

	protected void rebuild() {

		// Sort endpoints by nodeIDs
		Endpoint[] copy = new Endpoint[endpoints.length];
		System.arraycopy(endpoints, 0, copy, 0, copy.length);
		Arrays.sort(copy, (ep1, ep2) -> {
			return String.CASE_INSENSITIVE_ORDER.compare(ep1.getNodeID(), ep2.getNodeID());
		});

		// Calculate
		int total = copy.length * vnodes;
		long size = ringSize == null ? (long) Math.pow(2, 32) : ringSize;
		double slice = size / (double) total;

		// Build ring
		HashMap<Endpoint, long[]> limitMap = new HashMap<>(total * 2);
		long index = 0;
		for (int j = 0; j < vnodes; j++) {
			for (int i = 0; i < copy.length; i++) {
				long[] limits = limitMap.get(copy[i]);
				if (limits == null) {
					limits = new long[vnodes];
					limitMap.put(copy[i], limits);
				}
				if (j == vnodes - 1 && i == copy.length - 1) {
					limits[j] = size;
				} else {
					limits[j] = (long) (slice * (index++));
				}
			}
		}

		// Store the new ring
		limitMapRef.set(limitMap);
		cache.clear();
	}

	protected long getHash(String key) {

		// Calculate hash number
		long hash;
		try {
			byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
			MessageDigest hasher = hashers.get();
			if (hasher == null) {
				hasher = MessageDigest.getInstance("MD5");
				hashers.set(hasher);
			}
			hasher.update(bytes);
			byte[] md5Bytes = hasher.digest();
			char[] hexChars = new char[md5Bytes.length * 2];
			for (int j = 0; j < md5Bytes.length; j++) {
				int v = md5Bytes[j] & 0xFF;
				hexChars[j * 2] = HEX[v >>> 4];
				hexChars[j * 2 + 1] = HEX[v & 0x0F];
			}
			String hexString = new String(hexChars);
			if (hexString.length() > 8) {
				hexString = hexString.substring(0, 8);
			}
			hash = Long.parseLong(hexString, 16);
		} catch (NoSuchAlgorithmException err) {
			hash = Math.abs(key.hashCode());
		}

		// Return hash number
		if (ringSize == null) {
			return hash;
		}
		return hash % ringSize;
	}

}