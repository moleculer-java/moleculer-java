package services.moleculer.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import io.datatree.Tree;
import io.datatree.dom.TreeReaderRegistry;
import services.moleculer.services.Name;

public class CommonUtils {

	// --- CONFIG PROCESSOR UTILITIES ---

	public static final Tree getProperty(Tree config, String name, Object defaultValue) {
		Tree prop = config.get(name);
		if (prop == null) {
			prop = config.get("opts." + name);
		}
		if (prop == null) {
			Tree items = config.get("_items");
			if (items != null) {
				for (Tree item : items) {
					String id = idOf(item);
					if (name.equals(id)) {
						prop = item.get("@value");
						break;
					}
				}
			}
		}
		if (prop == null || prop.isNull()) {
			return new Tree().setObject(defaultValue);
		}
		return prop;
	}

	public static final String nameOf(Object object, boolean addQuotes) {
		String name = null;
		if (object != null) {
			Class<?> c = object.getClass();
			Name n = c.getAnnotation(Name.class);
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
		}
		if (name == null || name.isEmpty()) {
			name = "unknown";
		}
		if (addQuotes && name.indexOf(' ') == -1) {
			name = "\"" + name + "\"";
		}
		return name;
	}

	public static final String idOf(Tree tree) {
		String id = tree.get("id", "");
		if (id == null || id.isEmpty()) {
			id = tree.get("name", "");
			if (id == null || id.isEmpty()) {
				id = tree.get("@id", "");
				if (id == null || id.isEmpty()) {
					id = tree.get("@name", "");
				}
			}
		}
		if (id == null || id.isEmpty()) {
			id = tree.getName();
		}
		if (id == null) {
			return "";
		}
		return id.trim();
	}

	public static final String typeOf(Tree tree) {
		String type = tree.get("class", "");
		if (type == null || type.isEmpty()) {
			type = tree.get("type", "");
			if (type == null || type.isEmpty()) {
				type = tree.get("@class", "");
			}
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

				// JSON
				return null;
			}
		}

		// JSON
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