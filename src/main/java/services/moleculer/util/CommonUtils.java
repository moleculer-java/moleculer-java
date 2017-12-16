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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import io.datatree.Tree;
import io.datatree.dom.TreeReaderRegistry;
import services.moleculer.context.CallingOptions;
import services.moleculer.context.Context;
import services.moleculer.eventbus.Groups;
import services.moleculer.service.Name;

/**
 * Common utilities.
 */
public final class CommonUtils {

	public static final ParseResult parseParams(Object[] params) {
		Tree data = null;
		Context parent = null;
		CallingOptions.Options opts = null;
		Groups groups = null;
		if (params != null) {
			if (params.length == 1) {
				if (params[0] instanceof Tree) {
					data = (Tree) params[0];
				} else {
					data = new CheckedTree(params[0]);
				}
			} else {
				LinkedHashMap<String, Object> map = new LinkedHashMap<>();
				String prev = null;
				Object value;
				for (int i = 0; i < params.length; i++) {
					value = params[i];
					if (prev == null) {
						if (!(value instanceof String)) {
							if (value instanceof CallingOptions.Options) {
								opts = (CallingOptions.Options) value;
								continue;
							}
							if (value instanceof Context) {
								parent = (Context) value;
								continue;
							}
							if (value instanceof Groups) {
								groups = (Groups) value;
								continue;
							}
							i++;
							throw new IllegalArgumentException("Parameter #" + i + " (\"" + value
									+ "\") must be String, Context, Groups, or CallingOptions!");
						}
						prev = (String) value;
						continue;
					}
					map.put(prev, value);
					prev = null;
				}
				data = new Tree(map);
			}
		}
		return new ParseResult(data, parent, opts, groups);
	}

	public static final String serializerTypeToClass(String type) {
		String test = type.toLowerCase();
		if ("json".equals(test)) {
			return "services.moleculer.serializer.JsonSerializer";
		}
		if ("msgpack".equals(test) || "messagepack".equals(test)) {
			return "services.moleculer.serializer.MessagePackSerializer";
		}
		if ("smile".equals(test)) {
			return "services.moleculer.serializer.SmileSerializer";
		}
		if ("bson".equals(test)) {
			return "services.moleculer.serializer.BsonSerializer";
		}
		if ("ion".equals(test)) {
			return "services.moleculer.serializer.IonSerializer";
		}
		if ("cbor".equals(test)) {
			return "services.moleculer.serializer.CborSerializer";
		}
		if ("xml".equals(test)) {
			return "services.moleculer.serializer.XmlSerializer";
		}
		if (test.indexOf('.') > -1) {
			return type;
		}
		throw new IllegalArgumentException("Invalid serializer type (" + type + ")!");
	}

	public static final String nameOf(Object object, boolean addQuotes) {
		Objects.requireNonNull(object);
		Class<?> c = object.getClass();
		Name n = c.getAnnotation(Name.class);
		String name = null;
		if (n != null) {
			name = n.value();
		}
		if (name != null) {
			name = name.trim();
		}
		if (name == null || name.isEmpty()) {
			name = c.getName();
			int i = Math.max(name.lastIndexOf('.'), name.lastIndexOf('$'));
			if (i > -1) {
				name = name.substring(i + 1);
			}
			name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
		}
		name = name.trim();
		if (addQuotes && name.indexOf(' ') == -1) {
			name = "\"" + name + "\"";
		}
		return name;
	}

	public static final String nameOf(Tree tree) {
		String name = tree.get("name", "");
		if (name == null || name.isEmpty()) {
			name = tree.getName();
		}
		if (name == null) {
			return "";
		}
		return name.trim();
	}

	public static final String typeOf(Tree tree) {
		String type = tree.get("type", "");
		if ((type == null || type.isEmpty()) && tree.isPrimitive()) {
			type = tree.asString();
		}
		if (type == null) {
			return "";
		}
		return type;
	}

	public static final Tree readTree(InputStream in, String format) throws Exception {
		return new Tree(readFully(in), format);
	}

	public static final byte[] readFully(InputStream in) throws Exception {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int length;
			while ((length = in.read(buffer)) != -1) {
				bytes.write(buffer, 0, length);
			}
			return bytes.toByteArray();
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	public static final String getFormat(String path) {
		path = path.toLowerCase();
		int i = path.lastIndexOf('.');
		if (i > 0) {
			String format = path.substring(i + 1);
			try {

				// Is format valid?
				TreeReaderRegistry.getReader(format);
				return format;
			} catch (Exception notSupported) {
			}
		}

		// JSON is the default format
		return null;
	}

	// --- COMPRESSS / DECOMPRESS ---

	public static final byte[] compress(byte[] data) throws IOException {
		Deflater deflater = new Deflater(Deflater.BEST_SPEED, false);
		deflater.setInput(data);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
		deflater.finish();
		byte[] buffer = new byte[1024];
		while (!deflater.finished()) {
			int count = deflater.deflate(buffer);
			outputStream.write(buffer, 0, count);
		}
		return outputStream.toByteArray();
	}

	public static final byte[] decompress(byte[] data) throws IOException, DataFormatException {
		Inflater inflater = new Inflater(false);
		inflater.setInput(data);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
		byte[] buffer = new byte[1024];
		while (!inflater.finished()) {
			int count = inflater.inflate(buffer);
			outputStream.write(buffer, 0, count);
		}
		return outputStream.toByteArray();
	}

}