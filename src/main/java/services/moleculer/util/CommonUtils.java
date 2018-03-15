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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import io.datatree.Tree;
import io.datatree.dom.TreeReaderRegistry;
import services.moleculer.ServiceBroker;
import services.moleculer.context.CallOptions;
import services.moleculer.eventbus.Groups;
import services.moleculer.service.Name;
import services.moleculer.service.Version;
import services.moleculer.transporter.Transporter;

/**
 * Common utilities.
 */
public final class CommonUtils {

	// --- PATH FORMATTER ---

	public static final String formatPath(String path) {
		if (path == null) {
			return "";
		}
		path = path.trim();
		if (path.isEmpty()) {
			return path;
		}
		if (!path.startsWith("/")) {
			path = '/' + path;
		}
		while (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}
	
	// --- ANNOTATION TO JSON CONVERTER ---

	public static final void convertAnnotations(Tree config, Annotation[] annotations)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		for (Annotation annotation : annotations) {

			// Create entry for annotation
			String annotationName = annotation.toString();
			int i = annotationName.lastIndexOf('.');
			if (i > -1) {
				annotationName = annotationName.substring(i + 1);
			}
			i = annotationName.indexOf('(');
			if (i > -1) {
				annotationName = annotationName.substring(0, i);
			}
			if (annotationName.length() > 1) {
				annotationName = Character.toLowerCase(annotationName.charAt(0)) + annotationName.substring(1);
			} else {
				annotationName = annotationName.toLowerCase();
			}
			if ("name".equals(annotationName) || "override".equals(annotationName)) {
				continue;
			}
			Tree annotationMap = config.putMap(annotationName);

			// Add annotation values
			Class<? extends Annotation> type = annotation.annotationType();
			Method[] members = type.getDeclaredMethods();
			for (Method member : members) {
				member.setAccessible(true);
				String propName = member.getName();
				Object propValue = member.invoke(annotation);
				annotationMap.putObject(propName, propValue);
				Tree newChild = annotationMap.get(propName);
				if (newChild.size() < 1) {
					newChild.remove();
				}
			}
			int size = annotationMap.size();
			if (size == 0) {
				annotationMap.remove();
			} else if (size == 1) {
				Tree value = annotationMap.getFirstChild();
				if (value != null && "value".equals(value.getName())) {
					annotationMap.setObject(value.asObject());
				}
			}
		}
	}

	// --- DURATION FORMATTER ---

	private static final NumberFormat numberFormatter = DecimalFormat.getInstance();

	public static final String formatNumber(Number number) {
		synchronized (numberFormatter) {
			return numberFormatter.format(number);
		}
	}

	public static final String formatNamoSec(long nanoSec) {
		String test, test2;
		if ((test = test(nanoSec, TimeUnit.HOURS, "hour")) != null) {
			test2 = test(nanoSec, TimeUnit.MINUTES, "minute");
			return test2 == null ? test : test + " (" + test2 + ")";
		}
		if ((test = test(nanoSec, TimeUnit.MINUTES, "minute")) != null) {
			test2 = test(nanoSec, TimeUnit.SECONDS, "second");
			return test2 == null ? test : test + " (" + test2 + ")";
		}
		if ((test = test(nanoSec, TimeUnit.SECONDS, "second")) != null) {
			test2 = test(nanoSec, TimeUnit.MILLISECONDS, "millisecond");
			return test2 == null ? test : test + " (" + test2 + ")";
		}
		if ((test = test(nanoSec, TimeUnit.MILLISECONDS, "millisecond")) != null) {
			return test + " (" + formatNumber(nanoSec) + " nanoseconds)";
		}
		return formatNumber(nanoSec) + " nanoseconds";
	}

	private static final String test(long nanoSec, TimeUnit unit, String postfix) {
		long converted = unit.convert(nanoSec, TimeUnit.NANOSECONDS);
		if (converted > 0 && converted < 1000) {
			if (converted == 1) {
				return converted + " " + postfix;
			}
			return converted + " " + postfix + "s";
		}
		return null;
	}

	// --- GET ALL NODE INFO STRUCTURES OF ALL NODES ---

