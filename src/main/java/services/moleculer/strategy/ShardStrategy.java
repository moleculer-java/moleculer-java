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

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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

	// --- PROPERTIES ---

	/**
	 * Shard key's path (eg. "userID", "user.email", etc.)
	 */
	protected final String shardKey;

	/**
	 * Number of virtual nodes
	 */
	protected final int vnodes;

	/**
	 * Ring size (optional)
	 */
	protected final Integer ringSize;

	/**
	 * Size of the memory cache (0 = disabled)
	 */
	protected final boolean useMeta;

	/**
	 * Size of the memory cache (0 = disabled)
	 */
	protected final int cacheSize;

	// --- RING ---

	/**
	 * The "hash ring" (https://www.toptal.com/big-data/consistent-hashing)
	 */
	protected final AtomicReference<Ring> ringRef = new AtomicReference<>();

	// --- HASHER ---

	/**
	 * Hasher function (generated hash is 32 bits long, default method is MD5)
	 */
	protected final Function<String, Long> hash;

	// --- CONSTRUCTOR ---

	public ShardStrategy(ServiceBroker broker, boolean preferLocal, String shardKey, int vnodes, Integer ringSize,
			int cacheSize, Function<String, Long> hash) {
		super(broker, preferLocal);

		// Set properties
		if (shardKey != null && shardKey.startsWith("#")) {
			this.shardKey = shardKey.substring(1);
			useMeta = true;
		} else {
			this.shardKey = shardKey == null || shardKey.isEmpty() ? null : shardKey;
			useMeta = false;
		}
		this.vnodes = vnodes < 1 ? 1 : vnodes;
		this.ringSize = ringSize;
		this.cacheSize = cacheSize;
		this.hash = hash;

		// Init Ring
		rebuild();
	}

	// --- ADD A LOCAL OR REMOTE ENDPOINT ---

	@Override
	public void addEndpoint(T endpoint) {
		super.addEndpoint(endpoint);
		rebuild();
	}

	// --- REMOVE ALL ENDPOINTS OF THE SPECIFIED NODE ---

	@Override
	public boolean remove(String nodeID) {
		boolean removed = super.remove(nodeID);
		if (removed) {
			rebuild();
		}
		return removed;
	}

	// --- GET NEXT ENDPOINT ---

	@Override
	public Endpoint next(Context ctx, Endpoint[] array) {
		if (shardKey != null) {
			String key = getKeyFromContext(ctx);
			if (key != null) {

				// Get current ring
				Ring ring = ringRef.get();

				// Get from cache
				Endpoint next;
				if (ring.cache != null) {
					next = ring.cache.get(key);
					if (next != null) {
						return next;
					}
				}

				// Calculate hash number
				long hashNum = hash.apply(key);
				if (ring.ringSize > -1) {
					hashNum %= ring.ringSize;
				}

				// Find endpoint
				long[][] limits = new long[array.length][];
				for (int i = 0; i < array.length; i++) {
					limits[i] = ring.limitMap.get(array[i]);
				}
				for (int j = 0; j < vnodes; j++) {
					for (int i = 0; i < array.length; i++) {
						if (limits[i] == null) {
							continue;
						}
						if (hashNum <= limits[i][j]) {
							next = array[i];
							if (ring.cache != null) {
								ring.cache.put(key, next);
							}
							return next;
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

		// Check ringSize
		Ring ring = new Ring(copy.length, vnodes, ringSize, cacheSize);
		
		// Calculate
		int total = copy.length * vnodes;
		long size = ring.ringSize > -1 ? ring.ringSize : (long) Math.pow(2, 32);
		double slice = size / (double) total;
		
		// Build ring
		long index = 1;
		for (int j = 0; j < vnodes; j++) {
			for (int i = 0; i < copy.length; i++) {
				long[] limits = ring.limitMap.get(copy[i]);
				if (limits == null) {
					limits = new long[vnodes];
					ring.limitMap.put(copy[i], limits);
				}
				if (j == vnodes - 1 && i == copy.length - 1) {
					limits[j] = size;
				} else {
					limits[j] = (long) (slice * (index++));
				}
			}
		}

		// Store the new ring (clear cache)
		ringRef.set(ring);
	}

	// --- RING ---

	protected static class Ring {

		// --- VARIABLES ---
		/**
		 * The "hash ring" (https://www.toptal.com/big-data/consistent-hashing)
		 */
		protected HashMap<Endpoint, long[]> limitMap;

		/**
		 * Actual/minimum ring size (or -1, if not set)
		 */
		protected final int ringSize;

		/**
		 * Accelerator cache
		 */
		protected final Cache<String, Endpoint> cache;

		// --- CONSTRUCTOR ---

		protected Ring(int nodes, int vnodes, Integer ringSize, int cacheSize) {
			if (ringSize == null || nodes < 1) {
				this.ringSize = -1;
			} else {
				this.ringSize = Math.max(ringSize, vnodes * nodes);
			}
			cache = cacheSize < 1 ? null : new Cache<>(cacheSize);
			limitMap = new HashMap<>(this.ringSize == -1 ? 512 : this.ringSize * 2);
		}

	}

}