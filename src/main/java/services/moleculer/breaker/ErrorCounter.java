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
	
	// --- CONSTRUCTOR ---

	public ErrorCounter(long windowLength, long lockTimeout, int maxErrors) {
		this.windowLength = windowLength;
		this.lockTimeout = lockTimeout;		
		this.timestamps = new long[maxErrors];
	}
	
	// --- INCREMENT ERROR COUNTER ---
	
	public synchronized void increment(long now) {
		pointer++;
		if (pointer >= timestamps.length) {
			pointer = 0;
		}
		min = timestamps[pointer];
		timestamps[pointer] = now;
		max = now;
	}

	// --- CHECK ENDPOINT STATUS ---
	
	public synchronized boolean isAvailable(long now) {
		if (max == 0) {
			return true;
		}
		if (now - max > lockTimeout) {
			if (now - tested > lockTimeout) {
				tested = now;
				return true;
			}
			return false;
		}
		return now - min <= windowLength;
	}
	
	// --- RESET VARIABLES ---
	
	public synchronized void reset() {
		max = 0;
	}
	
}