	public static final Tree getNodeInfos(ServiceBroker broker, Transporter transporter) {
		Tree infos = new Tree();
		if (transporter == null) {
			infos.putObject(broker.getNodeID(), broker.getConfig().getServiceRegistry().getDescriptor());
		} else {
			Set<String> nodeIDset = transporter.getAllNodeIDs();
			String[] nodeIDarray = new String[nodeIDset.size()];
			nodeIDset.toArray(nodeIDarray);
			Arrays.sort(nodeIDarray, String.CASE_INSENSITIVE_ORDER);
			for (String nodeID : nodeIDarray) {
				Tree info = transporter.getDescriptor(nodeID);
				if (info == null) {
					continue;
				}
				infos.putObject(nodeID, info);
			}
		}
		return infos;
	}

	// --- GET HOSTNAME OR IP FROM AN INFO STRUCTURE ---

	public static final String getHostOrIP(boolean preferHostname, Tree info) {
		String hostOrIP = null;
		if (preferHostname) {
			hostOrIP = getHost(info);
			if (hostOrIP == null) {
				hostOrIP = getIP(info);
			}
		} else {
			hostOrIP = getIP(info);
			if (hostOrIP == null) {
				hostOrIP = getHost(info);
			}
		}
		return hostOrIP;
	}

	private static final String getHost(Tree info) {
		String host = info.get("hostname", (String) null);
		if (host != null && !host.isEmpty()) {
			return host;
		}
		return null;
	}

	private static final String getIP(Tree info) {
		Tree ipList = info.get("ipList");
		if (ipList != null && ipList.size() > 0) {
			String ip = ipList.get(0).asString();
			if (ip != null && !ip.isEmpty()) {
				return ip;
			}
		}
		return null;
	}

	// --- LOCAL HOST NAME ---

	private static String cachedHostName;

	public static final String getHostName() {
		if (cachedHostName != null) {
			return cachedHostName;
		}

		// User-defined public hostname
		String name = System.getProperty("hostname");
		if (name == null || name.isEmpty()) {
			try {
				name = InetAddress.getLocalHost().getHostName();
			} catch (Exception ignored) {
				name = null;
			}
		}
		if (name == null || name.isEmpty() || name.contains("localhost")) {
			try {
				name = System.getenv().get("COMPUTERNAME");
			} catch (Exception ignored) {
				name = null;
			}
		}
		if (name == null || name.isEmpty()) {
			cachedHostName = "localhost";
		} else {
			cachedHostName = name.toLowerCase();
		}
		return cachedHostName;
	}

	// --- PARSE URL LIST OR URL ARRAY ---

	public static final String[] parseURLs(Tree config, String name, String[] defaultURLs) {
		Tree urlNode = config.get(name);
		List<String> urlList;
		if (urlNode == null) {
			return defaultURLs;
		} else if (urlNode.isPrimitive()) {
			urlList = new ArrayList<>();
			String[] urls = urlNode.asString().split(",");
			for (String url : urls) {
				url = url.trim();
				if (!url.isEmpty()) {
					urlList.add(url);
				}
			}
		} else if (urlNode.isEnumeration()) {
			urlList = urlNode.asList(String.class);
		} else {
			return defaultURLs;
		}
		if (urlList.isEmpty()) {
			return defaultURLs;
		}
		String[] urls = new String[urlList.size()];
		urlList.toArray(urls);
		return urls;
	}

	// --- PARSE CALL / BROADCAST PARAMS ---

	public static final ParseResult parseParams(Object[] params) {
		Tree data = null;
		CallOptions.Options opts = null;
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
							if (value instanceof CallOptions.Options) {
								opts = (CallOptions.Options) value;
								continue;
							}
							if (value instanceof Groups) {
								groups = (Groups) value;
								continue;
							}
							i++;
							throw new IllegalArgumentException("Parameter #" + i + " (\"" + value
									+ "\") must be String, Context, Groups, or CallOptions!");
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
		return new ParseResult(data, opts, groups);
	}

	public static final String nameOf(String prefix, Field field) {
		Name n = field.getAnnotation(Name.class);
		String name = null;
		if (n != null) {
			name = n.value();
			if (name != null) {
				name = name.trim();
			}
		}
		if (name == null || name.isEmpty()) {
			name = field.getName();
		}
		if (name.indexOf('.') == -1) {
			return prefix + '.' + name;
		}
		return name;
	}

