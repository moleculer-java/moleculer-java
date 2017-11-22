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

import java.io.PrintWriter;
import java.io.StringWriter;
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
import java.util.Objects;
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
import services.moleculer.context.CallingOptions;
import services.moleculer.eventbus.EventBus;
import services.moleculer.strategy.Strategy;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.transporter.Transporter;

/**
 * Default implementation of the Service Registry.
 */
@Name("Default Service Registry")
public final class DefaultServiceRegistry extends ServiceRegistry implements Runnable {

	// --- REGISTERED LOCAL SERVICES ---

	private final HashMap<String, Service> services = new HashMap<>(64);

	// --- REGISTERED STRATEGIES PER ACTIONS ---

	private final HashMap<String, Strategy<ActionEndpoint>> strategies = new HashMap<>(256);

	// --- PENDING REMOTE INVOCATIONS ---

	private final ConcurrentHashMap<String, PendingPromise> promises = new ConcurrentHashMap<>(8192);

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
	private int cleanup = 1;

	/**
	 * Reader lock of configuration
	 */
	private final Lock readLock;

	/**
	 * Writer lock of configuration
	 */
	private final Lock writeLock;

	// --- LOCAL NODE ID ---

	private String nodeID;

	// --- COMPONENTS ---

	private ServiceBroker broker;
	private StrategyFactory strategy;
	private ScheduledExecutorService scheduler;
	private Transporter transporter;
	private EventBus eventbus;

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
	public final void start(ServiceBroker broker, Tree config) throws Exception {

		// Process config
		asyncLocalInvocation = config.get(ASYNC_LOCAL_INVOCATION, asyncLocalInvocation);
		cleanup = config.get(CLEANUP, cleanup);
		defaultTimeout = config.get(DEFAULT_TIMEOUT, defaultTimeout);

		// Node-style Service Registry config?
		Tree parent = config.getParent();
		if (parent != null
				&& (parent.get(STRATEGY, (String) null) != null || parent.get(PREFER_LOCAL, (String) null) != null)) {
			logger.warn("Service Registry has no \"" + STRATEGY + "\" or \"" + PREFER_LOCAL + "\" properties.");
		}

		// Local nodeID
		this.nodeID = broker.nodeID();

		// Set components
		this.broker = broker;
		this.strategy = broker.components().strategy();
		this.scheduler = broker.components().scheduler();
		this.transporter = broker.components().transporter();
		this.eventbus = broker.components().eventbus();
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
		for (PendingPromise pending : promises.values()) {
			pending.promise.complete(error);
		}

		// Stop action endpoints and services
		writeLock.lock();
		try {

			// Stop strategies (and registered actions)
			for (Strategy<ActionEndpoint> strategy : strategies.values()) {
				try {
					strategy.stop();
				} catch (Throwable cause) {
					logger.warn("Unable to stop strategy!", cause);
				}
			}
			strategies.clear();

			// Stop registered services
			stopAllLocalServices();

		} finally {
			writeLock.unlock();
		}
	}

	// --- CALL TIMEOUT CHECKER TASK ---

