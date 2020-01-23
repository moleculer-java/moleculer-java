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
package services.moleculer.uid;

import java.util.concurrent.atomic.AtomicLong;

import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * XORSHIFT-based pseudorandom UID generator.
 *
 * @see IncrementalUidGenerator
 * @see StandardUidGenerator
 */
@Name("XORSHIFT Pseudorandom UID Generator")
public class XorShiftRandomUidGenerator extends UidGenerator {

	// --- HOST/NODE PREFIX ---

	/**
	 * UID prefix (null = nodeID)
	 */
	protected char[] prefix = new char[0];
	
	// --- PROPERTIES ---

	protected final AtomicLong rnd = new AtomicLong(System.nanoTime());
	protected final AtomicLong counter = new AtomicLong();
	
	// --- START GENERATOR ---

	/**
	 * Initializes UID generator instance.
	 *
	 * @param broker
	 *            parent ServiceBroker
	 */
	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);
		if (prefix == null || prefix.length ==  0) {
			prefix = (broker.getNodeID() + ':').toCharArray();
		}
	}
	
	// --- GENERATE UID ---

	@Override
	public String nextUID() {
		StringBuilder tmp = new StringBuilder(prefix.length + 32);
		
		// Add prefix
		tmp.append(prefix);
		
		// Add sequence and random number
		tmp.append(counter.incrementAndGet());
		tmp.append(':');
		tmp.append(nextLong());
		
		return tmp.toString();
	}

	protected long nextLong() {
		long start;
		long next;
		do {
			start = rnd.get();
			next = start + 1;
			next ^= (next << 21);
			next ^= (next >>> 35);
			next ^= (next << 4);
		} while (!rnd.compareAndSet(start, next));
		return Math.abs(next);
	}
	
	// --- GETTERS / SETTERS ---

	public String getPrefix() {
		return new String(prefix);
	}

	public void setPrefix(String prefix) {
		if (prefix != null && !prefix.isEmpty()) {
			this.prefix = prefix.toCharArray();
		}
	}
	
}