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

/**
 * Calling options (timeout, target nodeID, number of retries).
 */
public final class CallingOptions {

	// --- PROPERTIES ---

	public static final class Options {
	
		private String nodeID;
		private int timeout;
		private int retryCount;

		// --- VARIABLE SETTERS ---

		public final Options nodeID(String nodeID) {
			this.nodeID = nodeID;
			return this;
		}

		public final Options timeout(int timeout) {
			this.timeout = timeout;
			return this;
		}

		public final Options retryCount(int retryCount) {
			this.retryCount = retryCount;
			return this;
		}
		
		// --- VARIABLE GETTERS ---

		public final String nodeID() {
			return nodeID;
		}

		public final int timeout() {
			return timeout;
		}

		public final int retryCount() {
			return retryCount;
		}
		
	}
	
	// --- PRIVATE CONSTRUTOR ---

	private CallingOptions() {
	}

	// --- STATIC BUILDER-LIKE CONSTRUTOR ---
	
	public static final Options nodeID(String nodeID) {
		return new Options().nodeID(nodeID);
	}

	public static final Options timeout(int timeout) {
		return new Options().timeout(timeout);
	}

	public static final Options retryCount(int retryCount) {
		return new Options().retryCount(retryCount);
	}

}