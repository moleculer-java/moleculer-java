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
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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

	private final HashMap<String, Service> services = new HashMap<>(64);

	// --- REGISTERED ACTIONS ---

	private final HashMap<String, Strategy> strategies = new HashMap<>(256);

	// --- PENDING REMOTE INVOCATIONS ---

	private final ConcurrentHashMap<String, PromiseContainer> promises = new ConcurrentHashMap<>(8192);

	// --- PROPERTIES ---

	/**
	 * Invoke all local services via Thread pool (true) or directly (false)
	 */
	private boolean asyncLocalInvocation;

	/**
	 * Default action invocation timeout (seconds)
	 */
	private int defaultTimeout;

	/**
	 * Timeout-checker's period delay (seconds)
	 */
	private int cleanupDelay = 1;

	/**
	 * Reader lock of configuration
	 */
	private final Lock readLock;

	/**
	 * Writer lock of configuration
	 */
	private final Lock writeLock;

	// --- COMPONENTS ---

	private ServiceBroker broker;
	private StrategyFactory strategy;
	private ScheduledExecutorService scheduler;

	// --- CONSTRUCTORS ---

	public DefaultServiceRegistry() {
		this(false);
	}

	public DefaultServiceRegistry(boolean asyncLocalInvocation) {

		// Async or direct local invocation
		this.asyncLocalInvocation = asyncLocalInvocation;

		// Create locks
		ReentrantReadWriteLock configLock = new ReentrantReadWriteLock(true);
		readLock = configLock.readLock();
		writeLock = configLock.writeLock();
	}

	// --- START SERVICE REGISTRY ---

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
		cleanupDelay = config.get("cleanupDelay", cleanupDelay);
		defaultTimeout = config.get("defaultTimeout", defaultTimeout);

		// Node-style Service Registry config?
		Tree parent = config.getParent();
		if (parent != null && (parent.get("strategy", (String) null) != null
				|| parent.get("preferLocal", (String) null) != null)) {
			logger.warn("Service Registry has no \"strategy\" or \"preferLocal\" properties.");
		}

		// Set components
		this.broker = broker;
		this.strategy = broker.components().strategy();
		this.scheduler = broker.components().scheduler();
	}

	// --- STOP SERVICE REGISTRY ---

	@Override
	public final void stop() {

		// Stop timer
		ScheduledFuture<?> task = timer.get();
		if (task != null) {
			task.cancel(false);
		}

		// Stop pending invocations
		InterruptedException error = new InterruptedException("Registry is shutting down.");
		for (PromiseContainer container : promises.values()) {
			container.promise.complete(error);
		}
		
		// Stop action containers and services
		writeLock.lock();
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
			writeLock.unlock();
		}
	}

	// --- CALL TIMEOUT CHECKER TASK ---

	public final void run() {
		long now = System.currentTimeMillis();
		PromiseContainer container;
		Iterator<PromiseContainer> i = promises.values().iterator();
		boolean removed = false;
		Exception error = new TimeoutException("Action invocation timeouted!");
		while (i.hasNext()) {
			container = i.next();
			if (container.timeoutAt > 0 && now >= container.timeoutAt) {
				container.promise.complete(error);
				i.remove();
				removed = true;
			}
		}
		if (removed) {
			scheduler.execute(() -> {
				reschedule(Long.MAX_VALUE);				
			});
		} else {
			prevTimeoutAt.set(0);
		}
	}

	// --- SCHEDULER ---

	/**
	 * Cancelable timer
	 */
	private final AtomicReference<ScheduledFuture<?>> timer = new AtomicReference<>();

	/**
	 * Next scheduled time to check timeouts
	 */
	private final AtomicLong prevTimeoutAt = new AtomicLong();

	/**
	 * Recalculates the next timeout checking time
	 */
	private final void reschedule(long minTimeoutAt) {
		if (minTimeoutAt == Long.MAX_VALUE) {
			for (PromiseContainer container : promises.values()) {
				if (container.timeoutAt > 0 && container.timeoutAt < minTimeoutAt) {
					minTimeoutAt = container.timeoutAt;
				}
			}
		}
		long now = System.currentTimeMillis();
		if (minTimeoutAt == Long.MAX_VALUE) {
			ScheduledFuture<?> t = timer.get();
			if (t != null) {
				if (prevTimeoutAt.get() > now) {
					t.cancel(false);
					prevTimeoutAt.set(0);
				} else {
					timer.set(null);
					prevTimeoutAt.set(0);
				}
			}
		} else {
			minTimeoutAt = (minTimeoutAt / 1000 * 1000) + 1000;

			long prev = prevTimeoutAt.getAndSet(minTimeoutAt);
			if (prev == minTimeoutAt) {

				// Next timestamp not changed
				return;
			}

			// Stop previous timer
			ScheduledFuture<?> t = timer.get();
			if (t != null) {
				t.cancel(false);
			}

			// Schedule next timeout timer
			long delay = Math.max(1000, minTimeoutAt - now);
			timer.set(scheduler.schedule(this, delay, TimeUnit.MILLISECONDS));
		}
	}

	// --- REGISTER PROMISE ---

	final void register(String id, Promise promise, long timeoutAt) {
		PromiseContainer container = new PromiseContainer(promise, timeoutAt);
		promises.put(id, container);

		long nextTimeoutAt = prevTimeoutAt.get();
		if (nextTimeoutAt == 0 || (timeoutAt / 1000 * 1000) + 1000 < nextTimeoutAt) {
			scheduler.execute(() -> {
				reschedule(timeoutAt);
			});
		}
	}

	final void deregister(String id) {
		promises.remove(id);
	}

	// --- RECEIVE RESPONSE FROM REMOTE SERVICE ---

	@Override
	public final void receive(Tree message) {
		String id = message.get("id", "");
		if (id.isEmpty()) {
			logger.warn("Missing \"id\" property!", message);
			return;
		}
		PromiseContainer container = promises.remove(id);
		if (container == null) {
			logger.warn("Unknown (maybe timeouted) response received!", message);
			return;
		}
		try {
			String error = message.get("error", "");
			if (!error.isEmpty()) {

				// Error response
				container.promise.complete(new RemoteException(error));
				return;
			}
			Tree response = message.get("response");
			container.promise.complete(response);
		} catch (Throwable error) {
			container.promise.complete(error);
		}
	}

	// --- ADD LOCAL SERVICE ---

	@Override
	public final void addService(Service service, Tree config) throws Exception {
		writeLock.lock();
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
					LocalActionContainer container = new LocalActionContainer(this, action, asyncLocalInvocation);
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
			writeLock.unlock();
		}
	}

	// --- GET SERVICE ---

	@Override
	public final Service getService(String name) {
		Service service;
		readLock.lock();
		try {
			service = services.get(name);
		} finally {
			readLock.unlock();
		}
		if (service == null) {
			throw new NoSuchElementException("Invalid service name (" + name + ")!");
		}
		return service;
	}

	// --- GET ACTION ---

	@Override
	public final ActionContainer getAction(String name, String nodeID) {
		Strategy containers;
		readLock.lock();
		try {
			containers = strategies.get(name);
		} finally {
			readLock.unlock();
		}
		if (containers == null) {
			throw new NoSuchElementException("Invalid action name (" + name + ")!");
		}
		ActionContainer container = containers.get(nodeID);
		if (container == null) {
			throw new NoSuchElementException("Invalid nodeID (" + nodeID + ")!");
		}
		return container;
	}

	// --- GENERATE SERVICE DESCRIPTOR ---

	@Override
	public final Tree generateDescriptor() {
		Tree root = new Tree();

		// Protocol version
		root.put("ver", "2");

		// NodeID
		String nodeID = broker.nodeID();
		root.put("sender", nodeID);

		// Services array
		Tree services = root.putList("services");
		Tree servicesMap = new Tree();
		readLock.lock();
		try {
			for (Map.Entry<String, Strategy> entry : strategies.entrySet()) {

				// Split into parts ("math.add" -> "math" and "add")
				String name = entry.getKey();
				int i = name.lastIndexOf('.');
				String service = name.substring(0, i);

				// Get container
				LocalActionContainer container = (LocalActionContainer) entry.getValue().get(nodeID);
				container.cached();

				// Service block
				Tree serviceMap = servicesMap.putMap(service, true);
				serviceMap.put("name", service);

				// Not used
				serviceMap.putMap("settings");
				serviceMap.putMap("metadata");
				serviceMap.put("nodeID", nodeID);

				// Action block
				@SuppressWarnings("unchecked")
				Map<String, Object> actions = (Map<String, Object>) serviceMap.putMap("actions", true).asObject();
				LinkedHashMap<String, Object> map = new LinkedHashMap<>();
				actions.put(name, map);
				Tree actionMap = new Tree(map);

				actionMap.put("name", name);
				boolean cached = container.cached();
				actionMap.put("cache", cached);
				if (cached) {
					String[] keys = container.cacheKeys();
					if (keys != null) {
						Tree cacheKeys = actionMap.putList("cacheKeys");
						for (String key : keys) {
							cacheKeys.add(key);
						}
					}
				}

				// Not used
				actionMap.putMap("params");

			}
		} finally {
			readLock.unlock();
		}
		for (Tree service : servicesMap) {
			services.addObject(service);
		}

		// IP array
		Tree ipList = root.putList("ipList");
		try {
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
			while (e.hasMoreElements()) {
				NetworkInterface n = (NetworkInterface) e.nextElement();
				Enumeration<InetAddress> ee = n.getInetAddresses();
				while (ee.hasMoreElements()) {
					InetAddress i = (InetAddress) ee.nextElement();
					if (!i.isLoopbackAddress()) {
						ipList.add(i.getHostAddress());
					}
				}
			}
		} catch (Exception ioError) {
			try {
				ipList.add(InetAddress.getLocalHost().getHostAddress());
			} catch (Exception ignored) {
			}
		}

		// Client descriptor
		Tree client = root.putMap("client");
		client.put("type", "java");
		client.put("version", ServiceBroker.VERSION);
		client.put("langVersion", System.getProperty("java.version", "1.8"));

		// Port (reserved)
		root.put("port", (String) null);

		// Config (not used in this version)
		root.putMap("config");

		return root;
	}

	// --- GETTERS / SETTERS ---

	public final boolean isAsyncLocalInvocation() {
		return asyncLocalInvocation;
	}

	public final void setAsyncLocalInvocation(boolean asyncLocalInvocation) {
		this.asyncLocalInvocation = asyncLocalInvocation;
	}

	public final int getDefaultTimeout() {
		return defaultTimeout;
	}

	public final void setDefaultTimeout(int defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

	public final int getCleanupDelay() {
		return cleanupDelay;
	}

	public final void setCleanupDelay(int cleanupDelay) {
		this.cleanupDelay = cleanupDelay;
	}

}