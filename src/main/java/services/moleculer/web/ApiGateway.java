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

	// --- LIST OF HTTP STATUS CODES ---

	public static final String STATUS_100 = "100 Continue";
	public static final String STATUS_101 = "101 Switching Protocols";
	public static final String STATUS_102 = "102 Processing";
	public static final String STATUS_200 = "200 OK";
	public static final String STATUS_201 = "201 Created";
	public static final String STATUS_202 = "202 Accepted";
	public static final String STATUS_203 = "203 Non-authoritative Information";
	public static final String STATUS_204 = "204 No Content";
	public static final String STATUS_205 = "205 Reset Content";
	public static final String STATUS_206 = "206 Partial Content";
	public static final String STATUS_207 = "207 Multi-Status";
	public static final String STATUS_208 = "208 Already Reported";
	public static final String STATUS_226 = "226 IM Used";
	public static final String STATUS_300 = "300 Multiple Choices";
	public static final String STATUS_301 = "301 Moved Permanently";
	public static final String STATUS_302 = "302 Found";
	public static final String STATUS_303 = "303 See Other";
	public static final String STATUS_304 = "304 Not Modified";
	public static final String STATUS_305 = "305 Use Proxy";
	public static final String STATUS_307 = "307 Temporary Redirect";
	public static final String STATUS_308 = "308 Permanent Redirect";
	public static final String STATUS_400 = "400 Bad Request";
	public static final String STATUS_401 = "401 Unauthorized";
	public static final String STATUS_402 = "402 Payment Required";
	public static final String STATUS_403 = "403 Forbidden";
	public static final String STATUS_404 = "404 Not Found";
	public static final String STATUS_405 = "405 Method Not Allowed";
	public static final String STATUS_406 = "406 Not Acceptable";
	public static final String STATUS_407 = "407 Proxy Authentication Required";
	public static final String STATUS_408 = "408 Request Timeout";
	public static final String STATUS_409 = "409 Conflict";
	public static final String STATUS_410 = "410 Gone";
	public static final String STATUS_411 = "411 Length Required";
	public static final String STATUS_412 = "412 Precondition Failed";
	public static final String STATUS_413 = "413 Payload Too Large";
	public static final String STATUS_414 = "414 Request-URI Too Long";
	public static final String STATUS_415 = "415 Unsupported Media Type";
	public static final String STATUS_416 = "416 Requested Range Not Satisfiable";
	public static final String STATUS_417 = "417 Expectation Failed";
	public static final String STATUS_418 = "418 I'm a teapot";
	public static final String STATUS_421 = "421 Misdirected Request";
	public static final String STATUS_422 = "422 Unprocessable Entity";
	public static final String STATUS_423 = "423 Locked";
	public static final String STATUS_424 = "424 Failed Dependency";
	public static final String STATUS_426 = "426 Upgrade Required";
	public static final String STATUS_428 = "428 Precondition Required";
	public static final String STATUS_429 = "429 Too Many Requests";
	public static final String STATUS_431 = "431 Request Header Fields Too Large";
	public static final String STATUS_444 = "444 Connection Closed Without Response";
	public static final String STATUS_451 = "451 Unavailable For Legal Reasons";
	public static final String STATUS_499 = "499 Client Closed Request";
	public static final String STATUS_500 = "500 Internal Server Error";
	public static final String STATUS_501 = "501 Not Implemented";
	public static final String STATUS_502 = "502 Bad Gateway";
	public static final String STATUS_503 = "503 Service Unavailable";
	public static final String STATUS_504 = "504 Gateway Timeout";
	public static final String STATUS_505 = "505 HTTP Version Not Supported";
	public static final String STATUS_506 = "506 Variant Also Negotiates";
	public static final String STATUS_507 = "507 Insufficient Storage";
	public static final String STATUS_508 = "508 Loop Detected";
	public static final String STATUS_510 = "510 Not Extended";
	public static final String STATUS_511 = "511 Network Authentication Required";
	public static final String STATUS_599 = "599 Network Connect Timeout Error";

	// --- COMMON HTTP HEADERS ---

	public static final String CONTENT_TYPE = "Content-Type";
	public static final String CONTENT_LENGTH = "Content-Length";
	public static final String ETAG = "ETag";
	public static final String IF_NONE_MATCH = "If-None-Match";
	public static final String CONNECTION = "Connection";
	public static final String KEEP_ALIVE = "keep-alive";
	public static final String CLOSE = "close";
	public static final String ACCEPT_ENCODING = "Accept-Encoding";
	public static final String CONTENT_ENCODING = "Content-Encoding";
	public static final String DEFLATE = "deflate";

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

	protected Promise processRequest(String httpMethod, String path, Tree headers, byte[] body) throws Exception {
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

		// 404 Not Found
		Tree notFound = new Tree();
		notFound.getMeta().putMap("response.status").put("status", STATUS_404);
		notFound.put("message", "Not Found: " + path);
		return Promise.resolve(notFound);
	}

}