/**
 * MOLECULER MICROSERVICES FRAMEWORK<br>
 * <br>
 * This project is based on the idea of Moleculer Microservices
 * Framework for NodeJS (https://moleculer.services). Special thanks to
 * the Moleculer's project owner (https://github.com/icebob) for the
 * consultations.<br>
 * <br>
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
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
import services.moleculer.transporter.Transporter;

/**
 * Lowest CPU usage invocation strategy.
 * 
 * @see RoundRobinStrategy
 * @see NanoSecRandomStrategy
 * @see XorShiftRandomStrategy
 * @see SecureRandomStrategy
 */
@Name("Lowest CPU Usage Strategy")
public class CpuUsageStrategy<T extends Endpoint> extends XorShiftRandomStrategy<T> {

	// --- PROPERTIES ---

	protected final int maxTries;
	protected final int lowCpuUsage;

	// --- COMPONENTS ---

	protected final Transporter transporter;

	// --- CONSTRUCTOR ---

	public CpuUsageStrategy(ServiceBroker broker, boolean preferLocal, int maxTries, int lowCpuUsage,
			Transporter transporter) {
		super(broker, preferLocal);
		this.transporter = transporter;
		this.maxTries = maxTries;
		this.lowCpuUsage = lowCpuUsage;
	}

	// --- GET NEXT ENDPOINT ---

	@Override
	public Endpoint next(Endpoint[] array) {

		// Minimum values
		long minCPU = Long.MAX_VALUE;
		Endpoint minEndpoint = null;

		// Processing variables
		Endpoint endpoint;
		int cpu;

		// Find the lower CPU usage in sample
		for (int i = 0; i < maxTries; i++) {

			// Get random endpoint
			endpoint = super.next(array);

			// Check CPU usage
			cpu = transporter.getCpuUsage(endpoint.getNodeID());
			if (cpu <= lowCpuUsage) {
				return endpoint;
			}
			if (minEndpoint == null || cpu < minCPU) {
				minCPU = cpu;
				minEndpoint = endpoint;
			}
		}
		return minEndpoint;
	}

}