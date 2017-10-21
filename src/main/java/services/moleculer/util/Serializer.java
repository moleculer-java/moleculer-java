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
package services.moleculer.util;

import java.nio.charset.StandardCharsets;

import io.datatree.Tree;
import io.datatree.dom.TreeWriterRegistry;
import io.datatree.dom.builtin.JsonBuiltin;

/**
 * 
 */
public class Serializer {

	// --- NULL VALUE ---

	private static final byte[] NULL = { 'n', 'u', 'l', 'l' };

	// --- OBJECT TO BYTE ARRAY ---

	public static final byte[] serialize(Tree value, String format) {
		try {

			// JSON? (or "msgpack", "bson", "ion", etc...)
			// Non-JSON formats require extra dependencies
			// See https://berkesa.github.io/datatree-adapters/
			boolean json = format == null || format.equalsIgnoreCase("json");

			// Null value
			if (value == null) {
				if (json) {
					return NULL;
				} else {
					return TreeWriterRegistry.getWriter(format).toBinary(value, null, true);
				}
			}

			// Hierarchial JSON structure
			if (value.isStructure()) {
				return ((Tree) value).toBinary(format, true);
			}

			// Scalar value (String, Boolean, etc.)
			if (json) {
				return JsonBuiltin.serialize(value.asObject(), null).getBytes(StandardCharsets.UTF_8);
			}
			return TreeWriterRegistry.getWriter(format).toBinary(value, null, true);

		} catch (Exception ignored) {
		}
		return NULL;
	}

	// --- BYTE ARRAY TO OBJECT ---

	public static final Tree deserialize(byte[] bytes, String format) {
		try {

			// Null value
			if (bytes == null || bytes.length == 0) {
				return new Tree().setObject(null);
			}

			// JSON? (or "msgpack", "bson", "ion", etc...)
			// Non-JSON formats require extra dependencies
			// See https://berkesa.github.io/datatree-adapters/
			boolean json = format == null || format.equalsIgnoreCase("json");

			// JSON format
			if (json) {
				if (bytes.length > 3 && bytes[0] == 'n' && bytes[1] == 'u' && bytes[2] == 'l' && bytes[3] == 'l') {
					return new Tree().setObject(null);
				}

				// Hierarchial JSON structure
				final byte b = bytes[0];
				if (b == '{' || b == '[') {
					return new Tree(bytes);
				}

				// Scalar value (String, Boolean, etc.)
				return new Tree(bytes, "JsonBuiltin");
			}

			// Non-JSON
			return new Tree(bytes, format);

		} catch (Exception ignored) {
		}
		return new Tree().setObject(null);
	}

}