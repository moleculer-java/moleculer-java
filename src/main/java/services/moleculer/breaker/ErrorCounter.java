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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ErrorCounter {
	
	// --- PROPERTIES ---

	protected final long windowLength;
	protected final long lockTimeout;
	protected final int maxErrors;
	
	// --- ERROR TIMESTAMPS ---

	protected final AtomicInteger errorCounter = new AtomicInteger();
	protected final AtomicLong lastError = new AtomicLong();
	protected final AtomicLong lastTested = new AtomicLong();
	
	// --- CONSTRUCTOR ---

	protected ErrorCounter(long windowLength, long lockTimeout, int maxErrors) {
		this.windowLength = windowLength;
		this.lockTimeout = lockTimeout;
		this.maxErrors = maxErrors;
	}

	// --- INCREMENT ERROR COUNTER ---

	protected void increment(long now) {
		errorCounter.incrementAndGet();
		lastError.set(now);
	}

	// --- FOR CLEANUP ---

	protected boolean isEmpty() {
		return errorCounter.get() == 0;
	}

	// --- CHECK ENDPOINT STATUS ---

	protected boolean isAvailable(long now) {
		int errors = errorCounter.get();
		if (errors < maxErrors) {
			return true;
		}
		long last = lastError.get();
		if (now - last <= lockTimeout) {
			return false;
		}
		long tested = lastTested.get();
		if (now - tested <= lockTimeout) {
			return false;
		}
		lastTested.set(now);
		return true;
	}

	// --- RESET VARIABLES ---

	protected void reset() {
		errorCounter.set(0);
	}

}