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

import static services.moleculer.util.CommonUtils.parseParams;

import io.datatree.Tree;

/**
 * Custom Moleculer Exception class.
 */
public class MoleculerException extends Exception {

	// --- SERIAL VERSION UID ---
	
	private static final long serialVersionUID = 2592696425280724955L;

	// --- PROPERTIES ---
	
	protected final boolean retryable;
	
	protected final int code;
	
	protected final String type;
	
	protected final Tree data;
	
	// --- CONSTRUCTOR ---
	
	public MoleculerException(String message, Throwable cause, boolean retryable, int code, String type, Object... data) {
		super(message, cause);
		this.retryable = retryable;
		this.code = code < 1 ? 500 : code;
		this.type = type;
		this.data = parseParams(data).data;
	}
	
	// --- PROPERTY GETTERS ---

	public boolean isRetryable() {
		return retryable;
	}

	public int getCode() {
		return code;
	}

	public String getType() {
		return type;
	}

	public Tree getData() {
		return data;
	}
	
}