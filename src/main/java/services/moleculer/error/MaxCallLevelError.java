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

import static services.moleculer.error.MoleculerErrorUtils.MAX_CALL_LEVEL_ERROR;

import io.datatree.Tree;

/**
 * 'Max request call level' error message.
 */
public class MaxCallLevelError extends MoleculerError {

	// --- SERIAL VERSION UID ---

	private static final long serialVersionUID = -1748608950886915972L;

	// --- PROPERTIES ---

	protected final int level;

	// --- CONSTRUCTOR FOR LOCAL EXCEPTIONS ---

	public MaxCallLevelError(String nodeID, int level) {
		super("Request level is reached, the limit is '" + level + "' on '" + nodeID + "' node.", null,
				MAX_CALL_LEVEL_ERROR, nodeID, false, 500, "MAX_CALL_LEVEL", "level", level);
		this.level = level;
	}

	// --- CONSTRUCTOR FOR REMOTE EXCEPTIONS ---
	
	public MaxCallLevelError(Tree payload) {
		super(payload);
		this.level = data == null ? -1 : data.get("level", -1);
	}
	
	// --- PROPERTY GETTERS ---

	public int getLevel() {
		return level;
	}

}