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
 * Moleculer error class for server error which is retryable.
 */
public class MoleculerServerError extends MoleculerRetryableError {

	// --- SERIAL VERSION UID ---

	private static final long serialVersionUID = -7399379980035209828L;

	// --- CONSTRUCTORS FOR LOCAL EXCEPTIONS ---

	public MoleculerServerError(String message, String nodeID, String type) {
		this(message, nodeID, type, null);
	}

	public MoleculerServerError(String message, String nodeID, String type, Tree data) {
		this(message, null, nodeID, type, data);
	}

	public MoleculerServerError(String message, Throwable cause, String nodeID, String type, Object... data) {
		super(message, cause, "MoleculerServerError", nodeID, 500, type, data);
	}

	// --- CONSTRUCTOR FOR REMOTE EXCEPTIONS ---

	public MoleculerServerError(Tree payload) {
		super(payload);
	}

}