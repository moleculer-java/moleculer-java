/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
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

import static services.moleculer.ServiceBroker.PROTOCOL_VERSION;
import static services.moleculer.transporter.Transporter.PACKET_PING;
import static services.moleculer.transporter.Transporter.PACKET_RESPONSE;
import static services.moleculer.util.CommonUtils.convertAnnotations;
import static services.moleculer.util.CommonUtils.getHostName;
import static services.moleculer.util.CommonUtils.nameOf;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.CallOptions;
import services.moleculer.context.Context;
import services.moleculer.context.ContextFactory;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.strategy.Strategy;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.transporter.Transporter;
import services.moleculer.uid.UidGenerator;
import services.moleculer.util.FastBuildTree;

/**
 * Default implementation of the Service Registry.
 */
@Name("Default Service Registry")
public class DefaultServiceRegistry extends ServiceRegistry {

	// --- REGISTERED MIDDLEWARES ---

	protected final LinkedHashSet<Middleware> middlewares = new LinkedHashSet<>(32);

	// --- REGISTERED LOCAL SERVICES ---

	protected final LinkedHashMap<String, Service> services = new LinkedHashMap<>(64);

	// --- REGISTERED STRATEGIES PER ACTIONS ---

	protected final HashMap<String, Strategy<ActionEndpoint>> strategies = new HashMap<>(256);

	// --- REGISTERED LOCAL AND REMOTE SERVICE NAMES ---

	protected final HashSet<String> names = new HashSet<>(64);

	// --- PENDING REMOTE INVOCATIONS ---

	protected final ConcurrentHashMap<String, PendingPromise> promises = new ConcurrentHashMap<>(1024);

	// --- PROPERTIES ---

	/**
	 * Invoke all local services via Thread pool (true) or directly (false)
	 */
	protected boolean asyncLocalInvocation;

	/**
	 * Check protocol version
	 */
	protected boolean checkVersion;

	/**
	 * Include error trace in response
	 */
	protected boolean sendErrorTrace = true;

	/**
	 * Write exceptions into the log file
	 */
	protected boolean writeErrorsToLog = true;

	// --- LOCKS ---

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

	protected ExecutorService executor;
	protected ScheduledExecutorService scheduler;
	protected StrategyFactory strategyFactory;
	protected ContextFactory contextFactory;
	protected Transporter transporter;
	protected Eventbus eventbus;
	protected UidGenerator uid;

	// --- VARIABLES OF THE TIMEOUT HANDLER ---

	/**
	 * Cancelable timer for handling timeouts of action calls
	 */
	protected final AtomicReference<ScheduledFuture<?>> callTimeoutTimer = new AtomicReference<>();

	/**
	 * Next scheduled time to check timeouts
	 */
	protected final AtomicLong prevTimeoutAt = new AtomicLong();

	// --- WAIT FOR SERVICE(S) ---

	/**
	 * Cancelable timer for handling "wait for service" calls
	 */
	protected ScheduledFuture<?> servicesOnlineTimer;

	/**
	 * Promises of the "waitingForServices" calls
	 */
	protected final LinkedList<ServiceListener> serviceListeners = new LinkedList<>();

	// --- TIMESTAMP OF SERVICE DESCRIPTOR ---

	/**
	 * Timestamp of the service descriptor of this Moleculer Node (~=
	 * "generated at" timestamp)
	 */
	private AtomicLong timestamp = new AtomicLong();

	// --- CACHED SERVICE DESCRIPTOR ---

	private volatile Tree cachedDescriptor;

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

	// --- INIT SERVICE REGISTRY ---

	/**
	 * Initializes ServiceRegistry instance.
	 *
	 * @param broker
	 *            parent ServiceBroker
	 */
	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Local nodeID
		this.nodeID = broker.getNodeID();

