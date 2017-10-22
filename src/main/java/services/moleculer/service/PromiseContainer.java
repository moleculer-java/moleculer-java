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
package services.moleculer.service;

import java.util.concurrent.atomic.AtomicBoolean;

import services.moleculer.Promise;

/**
 * Promise container of a pending remote invocation.
 */
final class PromiseContainer {

	// --- COMPLETE FLAG ---
	
	private final AtomicBoolean completed = new AtomicBoolean();
	
	// --- PROPERTIES ---
	
	final long created;
	final Promise promise;
	final long timeout;
	
	// --- CONSTRUCTOR ---
	
	PromiseContainer(Promise promise, long timeout) {
		this.promise = promise;
		this.timeout = timeout;
		if (timeout > 0) {
			created = System.currentTimeMillis();
		} else {
			created = 0;
		}
	}

	// --- THREAD-SAFE COMPLETE METHODS ---
	
	final void complete(Object value) {
		if (completed.compareAndSet(false, true)) {
			promise.complete(value);
		}
	}

	final void complete(Throwable error) {
		if (completed.compareAndSet(false, true)) {
			promise.complete(error);
		}
	}

}
