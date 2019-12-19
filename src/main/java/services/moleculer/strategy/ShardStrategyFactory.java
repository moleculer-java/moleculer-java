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
import java.util.function.Function;

import services.moleculer.service.Endpoint;
import services.moleculer.service.Name;

/**
 * Sharding invocation strategy factory. Using consistent-hashing.<br>
 * More info: https://www.toptal.com/big-data/consistent-hashing
 * 
 * @see RoundRobinStrategyFactory
 * @see SecureRandomStrategyFactory
 * @see XorShiftRandomStrategyFactory
 * @see NanoSecRandomStrategyFactory
 * @see CpuUsageStrategyFactory
 * @see NetworkLatencyStrategyFactory
 */
@Name("Shard Strategy Factory")
public class ShardStrategyFactory extends ArrayBasedStrategyFactory {

	// --- PROPERTIES ---

	/**
	 * Shard key's path (eg. "userID", "user.email", "#key", etc.)
	 */
	protected String shardKey;

	/**
	 * Number of virtual nodes
	 */
	protected int vnodes = 10;

	/**
	 * Ring size (optional)
	 */
	protected Integer ringSize;

	/**
	 * Size of the memory cache (0 = disabled)
	 */
	protected int cacheSize = 1024;

	// --- HASHER ---

	/**
	 * Default hash function (MD5).
	 */
	protected Function<String, Long> hash = new Function<String, Long>() {

		private final char[] HEX = "0123456789abcdef".toCharArray();
		private final ThreadLocal<MessageDigest> hashers = new ThreadLocal<>();

		@Override
		public Long apply(String key) {
			byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
			MessageDigest hasher = hashers.get();
			if (hasher == null) {
				try {
					hasher = MessageDigest.getInstance("MD5");
				} catch (NoSuchAlgorithmException notFound) {
					throw new RuntimeException(notFound);
				}
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
			return Long.parseLong(hexString, 16);
		}

	};

	// --- CONSTRUCTORS ---

	/**
	 * Constructor that calls for service based primarily on its own
	 * (hash-based) logic. Does not matter if the service is available locally.
	 */
	public ShardStrategyFactory() {
		super(false);
	}

	/**
	 * Constructor that can be configured to use local services if possible.
	 * 
	 * @param preferLocal
	 *            invoke local actions if possible
	 */
	public ShardStrategyFactory(boolean preferLocal) {
		super(preferLocal);
	}

	/**
	 * Constructor that can be configured to use local services if possible.
	 * 
	 * @param shardKey
	 *            Shard key's path (eg. "userID", "user.email", "#key", etc.)
	 */
	public ShardStrategyFactory(String shardKey) {
		this(false, shardKey);
	}
	
	/**
	 * Constructor that can be configured to use local services if possible.
	 * 
	 * @param preferLocal
	 *            invoke local actions if possible
	 * @param shardKey
	 *            Shard key's path (eg. "userID", "user.email", "#key", etc.)
	 */
	public ShardStrategyFactory(boolean preferLocal, String shardKey) {
		super(preferLocal);
		setShardKey(shardKey);
	}

	// --- FACTORY METHOD ---

	@Override
	public <T extends Endpoint> Strategy<T> create() {
		return new ShardStrategy<T>(broker, preferLocal, shardKey, vnodes, ringSize, cacheSize, hash);
	}

	// --- GETTERS AND SETTERS ---

	public String getShardKey() {
		return shardKey;
	}

	public void setShardKey(String shardKey) {
		this.shardKey = shardKey;
	}

	public int getVnodes() {
		return vnodes;
	}

	public void setVnodes(int vnodes) {
		this.vnodes = vnodes;
	}

	public Integer getRingSize() {
		return ringSize;
	}

	public void setRingSize(Integer ringSize) {
		this.ringSize = ringSize;
	}

	public int getCacheSize() {
		return cacheSize;
	}

	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	public Function<String, Long> getHash() {
		return hash;
	}

	public void setHash(Function<String, Long> hash) {
		this.hash = hash;
	}

}