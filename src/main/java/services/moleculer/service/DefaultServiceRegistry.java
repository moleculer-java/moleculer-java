/**
 * This software is licensed under MIT license.<br>
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
package services.moleculer.service;

import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.cacher.Cache;
import services.moleculer.context.CallingOptions;
import services.moleculer.context.Context;
import services.moleculer.context.ContextFactory;
import services.moleculer.transporter.Transporter;

/**
 * 
 */
@Name("Default Service Registry")
public final class DefaultServiceRegistry extends ServiceRegistry {

	// --- SERVICE MAP ---

	private final HashMap<String, Service> serviceMap = new HashMap<>(256);

	// --- COMPONENTS ---

	private ServiceBroker broker;
	private Executor executor;
	private ContextFactory context;
	private Transporter transporter;

	// --- PROPERTIES ---

	/**
	 * Invoke local service via ExecutorService
	 */
	private boolean asyncLocalInvocation;

	/**
	 * Reader lock
	 */
	private final Lock readerLock;

	/**
	 * Writer lock
	 */
	private final Lock writerLock;

	// --- PROMISES OF REMOTE ACTION INVOCATIONS ---

	private final ConcurrentHashMap<String, Promise> pendingPromises = new ConcurrentHashMap<>();

	// --- CONSTRUCTORS ---

	public DefaultServiceRegistry() {
		this(false);
	}

	public DefaultServiceRegistry(boolean asyncLocalInvocation) {

		// Async or direct local invocation
		this.asyncLocalInvocation = asyncLocalInvocation;

		// Create locks
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
		readerLock = lock.readLock();
		writerLock = lock.writeLock();
	}

	// --- INIT SERVICE REGISTRY ---

