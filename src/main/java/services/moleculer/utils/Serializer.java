package services.moleculer.utils;

import java.nio.charset.StandardCharsets;

import io.datatree.Tree;
import io.datatree.dom.TreeReaderRegistry;
import io.datatree.dom.TreeWriterRegistry;
import io.datatree.dom.builtin.JsonBuiltin;

public class Serializer {

	// --- NULL VALUE ---

	private static final byte[] NULL = { 'n', 'u', 'l', 'l' };

	// --- OBJECT TO BYTE ARRAY ---

	public static final byte[] serialize(Object value, String format) {
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
			if (value instanceof Tree) {
				return ((Tree) value).toBinary(format, true);
			}

			// Scalar value (String, Boolean, etc.)
			if (json) {
				return JsonBuiltin.serialize(value, null).getBytes(StandardCharsets.UTF_8);
			}
			return TreeWriterRegistry.getWriter(format).toBinary(value, null, true);

		} catch (Exception ignored) {
		}
		return NULL;
	}

	// --- BYTE ARRAY TO OBJECT ---

	public static final Object deserialize(byte[] bytes, String format) {
		try {

			// Null value
			if (bytes == null || bytes.length == 0) {
				return null;
			}

			// JSON? (or "msgpack", "bson", "ion", etc...)
			// Non-JSON formats require extra dependencies
			// See https://berkesa.github.io/datatree-adapters/
			boolean json = format == null || format.equalsIgnoreCase("json");

			// JSON format
			if (json) {
				if (bytes.length > 3 && bytes[0] == 'n' && bytes[1] == 'u' && bytes[2] == 'l' && bytes[3] == 'l') {
					return null;
				}

				// Hierarchial JSON structure
				final byte b = bytes[0];
				if (b == '{' || b == '[') {
					return new Tree(bytes);
				}

				// Scalar value (String, Boolean, etc.)
				return new JsonBuiltin().parse(bytes);
			}

			// Non-JSON
			return TreeReaderRegistry.getReader(format).parse(bytes);
			
		} catch (Exception ignored) {
		}
		return null;
	}

}