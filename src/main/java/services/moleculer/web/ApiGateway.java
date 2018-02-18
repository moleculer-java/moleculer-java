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
package services.moleculer.web;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.context.CallingOptions;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.transporter.Transporter;
import services.moleculer.web.router.Alias;
import services.moleculer.web.router.Mapping;
import services.moleculer.web.router.MappingPolicy;
import services.moleculer.web.router.Route;

@Name("api-gw")
public abstract class ApiGateway extends Service {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- PARENT BROKER ---

	protected ServiceBroker broker;

	// --- COMPONENTS ---

	protected ServiceRegistry registry;
	protected Transporter transporter;

	// --- ROUTES ---

	protected Route[] routes = new Route[0];

	// --- CACHED MAPPINGS ---

	protected int cacheSize;

	protected LinkedHashMap<String, Mapping> staticMappings;
	protected LinkedList<Mapping> dynamicMappings;

	// --- START GATEWAY INSTANCE ---

	/**
	 * Initializes gateway instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {
		this.broker = broker;
		this.registry = broker.components().registry();
		this.transporter = broker.components().transporter();

		// Process config
		cacheSize = Math.max(64, config.get("cachedSize", 1024));
		staticMappings = new LinkedHashMap<String, Mapping>(cacheSize) {

			private static final long serialVersionUID = 5994447707758047152L;

			protected final boolean removeEldestEntry(Map.Entry<String, Mapping> entry) {
				if (this.size() > cacheSize) {
					return true;
				}
				return false;
			};

		};
		dynamicMappings = new LinkedList<>();
		Tree routesBlock = config.get("routes");
		LinkedList<Route> routeList = new LinkedList<>();
		if (routes != null) {
			for (Tree route : routesBlock) {

				// Process path
				String path = route.get("path", (String) null);
				if (path != null) {
					path = path.trim();
					if (path.isEmpty() || "/".equals(path)) {
						path = null;
					} else {
						if (!path.startsWith("/")) {
							path = "/" + path;
						}
						if (path.endsWith("/")) {
							path = path.substring(0, path.length() - 1);
						}
					}
				}

				// Process callOptions
				Tree callOptions = route.get("callOptions");
				CallingOptions.Options opts = null;
				if (callOptions != null) {
					String nodeID = callOptions.get("nodeID", (String) null);
					if (nodeID != null && !nodeID.isEmpty()) {
						opts = CallingOptions.nodeID(nodeID);
					}
					int timeout = callOptions.get("timeout", 0);
					if (timeout > 0) {
						if (opts == null) {
							opts = CallingOptions.timeout(timeout);
						} else {
							opts = opts.timeout(timeout);
						}
					}
					int retryCount = callOptions.get("retryCount", 0);
					if (retryCount > 0) {
						if (opts == null) {
							opts = CallingOptions.retryCount(retryCount);
						} else {
							opts = opts.retryCount(retryCount);
						}
					}
				}

				// Process mappingPolicy
				MappingPolicy mappingPolicy = "all".equals(route.get("mappingPolicy", "all")) ? MappingPolicy.ALL
						: MappingPolicy.RESTRICT;

				// Process whitelist
				String[] whitelist = null;
				Tree whitelistBlock = route.get("whitelist");
				if (whitelistBlock != null && whitelistBlock.isEnumeration()) {
					List<String> list = whitelistBlock.asList(String.class);
					if (!list.isEmpty()) {
						whitelist = new String[list.size()];
						list.toArray(whitelist);
					}
				}

				// Process aliases
				Tree aliasesBlock = route.get("aliases");
				LinkedHashSet<Alias> aliasSet = new LinkedHashSet<>();
				if (aliasesBlock != null) {
					for (Tree alias : aliasesBlock) {
						String[] tokens = alias.getName().split(" ");
						if (tokens.length == 1) {
							String[] tmp = new String[2];
							tmp[0] = "ALL";
							tmp[1] = tokens[0];
							tokens = tmp;
						}
						String actionName = alias.asString();
						String httpMethod = tokens[0].toUpperCase();
						String pathPattern = tokens[1];
						if (!pathPattern.startsWith("/")) {
							pathPattern = "/" + pathPattern;
						}
						if (pathPattern.endsWith("/")) {
							pathPattern = pathPattern.substring(0, pathPattern.length() - 1);
						}
						if ("REST".equals(httpMethod)) {

							// TODO add POST/GET/etc.

						} else {
							aliasSet.add(new Alias(httpMethod, pathPattern, actionName));
						}
					}
				}
				Alias[] aliases = null;
				if (!aliasSet.isEmpty()) {
					aliases = new Alias[aliasSet.size()];
					aliasSet.toArray(aliases);
				}

				// Add route
				routeList.addLast(new Route(broker, path, mappingPolicy, opts, whitelist, aliases));
			}
			if (routeList.isEmpty()) {
				routes = new Route[0];
			} else {
				routes = new Route[routeList.size()];
				routeList.toArray(routes);
			}
		}
	}

	// --- STOP GATEWAY INSTANCE ---

	/**
	 * Closes gateway.
	 */
	@Override
	public void stop() {
		routes = new Route[0];
		
		// TODO Synchronize this block (with write lock)
		staticMappings.clear();
		dynamicMappings.clear();
		// TODO End of synchronized block
	}

