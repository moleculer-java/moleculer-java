package services.moleculer.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import services.moleculer.services.Name;

public class CommonUtils {

	public static final String nameOf(Object object) {
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
		return name;
	}

	public static final byte[] compress(byte[] data) throws IOException {
		Deflater deflater = new Deflater(Deflater.BEST_SPEED, true);
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
		Inflater inflater = new Inflater(true);
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