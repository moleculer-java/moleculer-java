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

import static services.moleculer.error.MoleculerErrorFactory.PROTOCOL_VERSION_MISMATCH_ERROR;

import io.datatree.Tree;

/**
 * Protocol version mismatch exception.
 */
public class ProtocolVersionMismatchError extends MoleculerError {

	// --- SERIAL VERSION UID ---

	private static final long serialVersionUID = -7734103095126608077L;

	// --- PROPERTIES ---

	protected final String actual;

	protected final String received;

	// --- CONSTRUCTOR FOR LOCAL EXCEPTIONS ---

	public ProtocolVersionMismatchError(String nodeID, String actual, String received) {
		super("Protocol version mismatch.", null, PROTOCOL_VERSION_MISMATCH_ERROR, nodeID, false, 500,
				"PROTOCOL_VERSION_MISMATCH", "actual", actual, "received", received);
		this.actual = actual;
		this.received = received;
	}

	// --- CONSTRUCTOR FOR REMOTE EXCEPTIONS ---

	public ProtocolVersionMismatchError(Tree payload) {
		super(payload);
		this.actual = payload.get("actual", "unknown");
		this.received = payload.get("received", "unknown");
	}

	// --- PROPERTY GETTERS ---

	public String getActual() {
		return actual;
	}

	public String getReceived() {
		return received;
	}

}