	public static final String nameOf(Object object, boolean addQuotes) {
		Class<?> c = object.getClass();
		Version v = c.getAnnotation(Version.class);
		String version = null;
		if (v != null) {
			version = v.value();
			if (version != null) {
				version = version.trim();
				if (version.isEmpty()) {
					version = null;
				}
			}
			if (version != null) {
				try {
					Double.parseDouble(version);
					version = 'v' + version;
				} catch (Exception ignored) {
				}
			}
		}
		Name n = c.getAnnotation(Name.class);
		String name = null;
		if (n != null) {
			name = n.value();
			if (name != null) {
				name = name.trim();
			}
		}
		if (name == null || name.isEmpty()) {
			name = c.getName();
			int i = Math.max(name.lastIndexOf('.'), name.lastIndexOf('$'));
			if (i > -1) {
				name = name.substring(i + 1);
			}
			name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
		}
		if (version != null) {
			name = version + '.' + name;
		}
		if (addQuotes && name.indexOf(' ') == -1) {
			name = "\"" + name + "\"";
		}
		return name;
	}

	public static final Tree readTree(String resourceURL) throws Exception {
		String format = getFormat(resourceURL);
		if (resourceURL.startsWith("http:") || resourceURL.startsWith("https:") || resourceURL.startsWith("file:")) {
			return readTree(new URL(resourceURL).openStream(), format);
		}
		URL url = CommonUtils.class.getResource(resourceURL);
		if (url == null && !resourceURL.startsWith("/")) {
			url = CommonUtils.class.getResource('/' + resourceURL);
		}
		if (url != null) {
			return readTree(url.openStream(), format);
		}
		File file = new File(resourceURL);
		if (file.isFile()) {
			return readTree(new FileInputStream(file), format);
		}
		throw new IOException("Resource not found (" + resourceURL + ")!");
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

	public static final byte[] compress(byte[] data, int level) throws IOException {
		final Deflater deflater = new Deflater(level, true);
		deflater.setInput(data);
		deflater.finish();
		final byte[] buffer = new byte[data.length + 128];
		final int length = deflater.deflate(buffer);
		final byte[] compressed = new byte[length];
		System.arraycopy(buffer, 0, compressed, 0, length);
		return compressed;
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

	// --- UNIT PARSER ---

	public static final long resolveUnit(String string) throws NumberFormatException {
		if (string == null || "null".equals(string)) {
			return 0l;
		}
		StringBuffer buffer = new StringBuffer(string.toLowerCase());

		// eg. 1KByte -> 1024byte
		long unit = resolveUnit(buffer);
		String number = buffer.toString().trim();
		number = number.replace(',', '.');
		int i = number.indexOf('.');
		if (i > -1) {
			number = number.substring(0, i);
		}
		if (number.length() == 0) {
			return 0;
		}
		long value = Long.parseLong(number);
		if (unit != 1) {
			value *= unit;
		}
		return value;
	}

	private static final long resolveUnit(StringBuffer buffer) {
		long unit = 1;
		int i = -1;
		for (;;) {
			i = buffer.indexOf("msec", 0);
			if (i != -1) {
				break;
			}
			i = buffer.indexOf("mill", 0);
			if (i != -1) {
				break;
			}
			i = buffer.indexOf("sec", 0);
			if (i != -1) {
				unit = 1000;
				break;
			}
			i = buffer.indexOf("min", 0);
			if (i != -1) {
				unit = 1000 * 60;
				break;
			}
			i = buffer.indexOf("hour", 0);
			if (i != -1) {
				unit = 1000 * 60 * 60;
				break;
			}
			i = buffer.indexOf("day", 0);
			if (i != -1) {
				unit = 1000 * 60 * 60 * 24;
				break;
			}
			i = buffer.indexOf("week", 0);
			if (i != -1) {
				unit = 1000 * 60 * 60 * 24 * 7;
				break;
			}
			i = buffer.indexOf("month", 0);
			if (i != -1) {
				unit = 1000 * 60 * 60 * 24 * 30;
				break;
			}
			i = buffer.indexOf("year", 0);
			if (i != -1) {
				unit = 1000 * 60 * 60 * 24 * 365;
				break;
			}
			i = buffer.indexOf("kbyte", 0);
			if (i != -1) {
				unit = 1024;
				break;
			}
			i = buffer.indexOf("mbyte", 0);
			if (i != -1) {
				unit = 1024 * 1024;
				break;
			}
			i = buffer.indexOf("gbyte", 0);
			if (i != -1) {
				unit = 1024 * 1024 * 1024;
				break;
			}
			i = buffer.indexOf("tbyte", 0);
			if (i != -1) {
				unit = 1024 * 1024 * 1024 * 1024;
				break;
			}
			i = buffer.indexOf("byte", 0);
			break;
		}
		if (i != -1) {
			buffer.setLength(i);
		}
		return unit;
	}

}