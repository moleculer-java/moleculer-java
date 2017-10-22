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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.cacher.Cache;
import services.moleculer.strategy.Strategy;
import services.moleculer.strategy.StrategyFactory;

/**
 * Default implementation of the Service Registry.
 */
@Name("Default Service Registry")
public final class DefaultServiceRegistry extends ServiceRegistry implements Runnable {

	// --- REGISTERED SERVICES ---

	private final HashMap<String, Service> services = new HashMap<>(256);

	// --- REGISTERED ACTIONS ---

	private final HashMap<String, Strategy> strategies = new HashMap<>(256);

	// --- PENDING REMOTE INVOCATIONS ---

	private final HashMap<String, PromiseContainer> promises = new HashMap<>(256);

	// --- COMPONENTS ---

	private ServiceBroker broker;
	private StrategyFactory strategy;
	private ExecutorService executor;

	// --- PROPERTIES ---

	/**
	 * Invoke local service via Thread pool or directly
	 */
	private boolean asyncLocalInvocation;

	/**
	 * Reader lock of configuration
	 */
	private final Lock configReadLock;

	/**
	 * Writer lock of configuration
	 */
	private final Lock configWriteLock;

	/**
	 * Reader lock of configuration
	 */
	private final Lock promiseReadLock;

	/**
	 * Writer lock of configuration
	 */
	private final Lock promiseWriteLock;

	// --- CONSTRUCTORS ---

	public DefaultServiceRegistry() {
		this(false);
	}

	public DefaultServiceRegistry(boolean asyncLocalInvocation) {

		// Async or direct local invocation
		this.asyncLocalInvocation = asyncLocalInvocation;

		// Create locks
		ReentrantReadWriteLock configLock = new ReentrantReadWriteLock(true);
		configReadLock = configLock.readLock();
		configWriteLock = configLock.writeLock();

		ReentrantReadWriteLock promiseLock = new ReentrantReadWriteLock(true);
		promiseReadLock = promiseLock.readLock();
		promiseWriteLock = promiseLock.writeLock();
	}

	// --- INIT SERVICE REGISTRY ---

	/**
	 * Cancelable timer
	 */
	private volatile ScheduledFuture<?> timer;

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
		long cleanup = config.get("cleanup", 1L);

		// Node-style Service Registry config?
		Tree parent = config.getParent();
		if (parent != null && (parent.get("strategy", (String) null) != null
				|| parent.get("preferLocal", (String) null) != null)) {
			logger.warn("Service Registry has no \"strategy\" or \"preferLocal\" properties.");
		}

		// Set components
		this.broker = broker;
		this.strategy = broker.components().strategy();
		this.executor = broker.components().executor();

