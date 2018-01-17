/**
 * MOLECULER MICROSERVICES FRAMEWORK<br>
 * <br>
 * This project is based on the idea of Moleculer Microservices
 * Framework for NodeJS (https://moleculer.services). Special thanks to
 * the Moleculer's project owner (https://github.com/icebob) for the
 * consultations.<br>
 * <br>
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
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
package services.moleculer.config;

import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import io.datatree.dom.TreeReader;
import io.datatree.dom.TreeReaderRegistry;

/**
 * JS to JSON converter. Converts<br>
 * {<br>
 * ttl:10*1000<br>
 * }<br>
 * to<br>
 * {<br>
 * "ttl":10000<br>
 * }
 */
public final class JSReader implements TreeReader {

	@Override
	public final Object parse(byte[] source) throws Exception {
		return parse(new String(source, StandardCharsets.UTF_8));
	}

	@Override
	public final Object parse(String source) throws Exception {
		StringBuilder json = new StringBuilder(source.length() + 64);
		json.append("{\r\n");

		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");

		StringTokenizer lines = new StringTokenizer(source, "\r\n");
		while (lines.hasMoreTokens()) {

			String line = lines.nextToken().trim();
			if (line.isEmpty() || line.endsWith(";")) {
				continue;
			}
			char c = line.charAt(0);
			if (c == '}' || c == '{' || c == ']' || c == '[') {
				json.append(line);
				json.append("\r\n");
				continue;
			}

			int i = line.indexOf(':');
			if (i == -1) {
				continue;
			}
			String key = line.substring(0, i).trim();
			String val = line.substring(i + 1).trim();
			boolean comma = val.endsWith(",");
			if (comma) {
				val = val.substring(0, val.length() - 1);
			}

			json.append(normalize(key, null));
			json.append(':');
			json.append(normalize(val, engine));
			if (comma) {
				json.append(',');
			}
			json.append("\r\n");
		}
		json.append('}');
		return TreeReaderRegistry.getReader(null).parse(json.toString());
	}

	@Override
	public final String getFormat() {
		return "js";
	}

	private static final String normalize(String txt, ScriptEngine engine) {
		if (engine != null) {
			if ("null".equals(txt) || "true".equals(txt) || "false".equals(txt) || "0".equals(txt)
					|| txt.startsWith("{") || txt.startsWith("[")) {
				return txt;
			}
			try {
				Double.parseDouble(txt);
				return txt;
			} catch (Exception notNumber) {
			}
			boolean math = true;
			char[] chars = txt.toCharArray();
			for (char c : chars) {
				if (c == ' ' || c == '+' || c == '-' || c == '*' || c == '(' || c == ')' || c == '/' || c == '.') {
					continue;
				}
				if (!Character.isDigit(c)) {
					math = false;
					break;
				}
			}
			if (math) {
				try {
					txt = String.valueOf(engine.eval(txt));
					return txt;
				} catch (Exception ignored) {
				}
			}
		}
		if (!txt.startsWith("\"")) {
			return "\"" + txt.replace("\"", "\\\"") + "\"";
		}
		return txt;
	}

}