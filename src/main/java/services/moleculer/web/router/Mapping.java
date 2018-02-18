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
package services.moleculer.web.router;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.context.CallingOptions;

public class Mapping {

	// --- PARENT BROKER ---

	protected final ServiceBroker broker;

	// --- PROPERTIES ---

	protected final String actionName;
	protected final String pathPattern;
	protected final boolean isStatic;
	protected final String pathPrefix;

	protected final int[] indexes;
	protected final String[] names;

	protected final CallingOptions.Options opts;

	protected final int hashCode;
	
	// --- CONSTRUCTOR ---

	public Mapping(ServiceBroker broker, String pathPattern, String actionName,
			CallingOptions.Options opts) {
		this.broker = broker;
		this.pathPattern = pathPattern;
		this.actionName = actionName;
		this.opts = opts;

		// Parse "path pattern"
		isStatic = pathPattern.indexOf(':') == -1;
		String[] tokens = null;
		ArrayList<Integer> indexList = new ArrayList<>();
		ArrayList<String> nameList = new ArrayList<>();
		if (isStatic) {
			pathPrefix = pathPattern;
		} else {
			tokens = pathPattern.split("/");
			int endIndex = 0;
			for (int i = 0; i < tokens.length; i++) {
				String token = tokens[i].trim();
				if (token.startsWith(":")) {
					token = token.substring(1);
					indexList.add(i);
					nameList.add(token);
					continue;
				}
				if (indexList.isEmpty()) {
					endIndex += token.length() + 1;
				}
			}
			pathPrefix = pathPattern.substring(0, endIndex);
		}
		indexes = new int[indexList.size()];
		names = new String[nameList.size()];
		for (int i = 0; i < indexes.length; i++) {
			indexes[i] = indexList.get(i);
			names[i] = nameList.get(i);
		}
		
		// Generate hashcode
		final int prime = 31;
		int result = 1;
		result = prime * result + actionName.hashCode();
		result = prime * result + pathPrefix.hashCode();
		hashCode = result;
	}

	// --- MATCH TYPE ---

	public boolean isStatic() {
		return isStatic;
	}

	// --- PATH PREFIX ---

	public String getPathPrefix() {
		return pathPrefix;
	}

	// --- MATCH TEST ---

	public boolean matches(String path) {
		if (isStatic) {
			if (!path.equals(pathPrefix)) {
				return false;
			}
		} else {
			if (!path.startsWith(pathPrefix)) {
				return false;
			}
		}
		return true;
	}

	// --- REQUEST PROCESSOR ---

	public Promise processRequest(String path, LinkedHashMap<String, String> headers, byte[] body) throws Exception {

		// Check path
		if (!matches(path)) {
			return null;
		}

		// Parse request
		Tree params;
		if (isStatic) {
			if (body == null || body.length < 1) {

				// Empty body
				params = new Tree();
			} else if (body[0] == '{' || body[0] == '[') {

				// JSON body
				params = new Tree(body);
			} else {

				// URL-encoded Query String
				params = new Tree();
				String query = new String(body, StandardCharsets.UTF_8);
				String[] pairs = query.split("&");
				int i;
				for (String pair : pairs) {
					i = pair.indexOf("=");
					params.put(URLDecoder.decode(pair.substring(0, i), "UTF-8"),
							URLDecoder.decode(pair.substring(i + 1), "UTF-8"));
				}
			}
		} else {

			// parameters in URL (eg "/path/:id/:name")
			params = new Tree();
			String[] tokens = pathPattern.split("/");
			for (int i = 0; i < indexes.length; i++) {
				params.put(names[i], tokens[i]);
			}
		}

		// TODO Call middlewares

		// Call action
		return broker.call(actionName, params, opts);
	}

	// --- COLLECTION HELPERS ---

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Mapping other = (Mapping) obj;
		if (actionName.equals(other.actionName)) {
			return true;
		}
		if (pathPrefix.equals(other.pathPrefix)) {
			return true;
		}
		return false;
	}

}