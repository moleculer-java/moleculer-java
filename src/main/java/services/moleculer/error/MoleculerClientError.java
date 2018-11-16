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

import static services.moleculer.error.MoleculerErrorUtils.MOLECULER_CLIENT_ERROR;

import io.datatree.Tree;

/**
 * Moleculer error class for client errors which is not retryable.
 */
public class MoleculerClientError extends MoleculerError {

	// --- SERIAL VERSION UID ---

	private static final long serialVersionUID = -3748468322324813894L;

	// --- CONSTRUCTORS FOR LOCAL EXCEPTIONS ---

	public MoleculerClientError(String message, String nodeID) {
		this(message, null, nodeID, null);
	}

	public MoleculerClientError(String message, Throwable cause, String nodeID, Tree data) {
		this(message, cause, nodeID, 400, "CLIENT_ERROR", data);
	}

	public MoleculerClientError(String message, Throwable cause, String nodeID, int code, String type, Tree data) {
		this(message, cause, MOLECULER_CLIENT_ERROR, nodeID, code, type, data);
	}

	public MoleculerClientError(String message, Throwable cause, String name, String nodeID, int code, String type,
			Object... data) {
		super(message, cause, name, nodeID, false, code, type, data);
	}

	// --- CONSTRUCTOR FOR REMOTE EXCEPTIONS ---

	public MoleculerClientError(Tree payload) {
		super(payload);
	}

}