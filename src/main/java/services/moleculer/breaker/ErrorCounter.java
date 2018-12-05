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
package services.moleculer.breaker;

public class ErrorCounter {

	// --- PROPERTIES ---

	protected final long windowLength;
	protected final long lockTimeout;

	// --- ERROR TIMESTAMPS ---

	protected final long[] timestamps;
	protected volatile int pointer;

	protected volatile long max;
	protected volatile long min;

	protected volatile long tested;

	protected volatile boolean locked;

	// --- CONSTRUCTOR ---

	protected ErrorCounter(long windowLength, long lockTimeout, int maxErrors) {
		this.windowLength = windowLength;
		this.lockTimeout = lockTimeout;
		this.timestamps = new long[maxErrors];
	}

	// --- INCREMENT ERROR COUNTER ---

	protected synchronized void increment(long now) {
		pointer++;
		if (pointer >= timestamps.length) {
			pointer = 0;
		}
		timestamps[pointer] = now;
		max = now;

		int next = pointer + 1;
		if (next >= timestamps.length) {
			next = 0;
		}
		min = timestamps[next];
	}

	// --- FOR CLEANUP ---
	
	protected synchronized boolean isEmpty() {
		return (max == 0 || min == 0);
	}
	
	// --- CHECK ENDPOINT STATUS ---

	protected synchronized boolean isAvailable(long now) {
		if (max == 0 || min == 0) {
			return true;
		}
		if (now - max > lockTimeout) {
			if (now - tested > lockTimeout) {
				tested = now;
				return true;
			}
			return false;
		}
		if (locked) {
			return false;
		}
		if (now - min > windowLength) {
			return true;
		}
		locked = true;
		return false;
	}

	// --- RESET VARIABLES ---

	protected synchronized void reset() {
		if (max == 0) {
			return;
		}
		for (int i = 0; i < timestamps.length; i++) {
			timestamps[i] = 0;
		}
		min = 0;
		max = 0;
		tested = 0;
		locked = false;
	}

}