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
package services.moleculer.service;

import static services.moleculer.util.CommonUtils.getHostName;
import static services.moleculer.util.CommonUtils.nameOf;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
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
public class DefaultServiceRegistry extends ServiceRegistry implements Runnable {

	// --- REGISTERED LOCAL SERVICES ---

	protected final HashMap<String, Service> services = new HashMap<>(64);

	// --- REGISTERED STRATEGIES PER ACTIONS ---

	protected final HashMap<String, Strategy<ActionEndpoint>> strategies = new HashMap<>(256);

	// --- PENDING REMOTE INVOCATIONS ---

	protected final ConcurrentHashMap<String, PendingPromise> promises = new ConcurrentHashMap<>(8192);

	// --- PROPERTIES ---

	/**
	 * Invoke all local services via Thread pool (true) or directly (false)
	 */
	protected boolean asyncLocalInvocation;

	/**
	 * Default action invocation socketTimeout (seconds)
	 */
	protected int defaultTimeout;

	/**
	 * Timeout-checker's period delay (seconds)
	 */
	protected int cleanup = 1;

	/**
	 * Check protocol version
	 */
	protected boolean checkVersion;

	/**
	 * Reader lock of configuration
	 */
	protected final Lock readLock;

	/**
	 * Writer lock of configuration
	 */
	protected final Lock writeLock;

	// --- LOCAL NODE ID ---

	protected String nodeID;

	// --- COMPONENTS ---

	protected ServiceBroker broker;
	protected StrategyFactory strategy;
	protected ScheduledExecutorService scheduler;
	protected Transporter transporter;
	protected EventBus eventbus;

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
		super.start(broker, config);

		// Process config
		asyncLocalInvocation = config.get("asyncLocalInvocation", asyncLocalInvocation);
		cleanup = config.get("cleanup", cleanup);
		defaultTimeout = config.get("defaultTimeout", defaultTimeout);
		checkVersion = config.get("checkVersion", checkVersion);

		// Node-style Service Registry config?
		Tree parent = config.getParent();
		if (parent != null && (parent.get("strategy", (String) null) != null
				|| parent.get("preferLocal", (String) null) != null)) {
			logger.warn("Service Registry has no \"strategy\" or \"preferLocal\" properties.");
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
	public void stop() {

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

			// Delete cached node descriptor
			clearDescriptorCache();

			writeLock.unlock();
		}
	}

	// --- CALL TIMEOUT CHECKER TASK ---

