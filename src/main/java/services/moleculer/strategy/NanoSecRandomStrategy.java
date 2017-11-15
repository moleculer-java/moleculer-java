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

import services.moleculer.service.Name;

/**
 * Nanosec-based pseudorandom invocation strategy.
 * 
 * @see RoundRobinStrategy
 * @see SecureRandomStrategy
 * @see XORShiftRandomStrategy
 */
@Name("Nanosecond-based Pseudorandom Strategy")
public final class NanoSecRandomStrategy<T extends Endpoint> extends ArrayBasedStrategy<T> {

	// --- CONSTRUCTOR ---

	public NanoSecRandomStrategy(boolean preferLocal) {
		super(preferLocal);
	}

	// --- GET NEXT ENDPOINT ---

	@Override
	public final Endpoint next() {
		return endpoints[(int) (System.nanoTime() % endpoints.length)];
	}

}