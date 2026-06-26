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
package services.moleculer.strategy;

import services.moleculer.ServiceBroker;
import services.moleculer.context.Context;
import services.moleculer.service.Endpoint;
import services.moleculer.service.Name;

/**
 * Nanosec-based pseudorandom invocation strategy.
 *
 * @see RoundRobinStrategy
 * @see SecureRandomStrategy
 * @see XorShiftRandomStrategy
 * @see CpuUsageStrategy
 * @see NetworkLatencyStrategy
 * @see ShardStrategy
 */
@Name("Nanosecond-based Pseudorandom Strategy")
public class NanoSecRandomStrategy<T extends Endpoint> extends ArrayBasedStrategy<T> {

	// --- CONSTRUCTOR ---

	public NanoSecRandomStrategy(ServiceBroker broker, boolean preferLocal) {
		super(broker, preferLocal);
	}

	// --- GET NEXT ENDPOINT ---

	@Override
	public Endpoint next(Context ctx, Endpoint[] array) {

		// Mix the nanosecond clock with the SplitMix64 finalizer so the
		// quasi-regular, low-entropy lower bits of System.nanoTime() avalanche
		// into a uniformly distributed index. The previous Long.hashCode() fold
		// kept consecutive draws strongly correlated, which on some platforms
		// (e.g. Windows/JDK 25) skewed the distribution badly enough that some
		// endpoints were never selected.
		long z = System.nanoTime() * 0x9E3779B97F4A7C15L;
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		z = z ^ (z >>> 31);

		// floorMod yields a non-negative index without the Math.abs(MIN_VALUE) trap.
		return array[(int) Math.floorMod(z, array.length)];
	}

}