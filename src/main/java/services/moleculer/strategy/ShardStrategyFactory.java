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
	
	protected String shardKey;
	protected int vnodes = 10;
	protected Integer ringSize;
	protected int cacheSize = 1024;
	
	// --- CONSTRUCTORS ---

	public ShardStrategyFactory() {
		super(false);
	}

	public ShardStrategyFactory(boolean preferLocal) {
		super(preferLocal);
	}

	// --- FACTORY METHOD ---

	@Override
	public <T extends Endpoint> Strategy<T> create() {
		return new ShardStrategy<T>(broker, preferLocal, shardKey, vnodes, ringSize, cacheSize);
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
	
}