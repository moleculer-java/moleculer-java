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

	// --- STATUS CODES ---

	protected enum Status {
		STATUS_OPENED, STATUS_HALF_OPENED, STATUS_CLOSED
	}

	// --- PROPERTIES ---

	protected final long windowLength;
	protected final long lockTimeout;
	
	protected final int lastPos;

	// --- ERROR TIMESTAMPS ---

	protected final long[] errors;
	
	// --- CONSTRUCTOR ---

	protected ErrorCounter(long windowLength, long lockTimeout, int maxErrors) {
		this.windowLength = windowLength;
		this.lockTimeout = lockTimeout;
		this.lastPos = maxErrors - 1;
		
		this.errors = new long[maxErrors];
	}

	// --- INCREMENT/DECREMENT ERROR COUNTER ---

	protected synchronized void onError(long now) {
		if (errors.length > 1) {
			System.arraycopy(errors, 1, errors, 0, lastPos);
		}
		errors[lastPos] = now;
	}

	protected synchronized void onSuccess() {
		errors[0] = 0;
	}
	
	// --- FOR CLEANUP ---

	protected synchronized boolean canRemove(long now) {
		for (long time: errors) {
			if (now - time <= windowLength) {
				return false;
			}
		}
		return now - errors[lastPos] > lockTimeout;
	}

	// --- CHECK ENDPOINT STATUS ---

	protected synchronized Status getStatus(long now) {
		int count = 0;
		for (long time: errors) {
			if (now - time <= windowLength) {
				count++;
			}
		}
		if (count <= lastPos) {
			return Status.STATUS_OPENED;
		}
		if (now - errors[lastPos] <= lockTimeout) {
			return Status.STATUS_CLOSED;
		}
		return Status.STATUS_HALF_OPENED;
	}
	
	protected boolean isAvailable(long now) {
		return getStatus(now) != Status.STATUS_CLOSED;
	}
	
	// --- TESTING ---
	
	protected synchronized int getErrorCounter(long now) {
		int counter = 0;
		for (long time: errors) {
			if (now - time <= windowLength) {
				counter++;
			}
		}
		return counter;
	}

	protected synchronized long getLastError() {
		long max = 0;
		for (long time: errors) {
			if (time > max) {
				max = time;
			}
		}
		return max;
	}
	
}