		// Start timeout timer
		timer = broker.components().scheduler().scheduleWithFixedDelay(this, cleanup, cleanup, TimeUnit.SECONDS);
	}

	// --- STOP SERVICE REGISTRY ---

	@Override
	public final void stop() {

		// Stop timer
		if (timer != null) {
			timer.cancel(false);
			timer = null;
		}

		// Stop pending invocations
		// TODO Use executor
		InterruptedException error = new InterruptedException("Registry is shutting down.");
		promiseWriteLock.lock();
		try {
			for (PromiseContainer container : promises.values()) {
				container.complete(error);
			}
			promises.clear();
		} finally {
			promiseWriteLock.unlock();
		}

		// Stop action containers and services
		configWriteLock.lock();
		try {

			// Stop strategies (and registered actions)
			for (Strategy containers : strategies.values()) {
				try {
					containers.stop();
				} catch (Throwable cause) {
					logger.warn("Unable to stop strategy!", cause);
				}
			}
			strategies.clear();

			// Stop registered services
			for (Service service : services.values()) {
				try {
					service.stop();
					logger.info("Service \"" + service.name + "\" stopped.");
				} catch (Throwable cause) {
					logger.warn("Unable to stop \"" + service.name + "\" service!", cause);
				}
			}
			services.clear();

		} finally {
			configWriteLock.unlock();
		}
	}

	// --- REGISTER PROMISE ---

	final void register(String id, Promise promise, long timeout) {
		final PromiseContainer container = new PromiseContainer(promise, timeout);
		promiseWriteLock.lock();
		try {
			promises.put(id, container);
		} finally {
			promiseWriteLock.unlock();
		}
	}

	// --- RECEIVE RESPONSE FROM REMOTE SERVICE ---

	@Override
	public final void receive(Tree message) {
		String id = message.get("id", "");
		if (id.isEmpty()) {
			logger.warn("Missing \"id\" property!", message);
			return;
		}
		PromiseContainer container;
		promiseWriteLock.lock();
		try {
			container = promises.remove(id);
		} finally {
			promiseWriteLock.unlock();
		}
		if (container == null) {
			logger.warn("Missing (maybe timeouted) message!", message);
			return;
		}
		try {
			String error = message.get("error", "");
			if (!error.isEmpty()) {

				// Error response
				container.complete(new RemoteException(error));
				return;
			}
			Tree response = message.get("response");
			container.complete(response);
		} catch (Throwable cause) {
			container.complete(cause);
		}
	}

	// --- TIMEOUT HANDLER ---

	public final void run() {
		
		// TODO Simplify code (one lock object is enough)
		HashMap<String, PromiseContainer> removables = new HashMap<>();
		long now = System.currentTimeMillis();
		promiseReadLock.lock();
		try {
			PromiseContainer container;
			for (Map.Entry<String, PromiseContainer> entry : promises.entrySet()) {
				container = entry.getValue();
				if (container.timeout > 0 && now - container.created > container.timeout) {
					removables.put(entry.getKey(), container);
				}
			}
		} finally {
			promiseReadLock.unlock();
		}
		if (!removables.isEmpty()) {
			TimeoutException error = new TimeoutException("Invocation timeouted!");
			for (PromiseContainer container : removables.values()) {
				executor.execute(() -> {
					container.complete(error);
				});
			}
			promiseWriteLock.lock();
			try {
				for (String id : removables.keySet()) {
					promises.remove(id);
				}
			} finally {
				promiseWriteLock.unlock();
			}
		}
	}

	// --- ADD LOCAL SERVICE ---

	@Override
	public final void addService(Service service, Tree config) throws Exception {
		configWriteLock.lock();
		try {

			// Initialize actions in services
			Class<? extends Service> clazz = service.getClass();
			Field[] fields = clazz.getFields();
			for (Field field : fields) {
				if (Action.class.isAssignableFrom(field.getType())) {
					String name = field.getName();
					Tree actionConfig = config.get(name);
					if (actionConfig == null) {
						if (config.isMap()) {
							actionConfig = config.putMap(name);
						} else {
							actionConfig = new Tree();
						}
					}

					// Name of the action (eg. "v2.service.add")
					name = service.name + '.' + name;
					actionConfig.put("name", name);

					// Process "Cache" annotation
					if (actionConfig.get("cached") == null) {
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
						actionConfig.put("cached", cached);
						if (keys != null && keys.length > 0) {
							actionConfig.put("cacheKeys", String.join(",", keys));
						}
					}

					// Register actions
					field.setAccessible(true);
					Action action = (Action) field.get(service);
					LocalActionContainer container = new LocalActionContainer(action, asyncLocalInvocation);
					Strategy actionStrategy = strategies.get(name);
					if (actionStrategy == null) {
						actionStrategy = strategy.create();
						actionStrategy.start(broker, actionConfig);
						strategies.put(name, actionStrategy);
					}
					actionStrategy.add(container, actionConfig);
					container.start(broker, actionConfig);
				}
			}

			// Start service
			service.start(broker, config);
			services.put(service.name, service);

		} finally {
			configWriteLock.unlock();
		}
	}

	// --- GET SERVICE ---

	@Override
	public final Service getService(String name) {
		configReadLock.lock();
		try {
			return services.get(name);
		} finally {
			configReadLock.unlock();
		}
	}

	// --- GET ACTION ---

	@Override
	public final ActionContainer getAction(String name, String nodeID) {
		Strategy containers;
		configReadLock.lock();
		try {
			containers = strategies.get(name);
		} finally {
			configReadLock.unlock();
		}
		if (containers == null) {
			return null;
		}
		return containers.get(nodeID);
	}

}