	public final void run() {
		long now = System.currentTimeMillis();
		PendingPromise pending;
		Iterator<PendingPromise> i = promises.values().iterator();
		boolean removed = false;
		Exception error = new TimeoutException("Action invocation timeouted!");
		while (i.hasNext()) {
			pending = i.next();
			if (pending.timeoutAt > 0 && now >= pending.timeoutAt) {
				pending.promise.complete(error);
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
			for (PendingPromise pending : promises.values()) {
				if (pending.timeoutAt > 0 && pending.timeoutAt < minTimeoutAt) {
					minTimeoutAt = pending.timeoutAt;
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
		promises.put(id, new PendingPromise(promise, timeoutAt));

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

	// --- RECEIVE REQUEST FROM REMOTE SERVICE ---

	public final void receiveRequest(Tree message) {

		// Verify Moleculer version
		int ver = message.get(VER, -1);
		if (ver != ServiceBroker.MOLECULER_VERSION) {
			logger.warn("Invalid message version (" + ver + ")!");
			return;
		}

		// Get action property
		String action = message.get("action", (String) null);
		if (action == null || action.isEmpty()) {
			logger.warn("Missing \"action\" property!");
			return;
		}

		// Get strategy (action endpoint array) by action name
		Strategy<ActionEndpoint> strategy;
		readLock.lock();
		try {
			strategy = strategies.get(action);
		} finally {
			readLock.unlock();
		}
		if (strategy == null) {
			logger.warn("Invalid action name (" + action + ")!");
			return;
		}

		// Get local action endpoint (with cache handling)
		ActionEndpoint endpoint = strategy.getLocalEndpoint();
		if (endpoint == null) {
			logger.warn("Not a local action (" + action + ")!");
			return;
		}

		// Get request's unique ID
		String id = message.get("id", (String) null);
		if (id == null || id.isEmpty()) {
			logger.warn("Missing \"id\" property!");
			return;
		}

		// Get sender's nodeID
		String sender = message.get(SENDER, (String) null);
		if (sender == null || sender.isEmpty()) {
			logger.warn("Missing \"sender\" property!");
			return;
		}

		// Create CallingOptions
		int timeout = message.get("timeout", 0);
		Tree params = message.get(PARAMS);

		// TODO Process other properties:
		//
		// Tree meta = message.get("meta");
		// int level = message.get("level", 1);
		// boolean metrics = message.get("metrics", false);
		// String parentID = message.get("parentID", (String) null);
		// String requestID = message.get("requestID", (String) null);

		CallingOptions opts = new CallingOptions(nodeID, timeout, 0);

		// Invoke action
		try {
			endpoint.call(params, opts, null).then(data -> {

				// Send response
				Tree response = new Tree();
				response.put("id", id);
				response.put(VER, ServiceBroker.MOLECULER_VERSION);
				response.put("success", true);
				response.putObject("data", data);
				transporter.publish(Transporter.PACKET_RESPONSE, sender, response);

			}).Catch(error -> {

				// Send error
				transporter.publish(Transporter.PACKET_RESPONSE, sender, throwableToTree(id, error));

			});
		} catch (Throwable error) {

			// Send error
			transporter.publish(Transporter.PACKET_RESPONSE, sender, throwableToTree(id, error));

		}
	}

	private final Tree throwableToTree(String id, Throwable error) {
		Tree response = new Tree();
		response.put("id", id);
		response.put(VER, ServiceBroker.MOLECULER_VERSION);
		response.put("success", false);
		response.put("data", (String) null);
		if (error != null) {

			// Add message
			Tree errorMap = response.putMap("error");
			errorMap.put("message", error.getMessage());

			// Add trace
			StringWriter sw = new StringWriter(128);
			PrintWriter pw = new PrintWriter(sw);
			error.printStackTrace(pw);
			errorMap.put("trace", sw.toString());

		}
		return response;
	}

	// --- RECEIVE RESPONSE FROM REMOTE SERVICE ---

	@Override
	public final void receiveResponse(Tree message) {

		// Verify Moleculer version
		int ver = message.get(VER, -1);
		if (ver != ServiceBroker.MOLECULER_VERSION) {
			logger.warn("Invalid version:\r\n" + message);
			return;
		}

		// Get response's unique ID
		String id = message.get("id", (String) null);
		if (id == null || id.isEmpty()) {
			logger.warn("Missing \"id\" property!", message);
			return;
		}

		// Get stored promise
		PendingPromise pending = promises.remove(id);
		if (pending == null) {
			logger.warn("Unknown (maybe timeouted) response received!", message);
			return;
		}
		try {

			// Get response status (successed or not?)
			boolean success = message.get("success", true);
			if (success) {

				// Ok -> resolve
				pending.promise.complete(message.get("data"));

			} else {

				// Failed -> reject
				Tree error = message.get("error");
				String errorMessage = null;
				String trace = null;
				if (error != null) {
					errorMessage = error.get("message", (String) null);
					trace = error.get("trace", (String) null);
					if (trace != null && !trace.isEmpty()) {
						logger.error("Remote invaction failed!\r\n" + trace);
					}
				}
				if (errorMessage == null || errorMessage.isEmpty()) {
					errorMessage = "Unknow error!";
				}
				if (trace == null || trace.isEmpty()) {
					logger.error("Remote invaction failed (unknown error occured)!");
				}
				pending.promise.complete(new RemoteException(errorMessage));
				return;
			}
		} catch (Throwable cause) {
			logger.error("Unable to pass on incoming response!", cause);
		}
	}

	// --- ADD A LOCAL SERVICE ---

	@Override
	public final void addActions(Service service, Tree config) throws Exception {
		writeLock.lock();
		try {

			// Initialize actions in services
			Class<? extends Service> clazz = service.getClass();
			Field[] fields = clazz.getFields();
			for (Field field : fields) {

				// Register action
				if (Action.class.isAssignableFrom(field.getType())) {
					String actionName = field.getName();
					Tree actionConfig = config.get(actionName);
					if (actionConfig == null) {
						if (config.isMap()) {
							actionConfig = config.putMap(actionName);
						} else {
							actionConfig = new Tree();
						}
					}

					// Name of the action (eg. "v2.service.add")
					actionName = service.name + '.' + actionName;
					actionConfig.put(NAME, actionName);

					// Process "Cache" annotation
					if (actionConfig.get(CACHE) == null) {
						Cache cache = field.getAnnotation(Cache.class);
						boolean cached = false;
						String[] keys = null;
						int ttl = 0;
						if (cache != null) {
							cached = true;
							if (cached) {
								keys = cache.keys();
								if (keys != null && keys.length == 0) {
									keys = null;
								}
								ttl = cache.ttl();
							}
						}
						actionConfig.put(CACHE, cached);
						if (ttl > 0) {
							actionConfig.put(TTL, ttl);
						}
						if (keys != null && keys.length > 0) {
							actionConfig.put(CACHE_KEYS, String.join(",", keys));
						}
					}

					// Register actions
					field.setAccessible(true);
					Action action = (Action) field.get(service);
					LocalActionEndpoint endpoint = new LocalActionEndpoint(this, action, asyncLocalInvocation);
					endpoint.start(broker, actionConfig);
					Strategy<ActionEndpoint> actionStrategy = strategies.get(actionName);
					if (actionStrategy == null) {
						actionStrategy = strategy.create();
						actionStrategy.start(broker, actionConfig);
						strategies.put(actionName, actionStrategy);
					}
					actionStrategy.addEndpoint(endpoint);
				}
			}

			// Start service
			service.start(broker, config);
			services.put(service.name, service);

		} finally {
			writeLock.unlock();
		}
	}

	// --- ADD A REMOTE SERVICE ---

	@Override
	public final void addActions(Tree config) throws Exception {
		Tree actions = config.get(ACTIONS);
		if (actions != null && actions.isMap()) {
			String nodeID = Objects.requireNonNull(config.get(NODE_ID, (String) null));
			writeLock.lock();
			try {
				for (Tree actionConfig : actions) {
					actionConfig.putObject(NODE_ID, nodeID, true);
					String actionName = actionConfig.get(NAME, "");

					// Register remote action
					RemoteActionEndpoint endpoint = new RemoteActionEndpoint(this);
					endpoint.start(broker, actionConfig);
					Strategy<ActionEndpoint> actionStrategy = strategies.get(actionName);
					if (actionStrategy == null) {
						actionStrategy = strategy.create();
						actionStrategy.start(broker, actionConfig);
						strategies.put(actionName, actionStrategy);
					}
					actionStrategy.addEndpoint(endpoint);
				}
			} finally {
				writeLock.unlock();
			}
		}
	}

	// --- REMOVE ALL REMOTE SERVICES/ACTIONS OF A NODE ---

	@Override
	public final void removeActions(String nodeID) {
		writeLock.lock();
		try {
			Iterator<Strategy<ActionEndpoint>> endpoints = strategies.values().iterator();
			while (endpoints.hasNext()) {
				Strategy<ActionEndpoint> strategy = endpoints.next();
				strategy.remove(nodeID);
				if (strategy.isEmpty()) {
					try {
						strategy.stop();
					} catch (Throwable cause) {
						logger.warn("Unable to stop strategy!", cause);
					}
					endpoints.remove();
				}
			}
			if (broker.nodeID().equals(nodeID)) {
				stopAllLocalServices();
			}
		} finally {
			writeLock.unlock();
		}
	}

	private final void stopAllLocalServices() {
		for (Service service : services.values()) {
			try {
				service.stop();
				logger.info("Service \"" + service.name + "\" stopped.");
			} catch (Throwable cause) {
				logger.warn("Unable to stop \"" + service.name + "\" service!", cause);
			}
		}
		services.clear();
	}

	// --- GET LOCAL SERVICE ---

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

	// --- GET LOCAL OR REMOTE ACTION CONTAINER ---

	@Override
	public final ActionEndpoint getAction(String name, String nodeID) {
		Strategy<ActionEndpoint> strategy;
		readLock.lock();
		try {
			strategy = strategies.get(name);
		} finally {
			readLock.unlock();
		}
		if (strategy == null) {
			throw new NoSuchElementException("Invalid action name (" + name + ")!");
		}
		ActionEndpoint endpoint = strategy.getEndpoint(nodeID);
		if (endpoint == null) {
			throw new NoSuchElementException("Invalid nodeID (" + nodeID + ")!");
		}
		return endpoint;
	}

	// --- GENERATE SERVICE DESCRIPTOR ---

	@Override
	public final Tree generateDescriptor() {
		Tree root = new Tree();

		// Protocol version
		root.put(VER, ServiceBroker.MOLECULER_VERSION);

		// NodeID
		String nodeID = broker.nodeID();
		root.put(SENDER, nodeID);

		// Services array
		Tree services = root.putList(SERVICES);
		Tree servicesMap = new Tree();
		readLock.lock();
		try {
			for (Map.Entry<String, Strategy<ActionEndpoint>> entry : strategies.entrySet()) {

				// Split into parts ("math.add" -> "math" and "add")
				String name = entry.getKey();
				int i = name.lastIndexOf('.');
				String service = name.substring(0, i);

				// Get endpoint
				ActionEndpoint endpoint = entry.getValue().getLocalEndpoint();
				if (endpoint == null) {
					continue;
				}

				// Service block
				Tree serviceMap = servicesMap.putMap(service, true);
				serviceMap.put(NAME, service);

				// Not used
				serviceMap.putMap(SETTINGS);
				serviceMap.putMap(METADATA);
				serviceMap.put(NODE_ID, nodeID);

				// Action block
				@SuppressWarnings("unchecked")
				Map<String, Object> actionBlock = (Map<String, Object>) serviceMap.putMap(ACTIONS, true).asObject();
				LinkedHashMap<String, Object> map = new LinkedHashMap<>();
				actionBlock.put(name, map);
				Tree actionMap = new Tree(map);

				actionMap.put(NAME, name);
				boolean cached = endpoint.cached();
				actionMap.put(CACHE, cached);
				if (cached) {
					String[] keys = endpoint.cacheKeys();
					if (keys != null) {
						Tree cacheKeys = actionMap.putList(CACHE_KEYS);
						for (String key : keys) {
							cacheKeys.add(key);
						}
					}
				}

				// Listener block
				Tree listeners = eventbus.generateListenerDescriptor(service);
				if (listeners != null && !listeners.isEmpty()) {
					serviceMap.putMap("events").assign(listeners);
				}

				// Not used
				actionMap.putMap(PARAMS);

			}
		} finally {
			readLock.unlock();
		}
		for (Tree service : servicesMap) {
			services.addObject(service);
		}

		// IP array
		Tree ipList = root.putList(IP_LIST);
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
		Tree client = root.putMap(CLIENT);
		client.put(TYPE, "java");
		client.put(VERSION, ServiceBroker.IMPLEMENTATION_VERSION);
		client.put(LANG_VERSION, System.getProperty("java.version", "1.8"));

		// Port (reserved)
		root.put(PORT, (String) null);

		// Config (not used in this version)
		root.putMap(CONFIG);

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

	public final void setDefaultTimeout(int defaultTimeoutSeconds) {
		this.defaultTimeout = defaultTimeoutSeconds;
	}

	public final int getCleanup() {
		return cleanup;
	}

	public final void setCleanup(int cleanupSeconds) {
		this.cleanup = cleanupSeconds;
	}

}