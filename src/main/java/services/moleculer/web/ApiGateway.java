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
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.transporter.Transporter;
import services.moleculer.web.router.HttpConstants;
import services.moleculer.web.router.Mapping;
import services.moleculer.web.router.Route;

@Name("API Gateway")
public abstract class ApiGateway extends Service implements HttpConstants {

	// --- COMPONENTS ---

	protected ServiceRegistry registry;
	protected Transporter transporter;

	// --- ROUTES ---

	protected Route[] routes = new Route[0];

	// --- CACHED MAPPINGS ---

	protected int cachedRoutes = 1024;

	protected final LinkedHashMap<String, Mapping> staticMappings = new LinkedHashMap<>();
	protected final LinkedList<Mapping> dynamicMappings = new LinkedList<>();

	// --- LOCKS ---

	protected final Lock readLock;
	protected final Lock writeLock;

	// --- REQUEST HANDLERS ---
	
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
				Promise response = mapping.processRequest(path, headers, query, body);
				if (response != null) {
					return response;
				}
			}

			// Find in routes
			for (Route route : routes) {
				mapping = route.findMapping(httpMethod, path);
				if (mapping != null) {
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

				Promise response = mapping.processRequest(path, headers, query, body);
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
			return Promise.reject(cause);
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
			staticMappings.clear();
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