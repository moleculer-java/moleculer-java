/**
 * This software is licensed under MIT license.<br>
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

import java.util.Map;

import services.moleculer.service.Name;
import services.moleculer.transporter.Transporter;

/**
 * Lowest CPU usage invocation strategy.
 * 
 * @see RoundRobinStrategy
 * @see NanoSecRandomStrategy
 * @see XORShiftRandomStrategy
 * @see SecureRandomStrategy
 */
@Name("Lowest CPU Usage Strategy")
public class CpuUsageStrategy<T extends Endpoint> extends XORShiftRandomStrategy<T> {

	// --- PROPERTIES ---
	
	protected final int maxTries;	
	protected final int lowCpuUsage;
	
	// --- COMPONENTS ---

	protected final Transporter transporter;

	// --- CONSTRUCTOR ---

	public CpuUsageStrategy(boolean preferLocal, int maxTries, int lowCpuUsage, Transporter transporter) {
		super(preferLocal);
		this.transporter = transporter;
		this.maxTries = maxTries;
		this.lowCpuUsage = lowCpuUsage;
	}

	// --- GET NEXT ENDPOINT ---

	@Override
	public Endpoint next(Endpoint[] array) {
		
		// Get the CPU usage map from the transporter
		Map<String, Long[]> activities = transporter.getNodeActivities();

		// Minimum values
		long minCPU = Long.MAX_VALUE;
		Endpoint minEndpoint = null;
		
		// Processing variables
		Endpoint endpoint;
		Long[] values;
		long cpuUsage;
		
		// Find the lower CPU usage in sample
		for (int i = 0; i < maxTries; i++) {
			
			// Get random endpoint
			endpoint = super.next(array);
			
			// Check CPU usage
			values = activities.get(endpoint.nodeID());
			if (values == null) {
				if (minEndpoint == null) {
					minEndpoint = endpoint;
				}
				continue;
			}
			cpuUsage = values[1];
			if (cpuUsage <= lowCpuUsage) {
				return endpoint;
			}
			if (cpuUsage < minCPU) {
				minCPU = cpuUsage;
				minEndpoint = endpoint;
			}			
		}
		return minEndpoint;
	}

}