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

public class EndpointStatus {

	// --- STATUS ---
	
	protected static final int STATUS_OPEN = 0;
	protected static final int STATUS_CLOSED = 1;
	protected static final int STATUS_HALF_OPEN = 2;
	
	// --- PROPERTIES ---
	
	protected final long windowSize;
	protected final int maxErrors;
	
	protected final AtomicLong errorTimestamp = new AtomicLong();
	protected final AtomicInteger errorCount = new AtomicInteger();
	protected final AtomicInteger status = new AtomicInteger();
	
	// --- CONSTRUCTOR ---
	
	public EndpointStatus(long windowSize, int maxErrors) {
		this.windowSize = windowSize;
		this.maxErrors = maxErrors;
	}
		
	public boolean isAvailable() {
		return status.get() != STATUS_CLOSED;
	}

	public void onError() {
		
	}
	
	public boolean isRemovable() {		
		return false;
	}
	
}