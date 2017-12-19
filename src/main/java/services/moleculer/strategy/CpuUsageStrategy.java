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

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

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
public class CpuUsageStrategy<T extends Endpoint> extends ArrayBasedStrategy<T> {

	// --- PROPERTIES ---

	protected final AtomicLong rnd = new AtomicLong(System.nanoTime());

	// --- COMPONENTS ---

	protected final Transporter transporter;

	// --- CONSTRUCTOR ---

	public CpuUsageStrategy(boolean preferLocal, Transporter transporter) {
		super(preferLocal);
		this.transporter = transporter;
	}

	// --- GET NEXT ENDPOINT ---

	@Override
	public Endpoint next(Endpoint[] array) {
		
		// Get the CPU usage map from the transporter
		Map<String, Long[]> activities = transporter.getNodeActivities();

		// List of the node indexes with the lowest CPU usage
		final ArrayList<Integer> indexes = new ArrayList<>(array.length + 1);

		// Find the list of nodes with the lowest CPU usage
		long min = Long.MAX_VALUE;
		Long[] values;
		long cpu;
		for (int i = 0; i < array.length; i++) {
			values = activities.get(array[i].nodeID());
			cpu = values == null ? 0 : values[1];
			if (cpu < min) {
				indexes.clear();
				indexes.add(i);
				min = cpu;
				continue;
			}
			if (cpu == min) {
				indexes.add(i);
			}
		}

		// Single result?
		if (indexes.size() == 1) {
			return array[indexes.get(0)];
		}

		// Generate pseudo random long (this is the fastest way to choose a
		// random endpoint index from the "indexes" array)
		long start, next;
		do {
			start = rnd.get();
			next = start + 1;
			next ^= (next << 21);
			next ^= (next >>> 35);
			next ^= (next << 4);
		} while (!rnd.compareAndSet(start, next));
		
		// Get a random index
		int index = (int) Math.abs(next % indexes.size());

		// Return ActionEndpoint
		return array[indexes.get(index)];
	}

}