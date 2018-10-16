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

import static services.moleculer.error.MoleculerErrorUtils.BROKER_OPTIONS_ERROR;

import io.datatree.Tree;

/**
 * Custom Moleculer exception class for broker option errors.
 */
public class BrokerOptionsError extends MoleculerError {

	// --- SERIAL VERSION UID ---
	
	private static final long serialVersionUID = 6016262167903015550L;
	
	// --- CONSTRUCTOR FOR LOCAL EXCEPTIONS ---
	
	public BrokerOptionsError(String message, String nodeID, Object... data) {
		super(message, null, BROKER_OPTIONS_ERROR, nodeID, false, 500, "BROKER_OPTIONS_ERROR", data);
	}

	// --- CONSTRUCTOR FOR REMOTE EXCEPTIONS ---
	
	public BrokerOptionsError(Tree payload) {
		super(payload);
	}

}