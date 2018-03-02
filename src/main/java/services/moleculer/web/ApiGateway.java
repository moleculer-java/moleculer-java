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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Middleware;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.transporter.Transporter;
import services.moleculer.web.router.Alias;
import services.moleculer.web.router.HttpConstants;
import services.moleculer.web.router.Mapping;
import services.moleculer.web.router.MappingPolicy;
import services.moleculer.web.router.Route;

/**
 * Base superclass of all Moleculer Web Server ("API Gateway") implementations.
 *
 * @see SunGateway
 */
@Name("API Gateway")
public abstract class ApiGateway extends Service implements HttpConstants {

	// --- COMPONENTS ---

	protected ServiceRegistry registry;
	protected Transporter transporter;

	// --- ROUTES ---

	protected Route[] routes = new Route[0];

	// --- CACHED MAPPINGS ---

	/**
	 * Maximum number of cached routes
	 */
	protected int cachedRoutes = 1024;

	protected LinkedHashMap<String, Mapping> staticMappings;	
	protected final LinkedList<Mapping> dynamicMappings = new LinkedList<>();

	// --- LOCKS ---

	protected final Lock readLock;
	protected final Lock writeLock;

	// --- CONSTRUCTOR ---

	public ApiGateway() {

		// Init locks
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
		readLock = lock.readLock();
		writeLock = lock.writeLock();
	}

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
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);
		staticMappings = new LinkedHashMap<String, Mapping>(64) {

			private static final long serialVersionUID = 2994447707758047152L;

			protected final boolean removeEldestEntry(Map.Entry<String, Mapping> entry) {
				if (this.size() > cachedRoutes) {
					return true;
				}
				return false;
			};

		};
		this.registry = broker.getConfig().getServiceRegistry();
		this.transporter = broker.getConfig().getTransporter();
	}

	// --- STOP GATEWAY INSTANCE ---

	/**
	 * Closes gateway.
	 */
	@Override
	public void stopped() {
		setRoutes(new Route[0]);
		writeLock.lock();
		try {
			checkedMiddlewares.clear();
		} finally {
			writeLock.unlock();
		}
		logger.info("HTTP server stopped.");
	}

	// --- COMMON HTTP REQUEST PROCESSOR ---

	public Promise processRequest(String httpMethod, String path, Tree headers, String query, byte[] body) {
		try {

			// Try to find in static mappings (eg. "/user")
			Mapping mapping;
			readLock.lock();
			try {
				mapping = staticMappings.get(path);
				if (mapping == null) {

					// Try to find in dynamic mappings (eg. "/user/:id")
					for (Mapping dynamicMapping : dynamicMappings) {
						if (dynamicMapping.matches(path)) {
							mapping = dynamicMapping;
							break;
						}
					}
				}
			} finally {
				readLock.unlock();
			}

			// Invoke mapping
			if (mapping != null) {
				Promise response = mapping.processRequest(httpMethod, path, headers, query, body);
				if (response != null) {
					return response;
				}
			}

			// Find in routes
			for (Route route : routes) {
				mapping = route.findMapping(httpMethod, path);
				if (mapping != null) {
					if (!checkedMiddlewares.isEmpty()) {
						mapping.use(checkedMiddlewares);
					}
					break;
				}
			}
			if (mapping != null) {

				// Store new mapping
				writeLock.lock();
				try {
					if (mapping.isStatic()) {
						if (!staticMappings.containsValue(mapping)) {
							staticMappings.put(mapping.getPathPrefix(), mapping);
						}
					} else {
						if (!dynamicMappings.contains(mapping)) {
							dynamicMappings.addLast(mapping);
							if (dynamicMappings.size() > cachedRoutes) {
								dynamicMappings.removeFirst();
							}
						}
					}
				} finally {
					writeLock.unlock();
				}

				Promise response = mapping.processRequest(httpMethod, path, headers, query, body);
				if (response != null) {
					return response;
				}
			}

			// 404 Not Found
			Tree notFound = new Tree();
			notFound.getMeta().put("response.status", 404);
			notFound.put("message", "Not Found: " + path);
			return Promise.resolve(notFound);

		} catch (Throwable cause) {
			logger.error("Unable to process request!", cause);
			return Promise.reject(cause);
		}
	}

	// --- GLOBAL MIDDLEWARES ---

	protected HashSet<Middleware> checkedMiddlewares = new HashSet<>(32);

	public void use(Middleware... middlewares) {
		use(Arrays.asList(middlewares));
	}

	public void use(Collection<Middleware> middlewares) {
		LinkedList<Middleware> newMiddlewares = new LinkedList<>();
		for (Middleware middleware : middlewares) {
			if (checkedMiddlewares.add(middleware)) {
				newMiddlewares.addLast(middleware);
			}
		}
		if (!newMiddlewares.isEmpty()) {
			readLock.lock();
			try {
				if (staticMappings != null) {
					for (Mapping mapping : staticMappings.values()) {
						mapping.use(newMiddlewares);
					}
				}
				for (Mapping mapping : dynamicMappings) {
					mapping.use(newMiddlewares);
				}
			} finally {
				readLock.unlock();
			}
		}
	}

	// --- ADD ROUTE ---

	/**
	 * Define a route for a list of Services with the specified path prefix (eg.
	 * if the path is "/rest-services" and service list is "service1", the
	 * service's "func" action will available on
	 * "http://host:port/rest-services/service1/func").
	 * 
	 * @param path
	 *            path prefix for all enumerated services (eg. "/rest-services")
	 * @param serviceList
	 *            list of services (eg. "service1,service2,service3")
	 * @param middlewares
	 *            optional middlewares (eg. CorsHeaders)
	 */
	public void addRoute(String path, String serviceList, Middleware... middlewares) {
		String[] serviceNames = serviceList.split(",");
		LinkedList<String> list = new LinkedList<>();
		for (String serviceName : serviceNames) {
			serviceName = '/' + serviceName.trim() + "*";
			if (serviceName.length() > 2) {
				list.addLast(serviceName);
			}
		}
		String[] whiteList = new String[list.size()];
		list.toArray(whiteList);
		Route route = new Route(this, path, MappingPolicy.RESTRICT, null, whiteList, null);
		if (middlewares != null && middlewares.length > 0) {
			route.use(middlewares);
		}
		addRoute(route);
	}

	/**
	 * Define a route to a single action. The action will available on the
	 * specified path.
	 * 
	 * @param httpMethod
	 *            HTTP method (eg. "GET", "POST", "ALL", "REST", etc.)
	 * @param path
	 *            path of the action (eg. "numbers/add" creates an endpoint on
	 *            "http://host:port/numbers/add")
	 * @param actionName
	 *            name of action (eg. "math.add")
	 * @param middlewares
	 *            optional middlewares (eg. CorsHeaders)
	 */
	public void addRoute(String httpMethod, String path, String actionName, Middleware... middlewares) {
		Alias alias = new Alias(httpMethod, path, actionName);
		Route route = new Route(this, "", MappingPolicy.RESTRICT, null, null, new Alias[] { alias });
		if (middlewares != null && middlewares.length > 0) {
			route.use(middlewares);
		}
		addRoute(route);
	}

	/**
	 * Adds a route to the list of routes.
	 * 
	 * @param route
	 *            the new route
	 */
	public void addRoute(Route route) {
		writeLock.lock();
		try {
			Route[] copy = new Route[routes.length + 1];
			System.arraycopy(routes, 0, copy, 0, routes.length);
			copy[routes.length] = route;
			routes = copy;
			if (staticMappings != null) {
				staticMappings.clear();
			}
			dynamicMappings.clear();
		} finally {
			writeLock.unlock();
		}
	}

	// --- PROPERTY GETTERS AND SETTERS ---

	public Route[] getRoutes() {
		return routes;
	}

	public void setRoutes(Route[] routes) {
		this.routes = Objects.requireNonNull(routes);
		writeLock.lock();
		try {
			if (staticMappings != null) {
				staticMappings.clear();
			}
			dynamicMappings.clear();
		} finally {
			writeLock.unlock();
		}
	}

	public int getCachedRoutes() {
		return cachedRoutes;
	}

	public void setCachedRoutes(int cacheSize) {
		this.cachedRoutes = cacheSize;
	}

}