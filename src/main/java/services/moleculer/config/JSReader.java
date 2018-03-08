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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import io.datatree.dom.TreeReader;
import io.datatree.dom.TreeReaderRegistry;

/**
 * JS to JSON converter. Converts<br>
 * <pre>
 * {
 *   ttl: 10*1000
 * }
 * </pre>
 * to<br>
 * <pre>
 * {
 *   "ttl": 10000
 * }
 * </pre>
 */
public final class JSReader implements TreeReader {

	@Override
	public final Object parse(byte[] source) throws Exception {
		return parse(new String(source, StandardCharsets.UTF_8));
	}

	@Override
	public final Object parse(String source) throws Exception {
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");

		// Load as JavaScript object, then convert to JSON
		StringBuilder js = new StringBuilder(source.length() + 64);
		js.append("module  = {};\r\n");
		js.append("exports = null;\r\n");
		js.append(source);
		js.append("\r\nJSON.stringify(exports ? exports : module.exports);");
		String json = String.valueOf(engine.eval(js.toString()));
		
		// Parse JSON to Tree
		return TreeReaderRegistry.getReader(null).parse(json.toString());
	}

	@Override
	public final String getFormat() {
		return "js";
	}

}