	public void run() {
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
	protected final AtomicReference<ScheduledFuture<?>> timer = new AtomicReference<>();

	/**
	 * Next scheduled time to check timeouts
	 */
	protected final AtomicLong prevTimeoutAt = new AtomicLong();

	/**
	 * Recalculates the next socketTimeout checking time
	 */
	protected void reschedule(long minTimeoutAt) {
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

				// Next when not changed
				return;
			}

			// Stop previous timer
			ScheduledFuture<?> t = timer.get();
			if (t != null) {
				t.cancel(false);
			}

			// Schedule next socketTimeout timer
			long delay = Math.max(1000, minTimeoutAt - now);
			timer.set(scheduler.schedule(this, delay, TimeUnit.MILLISECONDS));
		}
	}

	// --- REGISTER PROMISE ---

	protected void register(String id, Promise promise, long timeoutAt) {
		promises.put(id, new PendingPromise(promise, timeoutAt));

		long nextTimeoutAt = prevTimeoutAt.get();
		if (nextTimeoutAt == 0 || (timeoutAt / 1000 * 1000) + 1000 < nextTimeoutAt) {
			scheduler.execute(() -> {
				reschedule(timeoutAt);
			});
		}
	}

	protected void deregister(String id) {
		promises.remove(id);
	}

	// --- RECEIVE REQUEST FROM REMOTE SERVICE ---

	public void receiveRequest(Tree message) {

		// Verify protocol version
		if (checkVersion) {
			String ver = message.get("ver", "unknown");
			if (!ServiceBroker.PROTOCOL_VERSION.equals(ver)) {
				logger.warn("Invalid protocol version (" + ver + ")!");
				return;
			}
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
		ActionEndpoint endpoint = strategy.getEndpoint(nodeID);
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
		String sender = message.get("sender", (String) null);
		if (sender == null || sender.isEmpty()) {
			logger.warn("Missing \"sender\" property!");
			return;
		}

		// Create CallingOptions
		int timeout = message.get("socketTimeout", 0);
		Tree params = message.get("params");

		// TODO Process other properties:
		//
		// Tree meta = message.get("meta");
		// int level = message.get("level", 1);
		// boolean metrics = message.get("metrics", false);
		// String parentID = message.get("parentID", (String) null);
		// String requestID = message.get("requestID", (String) null);

		CallingOptions.Options opts = CallingOptions.nodeID(nodeID).timeout(timeout);

		// Invoke action
		try {
			endpoint.call(params, opts, null).then(data -> {

				// Send response
				Tree response = new Tree();
				response.put("sender", nodeID);
				response.put("id", id);
				response.put("ver", ServiceBroker.PROTOCOL_VERSION);
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

	protected Tree throwableToTree(String id, Throwable error) {
		Tree response = new Tree();
		response.put("id", id);
		response.put("ver", ServiceBroker.PROTOCOL_VERSION);
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
	public void receiveResponse(Tree message) {

		// Verify protocol version
		if (checkVersion) {
			String ver = message.get("ver", "unknown");
			if (!ServiceBroker.PROTOCOL_VERSION.equals(ver)) {
				logger.warn("Invalid protocol version (" + ver + ")!");
				return;
			}
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
	public void addActions(Service service, Tree config) throws Exception {
			
		writeLock.lock();
		try {

			// Initialize actions in services
			Class<? extends Service> clazz = service.getClass();
			Field[] fields = clazz.getFields();
			for (Field field : fields) {

				// Register action
				if (Action.class.isAssignableFrom(field.getType())) {

					// Name of the action (eg. "v2.service.add")
					String actionFullName = nameOf(service.name, field);
					String actionShortName = actionFullName;
					int i = actionFullName.indexOf('.');
					if (i > -1) {
						actionShortName = actionShortName.substring(i + 1);
					}
					String actionNodePath = "actions." + actionShortName;
					Tree actionConfig = config.get(actionNodePath);
					if (actionConfig == null) {
						if (config.isMap()) {
							actionConfig = config.putMap(actionNodePath);
						} else {
							actionConfig = new Tree();
						}
					}
					actionConfig.put("name", actionFullName);

					// Process "Cache" annotation
					if (actionConfig.get("cache") == null) {
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
						actionConfig.put("cache", cached);
						if (ttl > 0) {
							actionConfig.put("ttl", ttl);
						}
						if (keys != null && keys.length > 0) {
							actionConfig.put("cacheKeys", String.join(",", keys));
						}
					}

					// Register actions
					field.setAccessible(true);
					Action action = (Action) field.get(service);
					LocalActionEndpoint endpoint = new LocalActionEndpoint(this, action, asyncLocalInvocation);
					endpoint.start(broker, actionConfig);
					Strategy<ActionEndpoint> actionStrategy = strategies.get(actionFullName);
					if (actionStrategy == null) {
						Tree strategyConfig = actionConfig.get("strategy");
						if (strategyConfig == null) {
							actionStrategy = strategy.create();
							strategyConfig = config.getRoot().get("strategy");
							if (strategyConfig == null) {
								strategyConfig = new Tree();
							}
						} else {
							// TODO invoke strategy factory
						}
						actionStrategy.start(broker, strategyConfig);
						strategies.put(actionFullName, actionStrategy);
					}
					actionStrategy.addEndpoint(endpoint);
				}
			}

			// Start service
			Tree settings = config.get("settings");
			if (settings == null) {
				if (config.isMap()) {
					settings = config.putMap("settings");
				} else {
					settings = new Tree();
				}
			}
			service.start(broker, settings);
			services.put(service.name, service);

			// Notify local listeners about the new LOCAL service
			broadcastServicesChanged(true);

		} finally {

			// Delete cached node descriptor
			clearDescriptorCache();

			writeLock.unlock();
		}
	}

	protected void broadcastServicesChanged(boolean local) {
		Tree message = new Tree();
		message.put("localService", true);
		eventbus.broadcast("$services.changed", message, null, true);
	}

	// --- ADD A REMOTE SERVICE ---

	@Override
	public void addActions(Tree config) throws Exception {
		Tree actions = config.get("actions");
		if (actions != null && actions.isMap()) {
			String nodeID = Objects.requireNonNull(config.get("nodeID", (String) null));
			writeLock.lock();
			try {
				for (Tree actionConfig : actions) {
					actionConfig.putObject("nodeID", nodeID, true);
					String actionName = actionConfig.get("name", "");

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

			// Notify local listeners about the new REMOTE service
			broadcastServicesChanged(false);
		}
	}

	// --- REMOVE ALL REMOTE SERVICES/ACTIONS OF A NODE ---

	@Override
	public void removeActions(String nodeID) {
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
			if (this.nodeID.equals(nodeID)) {

				// Stop local services
				try {
					stopAllLocalServices();
				} finally {

					// Delete cached node descriptor
					clearDescriptorCache();
				}

				// Notify local listeners (LOCAL services changed)
				broadcastServicesChanged(true);

			} else {

				// Notify local listeners (REMOTE services changed)
				broadcastServicesChanged(false);
			}
		} finally {
			writeLock.unlock();
		}
	}

	protected void stopAllLocalServices() {
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
	public Service getService(String name) {
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
	public ActionEndpoint getAction(String name, String nodeID) {
		Strategy<ActionEndpoint> strategy;
		readLock.lock();
		try {
			strategy = strategies.get(name);
		} finally {
			readLock.unlock();
		}
		if (strategy == null) {
			throw new NoSuchElementException("Unknown action name (" + name + ")!");
		}
		ActionEndpoint endpoint = strategy.getEndpoint(nodeID);
		if (endpoint == null) {
			throw new NoSuchElementException("Unknown nodeID (" + nodeID + ")!");
		}
		return endpoint;
	}

	// --- TIMESTAMP OF SERVICE DESCRIPTOR ---

	private AtomicLong timestamp = new AtomicLong();

	public long getTimestamp() {
		return timestamp.get();
	}

	// --- GENERATE SERVICE DESCRIPTOR ---

	private volatile Tree descriptor;

	@Override
	public Tree getDescriptor() {
		Tree current = currentDescriptor();
		return current.clone();
	}

	protected synchronized void clearDescriptorCache() {
		descriptor = null;
	}

	protected synchronized Tree currentDescriptor() {
		if (descriptor == null) {

			// Create new descriptor block
			descriptor = new Tree();

			// Services array
			Tree services = descriptor.putList("services");
			Tree servicesMap = new Tree();
			readLock.lock();
			try {
				for (Map.Entry<String, Strategy<ActionEndpoint>> entry : strategies.entrySet()) {

					// Split into parts ("math.add" -> "math" and "add")
					String name = entry.getKey();
					int i = name.lastIndexOf('.');
					String service = name.substring(0, i);

					// Get endpoint
					ActionEndpoint endpoint = entry.getValue().getEndpoint(nodeID);
					if (endpoint == null) {
						continue;
					}

					// Service block
					Tree serviceMap = servicesMap.putMap(service, true);
					serviceMap.put("name", service);

					// TODO Store settings block
					// serviceMap.putMap("settings");

					// Not used
					// serviceMap.putMap("metadata");

					// Node ID
					serviceMap.put("nodeID", nodeID);

					// Action block
					@SuppressWarnings("unchecked")
					Map<String, Object> actionBlock = (Map<String, Object>) serviceMap.putMap("actions", true)
							.asObject();
					LinkedHashMap<String, Object> map = new LinkedHashMap<>();
					actionBlock.put(name, map);
					Tree actionMap = new Tree(map);

					actionMap.put("name", name);
					boolean cached = endpoint.cached();
					actionMap.put("cache", cached);
					if (cached) {
						String[] keys = endpoint.cacheKeys();
						if (keys != null) {
							Tree cacheKeys = actionMap.putList("cacheKeys");
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
					// actionMap.putMap("params");

				}
			} finally {
				readLock.unlock();
			}
			for (Tree service : servicesMap) {
				services.addObject(service);
			}

			// Host name
			descriptor.put("hostname", getHostName());

			// IP array
			Tree ipList = descriptor.putList("ipList");
			HashSet<String> ips = new HashSet<>();
			try {
				InetAddress local = InetAddress.getLocalHost();
				String defaultAddress = local.getHostAddress();
				if (!defaultAddress.startsWith("127.")) {
					ips.add(defaultAddress);
					ipList.add(defaultAddress);
				}
			} catch (Exception ignored) {
			}
			try {
				Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
				while (e.hasMoreElements()) {
					NetworkInterface n = (NetworkInterface) e.nextElement();
					Enumeration<InetAddress> ee = n.getInetAddresses();
					while (ee.hasMoreElements()) {
						InetAddress i = (InetAddress) ee.nextElement();
						if (!i.isLoopbackAddress()) {
							String test = i.getHostAddress();
							if (ips.add(test)) {
								ipList.add(test);
							}
						}
					}
				}
			} catch (Exception ignored) {
			}

			// Client descriptor
			Tree client = descriptor.putMap("client");
			client.put("type", "java");
			client.put("version", ServiceBroker.SOFTWARE_VERSION);
			client.put("langVersion", System.getProperty("java.version", "1.8"));

			// Config (not used in this version)
			// root.putMap("config");

			// Set timestamp
			timestamp.set(System.currentTimeMillis());
		}
		return descriptor;
	}

	// --- GETTERS / SETTERS ---

	public boolean isAsyncLocalInvocation() {
		return asyncLocalInvocation;
	}

	public void setAsyncLocalInvocation(boolean asyncLocalInvocation) {
		this.asyncLocalInvocation = asyncLocalInvocation;
	}

	public int getDefaultTimeout() {
		return defaultTimeout;
	}

	public void setDefaultTimeout(int defaultTimeoutSeconds) {
		this.defaultTimeout = defaultTimeoutSeconds;
	}

	public int getCleanup() {
		return cleanup;
	}

	public void setCleanup(int cleanupSeconds) {
		this.cleanup = cleanupSeconds;
	}

	public boolean isCheckVersion() {
		return checkVersion;
	}

	public void setCheckVersion(boolean checkVersion) {
		this.checkVersion = checkVersion;
	}

}