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
package services.moleculer.error;

import io.datatree.Tree;

/**
 * Throw it if your nested call is skipped because the execution is timed out
 * (error code: 514, retriable: false).
 */
public class RequestSkipped extends MoleculerException {

	// --- SERIAL VERSION UID ---

	private static final long serialVersionUID = -7442358361101938234L;

	// --- PROPERTIES ---
	
	protected final String action;
	protected final String nodeID;
	
	// --- CONSTRUCTORS ---

	public RequestSkipped(String type, Tree data, String action, String nodeID) {
		super(false, "Request skipped (" + action + " on " + nodeID + ")!", 514, type, data);
		this.action = action;
		this.nodeID = nodeID;
	}

	// --- PROPERTY GETTERS ---
	
	public String getAction() {
		return action;
	}

	public String getNodeID() {
		return nodeID;
	}
	
}