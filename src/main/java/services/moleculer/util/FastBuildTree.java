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
package services.moleculer.util;

import io.datatree.Tree;

/**
 * This Tree is optimized for fast building and serialization.
 */
public class FastBuildTree extends Tree {

	// --- SERIAL VERSION UID ---
	
	private static final long serialVersionUID = 8897557340559995525L;

	// --- INTERNAL MAP ---
	
	protected FastBuildMap map;
	
	// --- CONSTRUCTORS ---
	
	public FastBuildTree() {
		this(16);
	}
	
	public FastBuildTree(int size) {
		super(new FastBuildMap(size), null);
		map = (FastBuildMap) asObject();
	}

	// --- FAST AND UNSAFE PUT ---
	
	public Tree putUnsafe(String path, Object value) {
		map.put(path, value);
		return this;
	}
	
	public Tree putUnsafe(String path, Tree value) {
		map.put(path, value == null ? null : value.asObject());
		return this;
	}

	public Tree putUnsafe(String path, FastBuildTree value) {
		map.put(path, value == null ? null : value.map);
		return this;
	}

}