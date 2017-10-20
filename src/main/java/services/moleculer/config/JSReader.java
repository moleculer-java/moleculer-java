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