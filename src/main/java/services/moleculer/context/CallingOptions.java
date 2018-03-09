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
package services.moleculer.context;

/**
 * Calling options (socketTimeout, target nodeID, number of retries). Usage:<br>
 * <pre>
 * broker.call("math.add", "a", 3, "b", 5,
 *   CallingOptions.nodeID("node-2")).then(in -> {
 *   ...
 * });
 * </pre>
 * You can enumerate options in builder style:<br>
 * <pre>
 * CallingOptions.nodeID("node-2").timeout(5).retryCount(3);
 * </pre>
 */
public class CallingOptions {

	// --- PROPERTIES ---

	public static class Options {

		public final String nodeID;
		public final int timeout;
		public final int retryCount;

		// --- CONSTRUCTOR ---

		protected Options(String nodeID, int timeout, int retryCount) {
			this.nodeID = nodeID;
			this.timeout = timeout;
			this.retryCount = retryCount;
		}

		// --- VARIABLE SETTERS ---

		public Options nodeID(String nodeID) {
			return new Options(nodeID, timeout, retryCount);
		}

		public Options timeout(int timeout) {
			return new Options(nodeID, timeout, retryCount);
		}

		public Options retryCount(int retryCount) {
			return new Options(nodeID, timeout, retryCount);
		}

	}

	// --- HIDDEN CONSTRUTOR ---

	protected CallingOptions() {
	}

	// --- STATIC BUILDER-LIKE CONSTRUTOR ---

	public static Options nodeID(String nodeID) {
		return new Options(nodeID, 0, 0);
	}

	public static Options timeout(int timeout) {
		return new Options(null, timeout, 0);
	}

	public static Options retryCount(int retryCount) {
		return new Options(null, 0, retryCount);
	}

}