	// --- COMMON HTTP REQUEST PROCESSOR ---

	protected Promise processRequest(String httpMethod, String path, LinkedHashMap<String, String> headers,
			byte[] body) {
		try {
			if (!path.startsWith("/")) {
				path = "/" + path;
			}

			// TODO Synchronize this block (with read lock)
			// Try to find in static mappings (eg. "/user")
			Mapping mapping = staticMappings.get(path);
			if (mapping == null) {

				// Try to find in dynamic mappings (eg. "/user/:id")
				for (Mapping dynamicMapping : dynamicMappings) {
					if (dynamicMapping.matches(path)) {
						mapping = dynamicMapping;
						break;
					}
				}
			}
			// TODO End of synchronized block

			// Invoke mapping
			if (mapping != null) {
				Promise response = mapping.processRequest(path, headers, body);
				if (response != null) {
					return response;
				}
			}

			// Find in routes
			Mapping newMapping = null;
			for (Route route : routes) {
				newMapping = route.findMapping(httpMethod, path);
				if (newMapping != null) {
					break;
				}
			}
			if (newMapping != null) {

				// Store new mapping
				// TODO Synchronize this block (with write lock)
				if (mapping.isStatic()) {
					if (!staticMappings.containsValue(mapping)) {
						staticMappings.put(mapping.getPathPrefix(), mapping);
					}
				} else {
					if (!dynamicMappings.contains(mapping)) {
						dynamicMappings.addLast(mapping);
						if (dynamicMappings.size() > cacheSize) {
							dynamicMappings.removeFirst();
						}
					}
				}
				// TODO End of synchronized block

				Promise response = newMapping.processRequest(path, headers, body);
				if (response != null) {
					return response;
				}
			}
		} catch (Exception cause) {
			return convertError(cause, null, "500 Internal Server Error");
		}

		// 404 Not Found
		return convertError(null, "Not Found: " + path, "404 Not Found");
	}

	protected Promise convertError(Throwable cause, String message, String status) {
		String trace = null;
		if (cause != null) {
			if (message == null) {
				message = cause.getMessage();
			}
			StringWriter traceWriter = new StringWriter(512);
			cause.printStackTrace(new PrintWriter(traceWriter, true));
			trace = traceWriter.toString();
		}
		if (message != null) {
			message = message.replace('\r', ' ').replace('\t', ' ').replace('\n', ' ').replace("\"", "\\\"").trim();
		}
		if (message == null || message.isEmpty()) {
			message = "Unexpected error occured!";
		}
		Tree out = new Tree();
		out.put("message", message);
		if (trace != null) {
			out.put("trace", trace);
		}
		out.getMeta(true).put("status", status);
		return Promise.resolve(out);
	}

}