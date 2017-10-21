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
package services.moleculer.context;

import io.datatree.dom.Cache;

/**
 * 
 */
public class CallingOptions {

	// --- CONSTANTS ---

	public static final int DEFAULT_TIMEOUT = 0;
	public static final int DEFAULT_RETRY_COUNT = 0;

	// --- VARIABLES ---

	private final String nodeID;
	private final long timeout;
	private final int retryCount;
	private final Context parent;

	// --- CONSTRUTORS ---

	public CallingOptions(String nodeID, long timeout, int retryCount, Context parent) {
		this.nodeID = nodeID;
		this.timeout = timeout;
		this.retryCount = retryCount;
		this.parent = parent;
	}

	// --- STATIC CONSTRUCTORS ---

	private static final Cache<String, CallingOptions> cache = new Cache<>(1024, true);

	public static final CallingOptions get(long timeout) {
		return get(null, timeout, DEFAULT_RETRY_COUNT);
	}

	public static final CallingOptions get(long timeout, int retryCount) {
		return get(null, timeout, retryCount);
	}

	public static final CallingOptions get(String nodeID) {
		return get(nodeID, DEFAULT_TIMEOUT, DEFAULT_RETRY_COUNT);
	}

	public static final CallingOptions get(String nodeID, long timeout) {
		return get(nodeID, timeout, DEFAULT_RETRY_COUNT);
	}

	public static final CallingOptions get(String nodeID, long timeout, int retryCount) {
		String key = nodeID + '.' + timeout + '.' + retryCount;
		CallingOptions opts = cache.get(key);
		if (opts == null) {
			opts = new CallingOptions(nodeID, timeout, retryCount, null);
			cache.put(key, opts);
		}
		return opts;
	}

	public static final CallingOptions get(String nodeID, long timeout, int retryCount, Context parent) {
		return new CallingOptions(nodeID, timeout, retryCount, parent);
	}

	// --- VARIABLE GETTERS ---

	public final String nodeID() {
		return nodeID;
	}

	public final long timeout() {
		return timeout;
	}

	public final int retryCount() {
		return retryCount;
	}

	public final Context parent() {
		return parent;
	}

}