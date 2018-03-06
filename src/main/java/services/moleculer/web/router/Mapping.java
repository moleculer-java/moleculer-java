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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.context.CallingOptions;
import services.moleculer.context.Context;
import services.moleculer.context.ContextFactory;
import services.moleculer.service.Action;
import services.moleculer.service.Middleware;
import services.moleculer.web.common.HttpConstants;

public class Mapping implements HttpConstants {

	// --- PARENT BROKER ---

	protected final ServiceBroker broker;

	// --- PROPERTIES ---

	protected final String httpMethod;
	protected final String actionName;
	protected final String pathPattern;
	protected final boolean isStatic;
	protected final String pathPrefix;

	protected final int[] indexes;
	protected final String[] names;

	protected final CallingOptions.Options opts;

	protected final int hashCode;

	protected final ContextFactory contextFactory;

	// --- CONSTRUCTOR ---

	public Mapping(ServiceBroker broker, String httpMethod, String pathPattern, String actionName, CallingOptions.Options opts) {
		this.broker = broker;
		this.httpMethod =  "ALL".equals(httpMethod) ? null : httpMethod;
		this.pathPattern = pathPattern;
		this.actionName = actionName;
		this.opts = opts;
		this.contextFactory = broker.getConfig().getContextFactory();

		// Parse "path pattern"
		int starPos = pathPattern.indexOf('*');
		isStatic = pathPattern.indexOf(':') == -1 && starPos == -1;
		String[] tokens = null;
		ArrayList<Integer> indexList = new ArrayList<>();
		ArrayList<String> nameList = new ArrayList<>();
		if (isStatic) {
			pathPrefix = pathPattern;
		} else if (starPos > -1) {
			pathPrefix = pathPattern.substring(0, starPos);
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

	public boolean matches(String httpMethod, String path) {
		if (this.httpMethod != null && !this.httpMethod.equals(httpMethod)) {
			return false;
		}
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

	public Promise processRequest(String httpMethod, String path, Tree headers, String query, byte[] body) {
		try {

			// Parse request
			Tree params = null;
			if (isStatic) {
				if (body == null || body.length < 1) {

					// Empty body
					params = new Tree();
				} else if (body[0] == '{' || body[0] == '[') {

					// JSON body
					params = new Tree(body);
				}
				if (query != null) {

					// URL-encoded Query String
					if (params == null) {
						params = new Tree();
					}
					String[] pairs = query.split("&");
					int i;
					for (String pair : pairs) {
						i = pair.indexOf("=");
						params.put(URLDecoder.decode(pair.substring(0, i), "UTF-8"),
								URLDecoder.decode(pair.substring(i + 1), "UTF-8"));
					}
				}
			} else {

				// Parameters in URL (eg "/path/:id/:name")
				params = new Tree();
				String[] tokens = pathPattern.split("/");
				for (int i = 0; i < indexes.length; i++) {
					params.put(names[i], tokens[i]);
				}
			}

			// Set path
			Tree meta = params.getMeta();
			meta.put(METHOD, httpMethod);
			meta.put(PATH, path);
			meta.put(PATTERN, pathPattern);
			
			// Copy headers
			if (headers != null) {
				meta.putObject(HEADERS, headers.asObject());
			}
			
			// Call action
			if (current == brokerAction) {
				return broker.call(actionName, params, opts);
			}
			return new Promise(current.handler(contextFactory.create(actionName, params, opts, null)));

		} catch (Throwable cause) {
			return Promise.reject(cause);
		}
	}

	// --- ACTION WITH MIDDLEWARES ---

	protected HashSet<Middleware> checkedMiddlewares = new HashSet<>(32);

	protected final Action brokerAction = new Action() {

		@Override
		public Object handler(Context ctx) throws Exception {
			return broker.call(ctx.name, ctx.params, ctx.opts);
		}

	};

	protected Action current = brokerAction;

	public void use(Collection<Middleware> middlewares) {
		Tree config = new Tree();
		config.put("action", actionName);
		config.put("pattern", pathPattern);
		config.put("static", isStatic);
		config.put("prefix", pathPrefix);
		if (opts != null) {
			config.put("nodeID", opts.nodeID);
			config.put("retryCount", opts.retryCount);
			config.put("timeout", opts.timeout);
		}
		for (Middleware middleware : middlewares) {
			if (checkedMiddlewares.add(middleware)) {
				Action action = middleware.install(current, config);
				if (action != null) {
					current = action;
				}
			}
		}
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