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
import services.moleculer.service.Endpoint;
import services.moleculer.service.Name;

/**
 * Lowest network latency strategy.
 * 
 * @see RoundRobinStrategy
 * @see NanoSecRandomStrategy
 * @see XorShiftRandomStrategy
 * @see SecureRandomStrategy
 * @see CpuUsageStrategy
 */
@Name("Lowest Network Latency Strategy")
public class NetworkLatencyStrategy<T extends Endpoint> extends XorShiftRandomStrategy<T> {

	// --- PROPERTIES ---

	protected final int maxTries;

	// --- COMPONENTS ---

	protected final NetworkLatencyStrategyFactory factory;

	// --- CONSTRUCTOR ---

	public NetworkLatencyStrategy(ServiceBroker broker, boolean preferLocal, int maxTries,
			NetworkLatencyStrategyFactory factory) {
		super(broker, preferLocal);
		this.maxTries = maxTries;
		this.factory = factory;
	}

	// --- GET NEXT ENDPOINT ---

	@Override
	public Endpoint next(Endpoint[] array) {

		// Minimum values
		long minResponseTime = Long.MAX_VALUE;
		Endpoint minEndpoint = null;

		// Processing variables
		Endpoint endpoint;
		long responseTime;

		// Find the lower response time
		for (int i = 0; i < maxTries; i++) {

			// Get random endpoint
			endpoint = super.next(array);
			
			// Check response time
			responseTime = factory.getResponseTime(endpoint.getNodeID());
			if (minEndpoint == null || responseTime < minResponseTime) {
				minResponseTime = responseTime;
				minEndpoint = endpoint;
			}
		}
		return minEndpoint;
	}

}