	/**
	 * Initializes default ServiceRegistry instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {

		// Process config
		asyncLocalInvocation = config.get("asyncLocalInvocation", asyncLocalInvocation);
		
		// Node-style Service Registry config?
		Tree parent = config.getParent();
		if (parent != null && (parent.get("strategy", (String) null) != null
				|| parent.get("preferLocal", (String) null) != null)) {
			logger.warn("Service Registry has no \"strategy\" or \"preferLocal\" properties.");
		}

		// Parent service broker
		this.broker = Objects.requireNonNull(broker);

		// Async or direct local invocation
		if (asyncLocalInvocation) {
			executor = broker.components().executor();
		} else {
			executor = null;
		}

		// Set context factory
		context = Objects.requireNonNull(broker.components().context());

		// Set transporter (can be null)
		transporter = broker.components().transporter();
	}

	// --- STOP SERVICE REGISTRY ---

	@Override
	public final void stop() {
		writerLock.lock();
		try {
			for (Service service : serviceMap.values()) {
				try {
					service.stop();
					logger.info("Service \"" + service.name + "\" stopped.");
				} catch (Throwable cause) {
					logger.warn("Unable to stop \"" + service.name + "\" service!", cause);
				}
			}
			serviceMap.clear();
			pendingPromises.clear();
		} finally {
			writerLock.unlock();
		}
	}

	// --- CALL LOCAL SERVICE ---

	@Override
	public final Promise call(Action action, Tree params, CallingOptions opts) {

		// Create new context
		final Context ctx = context.create(params, opts);

		// A.) Invoke local action via thread pool
		if (asyncLocalInvocation) {
			return new Promise(CompletableFuture.supplyAsync(() -> {
				try {
					return action.handler(ctx);
				} catch (Throwable error) {
					return error;
				}
			}, executor));
		}

		// B.) In-process (direct) action invocation
		try {
			return new Promise(action.handler(ctx));
		} catch (Throwable error) {
			return Promise.reject(error);
		}
	}

	// --- SEND REQUEST TO REMOTE SERVICE ---

	@Override
	public Promise send(String name, Tree params, CallingOptions opts) {
		Context ctx = context.create(params, opts);
		String id = ctx.id();
		if (id == null) {

			// Local service
			ActionContainer actionContainer = getAction(null, name);
			if (actionContainer == null) {
				return Promise.reject(new IllegalArgumentException("Invalid action name (\"" + name + "\")!"));
			}
			return actionContainer.call(params, opts);
		}

		// TODO Create Tree by context
		Tree message = new Tree();
		message.put("id", id);
		message.put("name", name);

		String targetNodeID = null;
		if (opts != null) {
			targetNodeID = opts.nodeID();
			message.put("nodeID", targetNodeID);
		}

		// ...
		
		// Store promise (context ID -> promise)
		Promise p = new Promise();
		pendingPromises.put(id, p);

		// Send to transporter
		transporter.publish(Transporter.PACKET_REQUEST, targetNodeID, message);

		// Return promise
		return p;
	}

	// --- RECEIVE RESPONSE FROM REMOTE SERVICE ---

	@Override
	public void receive(Tree message) {
		String id = message.get("id", "");
		if (id.isEmpty()) {
			logger.warn("Missing \"id\" property!", message);
			return;
		}
		Promise promise = pendingPromises.remove(id);
		if (promise == null) {
			logger.warn("Missing (maybe timeouted) message!", message);
			return;
		}

		// TODO Convert Tree to Object or Exception
		try {
			String error = message.get("error", "");
			if (!error.isEmpty()) {

				// Error response
				promise.complete(new RemoteException(error));
				return;
			}
			Tree response = message.get("response");
			promise.complete(response);
		} catch (Throwable cause) {
			promise.complete(cause);
		}
	}

	// --- ADD LOCAL SERVICE ---

	@Override
	public final void addService(Service service, Tree config) throws Exception {
		writerLock.lock();
		try {

			// Initialize actions in services
			Class<? extends Service> clazz = service.getClass();
			Field[] fields = clazz.getFields();
			for (Field field : fields) {
				if (Action.class.isAssignableFrom(field.getType())) {
					Tree parameters = new Tree();

					// Name of the action (eg. "v2.service.add")
					String name = service.name + '.' + field.getName();
					parameters.put("name", name);

					// Process "Cache" annotation
					Cache cache = field.getAnnotation(Cache.class);
					boolean cached = false;
					String[] keys = null;
					if (cache != null) {
						cached = true;
						if (cached) {
							keys = cache.value();
							if (keys != null && keys.length == 0) {
								keys = null;
							}
						}
					}
					parameters.put("cached", cached);
					if (keys != null && keys.length > 0) {
						parameters.put("cacheKeys", String.join(",", keys));
					}

					// TODO register actions
					LocalActionContainer container = new LocalActionContainer(broker, parameters,
							(Action) field.get(service));
				}
			}

			// Start service
			service.start(broker, config);
			serviceMap.put(service.name, service);

		} finally {
			writerLock.unlock();
		}
	}

	// --- ADD REMOTE ACTION ---

	@Override
	public final void addAction(Tree parameters) throws Exception {

		// TODO register action
		RemoteActionContainer container = new RemoteActionContainer(broker, parameters);
	}

	// --- GET SERVICE ---

	@Override
	public final Service getService(String name) {
		readerLock.lock();
		try {
			return serviceMap.get(name);
		} finally {
			readerLock.unlock();
		}
	}

	// --- REMOVE SERVICE ---

	@Override
	public final void removeService(Service service) {
		writerLock.lock();
		try {
			Service removed = serviceMap.remove(service.name);
			if (removed != null) {
				try {
					removed.stop();
					logger.info("Service \"" + removed.name + "\" stopped.");
				} catch (Exception cause) {
					logger.warn("Service removed, but it threw an exception in the \"close\" method!", cause);
				}
			}
		} finally {
			writerLock.unlock();
		}
	}

	// --- GET ACTION ---

	@Override
	public final ActionContainer getAction(String nodeID, String name) {
		readerLock.lock();
		try {

			// TODO find action
			return null;

		} finally {
			readerLock.unlock();
		}
	}

}