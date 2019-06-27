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

import static services.moleculer.error.MoleculerErrorUtils.MOLECULER_ERROR;
import static services.moleculer.util.CommonUtils.parseParams;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import io.datatree.Tree;
import services.moleculer.util.FastBuildTree;

/**
 * Custom Moleculer Exception class.
 */
public class MoleculerError extends RuntimeException {

	// --- SERIAL VERSION UID ---

	private static final long serialVersionUID = 2592696425280724955L;

	// --- PROPERTIES ---

	protected final String stack;

	protected final String name;

	protected final String nodeID;

	protected final boolean retryable;

	protected final int code;

	protected final String type;

	protected final Tree data;

	// --- LOCAL / REMOTE STACK TRACE ---

	protected String stackTrace;

	// --- CONSTRUCTORS FOR LOCAL EXCEPTIONS ---

	public MoleculerError(String message, String nodeID, boolean retryable) {
		this(message, nodeID, retryable, 500);
	}

	public MoleculerError(String message, String nodeID, boolean retryable, int code) {
		this(message, nodeID, retryable, code, null);
	}

	public MoleculerError(String message, String nodeID, boolean retryable, int code, Tree data) {
		this(message, null, nodeID, retryable, code, "MOLECULER_ERROR", data);
	}

	public MoleculerError(String message, String nodeID, boolean retryable, int code, String type, Tree data) {
		this(message, null, nodeID, retryable, code, type, data);
	}

	public MoleculerError(String message, Throwable cause, String nodeID, boolean retryable, int code, String type,
			Tree data) {
		this(message, cause, MOLECULER_ERROR, nodeID, retryable, code, type, data);
	}

	public MoleculerError(String message, Throwable cause, String name, String nodeID, boolean retryable, int code,
			String type, Object... data) {
		super(message, cause);
		this.stack = null;
		this.name = name;
		this.nodeID = nodeID;
		this.retryable = retryable;
		this.code = code;
		this.type = type;
		this.data = parseParams(data).data;
	}

	// --- CONSTRUCTOR FOR REMOTE EXCEPTIONS ---

	public MoleculerError(Tree payload) {
		super(payload.get("message", "Unexpected exception occured!"));
		this.stack = payload.get("stack", "no stack");
		this.name = payload.get("name", (String) null);
		this.nodeID = payload.get("nodeID", "unknown");
		this.retryable = payload.get("retryable", true);
		this.code = payload.get("code", 500);
		this.type = payload.get("type", "UNKNOWN_ERROR");
		this.data = payload.get("data");
	}

	// --- STACK TRACE ---

	@Override
	public void printStackTrace() {
		if (stack == null) {
			super.printStackTrace();
		} else {
			System.err.println(getStack());
		}
	}

	@Override
	public void printStackTrace(PrintWriter s) {
		if (stack == null) {
			super.printStackTrace(s);
		} else {
			s.println(getStack());
		}
	}

	@Override
	public void printStackTrace(PrintStream s) {
		if (stack == null) {
			super.printStackTrace(s);
		} else {
			s.println(getStack());
		}
	}

	// --- CONVERT TO JSON ---

	public Tree toTree() {
		FastBuildTree tree = new FastBuildTree(8);
		toTree(tree);
		return tree;
	}

	public void toTree(Tree target) {
		target.put("name", name);
		target.put("message", getMessage());
		target.put("nodeID", nodeID);
		target.put("code", code);
		target.put("type", type);
		target.put("retryable", retryable);
		target.put("stack", getStack());
		target.putObject("data", data);
	}

	// --- LOCAL / REMOTE STACK TRACE ---

	public String getStack() {
		if (stackTrace == null) {
			if (stack == null) {
				StringWriter sw = new StringWriter(512);
				PrintWriter pw = new PrintWriter(sw, true);
				Throwable cause = getCause();
				if (cause == null) {
					super.printStackTrace(pw);
				} else {
					cause.printStackTrace(pw);
				}
				stackTrace = sw.toString().trim();
			} else {
				stackTrace = stack.trim();
			}
		}
		return stackTrace;
	}

	// --- PROPERTY GETTERS ---

	public boolean isRemote() {
		return stack != null;
	}

	public String getName() {
		return name;
	}

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