		// Set components
		ServiceBrokerConfig cfg = broker.getConfig();
		this.executor = cfg.getExecutor();
		this.scheduler = cfg.getScheduler();
		this.strategyFactory = cfg.getStrategyFactory();
		this.contextFactory = cfg.getContextFactory();
		this.transporter = cfg.getTransporter();
		this.eventbus = cfg.getEventbus();
		this.uid = cfg.getUidGenerator();
	}

	// --- STOP SERVICE REGISTRY ---

	@Override
	public void stopped() {

		// Stop timer
		ScheduledFuture<?> task = callTimeoutTimer.get();
		if (task != null) {
			task.cancel(false);
		}

		// Stop pending invocations
		InterruptedException error = new InterruptedException("Registry is shutting down.");
		for (PendingPromise pending : promises.values()) {
			pending.promise.complete(error);
		}

		// Stop all services
		writeLock.lock();
		try {

			// Stop registered services
			stopAllLocalServices();

			// Delete strategies (and registered actions)
			strategies.clear();

			// Delete all service names
			names.clear();

			// Stop middlewares
			for (Middleware middleware : middlewares) {
				try {
					middleware.stopped();
				} catch (Throwable cause) {
					logger.warn("Unable to stop middleware!", cause);
				}
			}
			middlewares.clear();

			// Delete cached node descriptor
			clearDescriptorCache();

		} finally {
			writeLock.unlock();
		}
	}

	// --- CALL TIMEOUT CHECKER TASK ---

	protected void checkTimeouts() {
		long now = System.currentTimeMillis();
		PendingPromise pending;
		Iterator<PendingPromise> i = promises.values().iterator();
		boolean removed = false;
		while (i.hasNext()) {
			pending = i.next();
			if (pending.timeoutAt > 0 && now >= pending.timeoutAt) {
				pending.promise.complete(new TimeoutException("Action invocation timeouted!"));
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

	// --- CALL TIMEOUT HANDLING ---

	/**
	 * Recalculates the next timeout checking time
	 * 
	 * @param minTimeoutAt
	 *            next / closest timestamp
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
			ScheduledFuture<?> t = callTimeoutTimer.get();
			if (t != null) {
				if (prevTimeoutAt.get() > now) {
					t.cancel(false);
					prevTimeoutAt.set(0);
				} else {
					callTimeoutTimer.set(null);
					prevTimeoutAt.set(0);
				}
			}
		} else {
			minTimeoutAt = (minTimeoutAt / 100 * 100) + 100;
			long prev = prevTimeoutAt.getAndSet(minTimeoutAt);
			if (prev == minTimeoutAt) {

				// Next when not changed
				return;
			}

			// Stop previous timer
			ScheduledFuture<?> t = callTimeoutTimer.get();
			if (t != null) {
				t.cancel(false);
			}

			// Schedule next timeout timer
			long delay = Math.max(10, minTimeoutAt - now);
			callTimeoutTimer.set(scheduler.schedule(this::checkTimeouts, delay, TimeUnit.MILLISECONDS));
		}
	}

	// --- REGISTER PROMISE ---

	protected void register(String id, Promise promise, long timeoutAt) {
		promises.put(id, new PendingPromise(promise, timeoutAt));

		long nextTimeoutAt = prevTimeoutAt.get();
		if (nextTimeoutAt == 0 || (timeoutAt / 100 * 100) + 100 < nextTimeoutAt || promises.size() < 3) {
			scheduler.execute(() -> {
				reschedule(timeoutAt);
			});
		}
	}

	protected void deregister(String id) {
		promises.remove(id);
	}

	// --- RECEIVE REQUEST FROM REMOTE SERVICE ---

	@Override
	public void receiveRequest(Tree message) {

		// Verify protocol version
		if (checkVersion) {
			String ver = message.get("ver", "unknown");
			if (!PROTOCOL_VERSION.equals(ver)) {
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

		// Process params and meta
		Tree params = message.get("params");
		if (params == null) {
			params = new Tree();
		}
		Tree meta = message.get("meta");
		if (meta != null && !meta.isEmpty()) {
			params.getMeta().setObject(params);
		}

		// Get timeout
		int timeout = message.get("timeout", 0);
		CallOptions.Options opts;
		if (timeout > 0) {
			opts = CallOptions.timeout(timeout);
		} else {
			opts = null;
		}

		// Get other properties
		int level = message.get("level", 1);
		String parentID = message.get("parentID", (String) null);
		String requestID = message.get("requestID", id);

		// Create context
		Context ctx = contextFactory.create(action, params, opts, id, level, requestID, parentID);

		// Invoke action
		try {
			new Promise(endpoint.handler(ctx)).then(data -> {

				// Send response
				FastBuildTree msg = new FastBuildTree(6);
				msg.putUnsafe("sender", nodeID);
				msg.putUnsafe("id", id);
				msg.putUnsafe("ver", PROTOCOL_VERSION);
				msg.putUnsafe("success", true);
				msg.putUnsafe("data", data);
				Tree rspMeta = data.getMeta(false);
				if (rspMeta != null && !rspMeta.isEmpty()) {
					msg.putUnsafe("meta", rspMeta);
				}
				transporter.publish(PACKET_RESPONSE, sender, msg);

			}).catchError(error -> {

				// Send error
				transporter.publish(PACKET_RESPONSE, sender, throwableToTree(id, error));

				// Write error to log file
				if (writeErrorsToLog) {
					logger.error("Unexpected error occurred while invoking \"" + action + "\" action!", error);
				}

			});
		} catch (Throwable error) {

			// Send error
			transporter.publish(PACKET_RESPONSE, sender, throwableToTree(id, error));

			// Write error to log file
			if (writeErrorsToLog) {
				logger.error("Unexpected error occurred while invoking \"" + action + "\" action!", error);
			}

		}

	}

	// --- CONVERT THROWABLE TO RESPONSE MESSAGE ---

	protected Tree throwableToTree(String id, Throwable error) {
		FastBuildTree msg = new FastBuildTree(5);
		try {
			msg.putUnsafe("id", id);
			msg.putUnsafe("ver", PROTOCOL_VERSION);
			msg.putUnsafe("sender", nodeID);
			msg.putUnsafe("success", false);
			if (error != null) {

				// Add message
				FastBuildTree errorMap = new FastBuildTree(4);
				msg.putUnsafe("error", errorMap);
				String message = String.valueOf(error.getMessage());
				message = message.replace('\r', ' ').replace('\n', ' ');
				errorMap.putUnsafe("message", message.trim());

				// Add trace
				if (sendErrorTrace) {
					StringWriter sw = new StringWriter(128);
					PrintWriter pw = new PrintWriter(sw);
					error.printStackTrace(pw);
					errorMap.putUnsafe("stack", sw.toString());
				}

				// Add source nodeID of the error
				errorMap.putUnsafe("nodeID", nodeID);

				// Add default error code
				errorMap.putUnsafe("code", 500);
			}
		} catch (Throwable cause) {
			logger.error("Unexpected error occurred!", cause);
		}
		return msg;
	}

	// --- RECEIVE PING-PONG RESPONSE ---

	@Override
	public void receivePong(Tree message) {

		// Verify protocol version
		if (checkVersion) {
			String ver = message.get("ver", "unknown");
			if (!PROTOCOL_VERSION.equals(ver)) {
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

		// Resolve Promise
		pending.promise.complete(message);
	}

	// --- RECEIVE RESPONSE FROM REMOTE SERVICE ---

	@Override
	public void receiveResponse(Tree message) {

		// Verify protocol version
		if (checkVersion) {
			String ver = message.get("ver", "unknown");
			if (!PROTOCOL_VERSION.equals(ver)) {
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
					trace = error.get("stack", (String) null);
					if (trace != null && !trace.isEmpty()) {
						logger.error("Remote invaction failed!\r\n" + trace);
					}
				}
				if (errorMessage == null || errorMessage.isEmpty()) {
					errorMessage = "Unknow error!";
				}
				if (trace == null || trace.isEmpty()) {
					logger.error("Remote invoction failed (unknown error occurred)!");
				}
				pending.promise.complete(new RemoteException(errorMessage));
				return;
			}
		} catch (Throwable cause) {
			logger.error("Unable to pass on incoming response!", cause);
		}
	}

	// --- ADD MIDDLEWARES ---

	@Override
	public void use(Collection<Middleware> middlewares) {
		LinkedList<Middleware> newMiddlewares = new LinkedList<>();
		writeLock.lock();
		try {

			// Register middlewares
			for (Middleware middleware : middlewares) {
				if (this.middlewares.add(middleware)) {
					newMiddlewares.add(middleware);
				}
			}
			if (!newMiddlewares.isEmpty()) {

				// Start new middlewares
				for (Middleware middleware : newMiddlewares) {
					try {
						middleware.started(broker);
					} catch (Exception cause) {
						throw new RuntimeException("Unable to start middleware!", cause);
					}
				}

				// Apply new middlewares
				for (Strategy<ActionEndpoint> strategy : strategies.values()) {
					List<ActionEndpoint> endpoints = strategy.getAllEndpoints();
					for (ActionEndpoint endpoint : endpoints) {
						for (Middleware middleware : newMiddlewares) {
							endpoint.use(middleware);
						}
					}
				}

			}
		} finally {
			writeLock.unlock();
		}
	}

	// --- ADD A LOCAL SERVICE ---

	@Override
	public void addActions(String serviceName, Service service) {
		if (serviceName == null || serviceName.isEmpty()) {
			serviceName = service.getName();
		} else {
			service.redefineName(serviceName);
		}
		final String name = serviceName.replace(' ', '-');
		Class<? extends Service> clazz = service.getClass();
		Dependencies dependencies = clazz.getAnnotation(Dependencies.class);
		if (dependencies != null) {
			String[] services = dependencies.value();
			if (services != null && services.length > 0) {
				waitForServices(0, Arrays.asList(services)).then(ok -> {
					StringBuilder msg = new StringBuilder(64);
					msg.append("Starting \"");
					msg.append(name);
					msg.append("\" service because ");
					for (int i = 0; i < services.length; i++) {
						msg.append('\"');
						msg.append(services[i]);
						msg.append('\"');
						if (i < services.length - 1) {
							msg.append(", ");
						}
					}
					if (services.length == 1) {
						msg.append(" service is");
					} else {
						msg.append(" services are");
					}
					msg.append(" available...");
					logger.info(msg.toString());
					addOnlineActions(name, service);
				}).catchError(cause -> {
					logger.error("Unable to deploy service!", cause);
				});
				return;
			}
		}
		addOnlineActions(name, service);
	}

	protected void addOnlineActions(String serviceName, Service service) {
		Class<? extends Service> clazz = service.getClass();
		Field[] fields = clazz.getFields();
		int actionCounter = 0;

		writeLock.lock();
		try {

			// Initialize actions in service
			for (Field field : fields) {
				if (!Action.class.isAssignableFrom(field.getType())) {
					continue;
				}
				field.setAccessible(true);
				Action action = (Action) field.get(service);

				// Name of the action (eg. "service.action")
				String actionName = nameOf(serviceName, field);

				Tree actionConfig = new Tree();
				actionConfig.put("name", actionName);

				Annotation[] annotations = field.getAnnotations();
				convertAnnotations(actionConfig, annotations);

				// Register action
				LocalActionEndpoint endpoint = new LocalActionEndpoint(this, executor, nodeID, actionConfig, action);
				Strategy<ActionEndpoint> actionStrategy = strategies.get(actionName);
				if (actionStrategy == null) {

					// Create strategy
					actionStrategy = strategyFactory.create();
					strategies.put(actionName, actionStrategy);
				}
				actionStrategy.addEndpoint(endpoint);

				// Apply middlewares
				for (Middleware middleware : middlewares) {
					endpoint.use(middleware);
				}

				// Write log about this action
				logger.info("Action \"" + actionName + "\" registered.");
				actionCounter++;
			}
			services.put(serviceName, service);
			names.add(serviceName);
			service.started(broker);

			// Delete cached node descriptor
			clearDescriptorCache();

		} catch (Exception cause) {
			logger.error("Unable to register local service!", cause);
			return;
		} finally {
			writeLock.unlock();
		}

		// Notify local listeners about the new LOCAL service
		broadcastServicesChanged(true);

		// Notify other nodes
		if (transporter != null) {
			transporter.broadcastInfoPacket();
		}

		// Write log about this service
		StringBuilder msg = new StringBuilder(64);
		msg.append("Service \"");
		msg.append(serviceName);
		msg.append("\" started ");
		if (actionCounter == 0) {
			msg.append("without any actions.");
		} else if (actionCounter == 1) {
			msg.append("with 1 action.");
		} else {
			msg.append("with ");
			msg.append(actionCounter);
			msg.append(" actions.");
		}
		logger.info(msg.toString());
	}

	// --- NOTIFY OTHER SERVICES ---

	protected void broadcastServicesChanged(boolean local) {
		Tree message = new Tree();
		message.put("localService", true);
		eventbus.broadcast("$services.changed", message, null, true);
	}

	// --- ADD A REMOTE SERVICE ---

	@Override
	public void addActions(Tree config) {
		Tree actions = config.get("actions");
		String serviceName = config.get("name", "");
		writeLock.lock();
		try {
			if (actions != null && actions.isMap()) {
				String nodeID;
				if (config.getParent().isEnumeration()) {
					nodeID = config.getRoot().get("sender", (String) null);
				} else {
					nodeID = config.getName();
				}
				for (Tree actionConfig : actions) {
					actionConfig.putObject("nodeID", nodeID, true);
					String actionName = actionConfig.get("name", "");

					// Register remote action
					RemoteActionEndpoint endpoint = new RemoteActionEndpoint(this, transporter, nodeID, actionConfig);
					Strategy<ActionEndpoint> actionStrategy = strategies.get(actionName);
					if (actionStrategy == null) {
						actionStrategy = strategyFactory.create();
						strategies.put(actionName, actionStrategy);
					}
					actionStrategy.addEndpoint(endpoint);
				}
			}
			names.add(serviceName);
		} finally {
			writeLock.unlock();
		}

		// Notify local listeners about the new REMOTE service
		broadcastServicesChanged(false);
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
					endpoints.remove();
				}
			}
			if (this.nodeID.equals(nodeID)) {

				// Stop local services
				writeLock.lock();
				try {
					stopAllLocalServices();

					// Delete cached node descriptor
					clearDescriptorCache();

				} finally {
					writeLock.unlock();
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
		for (Map.Entry<String, Service> serviceEntry : services.entrySet()) {
			String name = serviceEntry.getKey();
			try {
				serviceEntry.getValue().stopped();
				logger.info("Service \"" + name + "\" stopped.");
			} catch (Throwable cause) {
				logger.warn("Unable to stop \"" + name + "\" service!", cause);
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

	// --- GET LOCAL OR REMOTE ACTION ---

	@Override
	public Action getAction(String name, String nodeID) {
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

	// --- WAIT FOR SERVICE(S) ---

	@Override
	public Promise waitForServices(long timeoutMillis, Collection<String> services) {
		if (services == null || services.isEmpty() || isServicesOnline(services)) {
			return Promise.resolve();
		}
		Promise promise = new Promise();
		long timeoutAt;
		if (timeoutMillis > 0) {
			timeoutAt = System.currentTimeMillis() + timeoutMillis;
		} else {
			timeoutAt = 0;
		}
		ServiceListener listener = new ServiceListener(promise, timeoutAt, services);
		synchronized (serviceListeners) {
			serviceListeners.addLast(listener);
			if (servicesOnlineTimer == null) {
				servicesOnlineTimer = scheduler.scheduleWithFixedDelay(this::checkServicesOnline, 1, 1,
						TimeUnit.SECONDS);
			}
		}
		return promise;
	}

	protected void checkServicesOnline() {
		LinkedList<ServiceListener> onlineListeners = new LinkedList<>();
		LinkedList<ServiceListener> timeoutedListeners = new LinkedList<>();
		long now = System.currentTimeMillis();
		synchronized (serviceListeners) {
			Iterator<ServiceListener> i = serviceListeners.iterator();
			while (i.hasNext()) {
				ServiceListener listener = i.next();

				// Online?
				boolean online = isServicesOnline(listener.services);
				if (online) {
					onlineListeners.addLast(listener);
					i.remove();
					continue;
				}

				// Timeouted?
				if (listener.timeoutAt > 0 && listener.timeoutAt <= now) {
					timeoutedListeners.addLast(listener);
					i.remove();
				}
			}
			if (serviceListeners.isEmpty() && servicesOnlineTimer != null) {
				servicesOnlineTimer.cancel(false);
				servicesOnlineTimer = null;
			}
		}
		if (!timeoutedListeners.isEmpty()) {
			for (ServiceListener listener : timeoutedListeners) {
				try {
					String missingService = null;
					for (String service : listener.services) {
						if (!isServicesOnline(Collections.singleton(service))) {
							missingService = service;
							break;
						}
					}
					if (missingService == null) {
						missingService = listener.services.isEmpty() ? "unknown" : listener.services.iterator().next();
					}
					listener.promise.complete(new NoSuchElementException("Missing service (" + missingService + ")!"));
				} catch (Exception ignored) {
				}
			}
		}
		for (ServiceListener listener : onlineListeners) {
			try {
				listener.promise.complete();
			} catch (Exception ignored) {
			}
		}
	}

	protected boolean isServicesOnline(Collection<String> requiredServices) {
		int foundCounter = 0;
		readLock.lock();
		try {
			for (String service : requiredServices) {
				if (names.contains(service)) {
					foundCounter++;
					continue;
				}
				if (foundCounter == 0) {
					break;
				}
			}
		} finally {
			readLock.unlock();
		}
		return foundCounter == requiredServices.size();
	}

	// --- PING / PONG HANDLING ---

	@Override
	public Promise ping(long timeoutMillis, String nodeID) {

		// Local node?
		if (this.nodeID.equals(nodeID)) {
			Tree rsp = new Tree();
			long time = System.currentTimeMillis();
			rsp.put("time", time);
			rsp.put("arrived", time);
			return Promise.resolve(rsp);
		}

		// Do we have a transporter?
		if (transporter == null) {
			return Promise.reject(new IllegalArgumentException("Unknown nodeID (" + nodeID + ")!"));
		}

		// Create new promise
		Promise promise = new Promise();

		// Set timeout
		long timeoutAt;
		if (timeoutMillis > 0) {
			timeoutAt = System.currentTimeMillis() + timeoutMillis;
		} else {
			timeoutAt = 0;
		}

		// Register promise (timeout and response handling)
		String id = uid.nextUID();
		register(id, promise, timeoutAt);

		// Send request via transporter
		Tree message = transporter.createPingPacket(id);
		transporter.publish(PACKET_PING, nodeID, message);

		// Return promise
		return promise;
	}

	// --- TIMESTAMP OF SERVICE DESCRIPTOR ---

	@Override
	public long getTimestamp() {
		return timestamp.get();
	}

	// --- GENERATE SERVICE DESCRIPTOR ---

	@Override
	public Tree getDescriptor() {
		return currentDescriptor().clone();
	}

	protected void clearDescriptorCache() {
		cachedDescriptor = null;
		timestamp.set(System.currentTimeMillis());
	}

	protected Tree currentDescriptor() {
		Tree descriptor;
		readLock.lock();
		try {
			descriptor = cachedDescriptor;
			if (descriptor == null) {

				// Create new descriptor block
				descriptor = new Tree();

				// Services array
				Tree services = descriptor.putList("services");
				Tree servicesMap = new Tree();
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

					// Action block
					@SuppressWarnings("unchecked")
					Map<String, Object> actionBlock = (Map<String, Object>) serviceMap.putMap("actions", true)
							.asObject();
					actionBlock.put(name, endpoint.getConfig().asObject());

					// Listener block
					Tree listeners = eventbus.generateListenerDescriptor(service);
					if (listeners != null && !listeners.isEmpty()) {
						serviceMap.putObject("events", listeners);
					}
				}

				// Add services (without actions)
				for (String service : names) {
					if (servicesMap.get(service) == null) {

						// Service block
						Tree serviceMap = servicesMap.putMap(service, true);
						serviceMap.put("name", service);

						// Listener block
						Tree listeners = eventbus.generateListenerDescriptor(service);
						if (listeners != null && !listeners.isEmpty()) {
							serviceMap.putObject("events", listeners);
						}
					}
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
				cachedDescriptor = descriptor;
			}			
		} finally {
			readLock.unlock();
		}
		return descriptor;
	}

	// --- GETTERS / SETTERS ---

	public boolean isCheckVersion() {
		return checkVersion;
	}

	public void setCheckVersion(boolean checkVersion) {
		this.checkVersion = checkVersion;
	}

	public boolean isAsyncLocalInvocation() {
		return asyncLocalInvocation;
	}

	public void setAsyncLocalInvocation(boolean asyncLocalInvocation) {
		this.asyncLocalInvocation = asyncLocalInvocation;
	}

	public boolean isSendErrorTrace() {
		return sendErrorTrace;
	}

	public void setSendErrorTrace(boolean sendErrorTrace) {
		this.sendErrorTrace = sendErrorTrace;
	}

	public boolean isWriteErrorsToLog() {
		return writeErrorsToLog;
	}

	public void setWriteErrorsToLog(boolean writeErrorsToLog) {
		this.writeErrorsToLog = writeErrorsToLog;
	}

}