/**
 * This software is licensed under MIT license.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
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
package services.moleculer.serializer;

import java.nio.charset.StandardCharsets;

import io.datatree.Tree;
import io.datatree.dom.builtin.JsonBuiltin;
import services.moleculer.service.Name;
import services.moleculer.util.CheckedTree;

/**
 * <b>GENERIC JSON SERIALIZER</b><br>
 * <br>
 * JSON (JavaScript Object Notation) is a lightweight data-interchange format.
 * It is easy for humans to read and write.<br>
 * <br>
 * <b>Required dependency:</b> none / optional (eg. if you add Jackson API to
 * the classpath, JsonSerializer will user Jackson parser)
 */
@Name("JSON Serializer")
public final class JsonSerializer extends Serializer {

	// --- NULL VALUE ---

	private static final byte[] NULL = { 'n', 'u', 'l', 'l' };

	// --- CONSTRUCTOR ---

	public JsonSerializer() {
		super("json");
	}

	// --- SERIALIZE TREE TO BYTE ARRAY ---

	@Override
	public final byte[] write(Tree value) throws Exception {

		// Null value
		if (value == null || value.isNull()) {
			return NULL;
		}

		// Hierarchial JSON structure
		if (value.isStructure()) {
			return ((Tree) value).toBinary(null, true);
		}

		// Scalar value (String, Boolean, etc.)
		return JsonBuiltin.serialize(value.asObject(), null).getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public final Tree read(byte[] source) throws Exception {

		// Null values
		if (source == null) {
			return new CheckedTree(null);
		}
		if (source.length > 3 && source[0] == 'n' && source[1] == 'u' && source[2] == 'l' && source[3] == 'l') {
			return new CheckedTree(null);
		}

		// Hierarchial JSON structure
		final byte b = source[0];
		if (b == '{' || b == '[') {
			return new Tree(source);
		}

		// Scalar value (String, Boolean, etc.)
		return new Tree(source, "JsonBuiltin");
	}

}