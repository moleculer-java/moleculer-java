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
package services.moleculer.eventbus;

import static services.moleculer.util.CommonUtils.nameOf;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import io.datatree.Tree;
import io.datatree.dom.Cache;
import io.datatree.dom.Config;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.Context;
import services.moleculer.error.ListenerNotAvailableError;
import services.moleculer.error.MaxCallLevelError;
import services.moleculer.error.RequestTimeoutError;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.service.ServiceInvoker;
import services.moleculer.strategy.Strategy;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.stream.IncomingStream;
import services.moleculer.stream.PacketStream;
import services.moleculer.transporter.Transporter;
import services.moleculer.uid.UidGenerator;
import services.moleculer.util.CheckedTree;

/**
 * Default EventBus implementation.
 */
@Name("Default Event Bus")
public class DefaultEventbus extends Eventbus {

	// --- REGISTERED EVENT LISTENERS ---

	protected final HashMap<String, HashMap<String, Strategy<ListenerEndpoint>>> listeners = new HashMap<>(256);

	// --- CACHES ---

	protected final Cache<String, Strategy<ListenerEndpoint>[]> emitterCache = new Cache<>(Config.CACHE_SIZE);
	protected final Cache<String, ListenerEndpoint[]> broadcasterCache = new Cache<>(Config.CACHE_SIZE);
	protected final Cache<String, ListenerEndpoint[]> localBroadcasterCache = new Cache<>(Config.CACHE_SIZE);

	// --- PROPERTIES ---

	/**
	 * Invoke all local listeners via Thread pool (true) or directly (false)
	 */
	protected boolean asyncLocalInvocation;

	/**
	 * Check protocol version
	 */
	protected boolean checkVersion;

	/**
	 * ServiceBroker's protocol version
	 */
	protected String protocolVersion = "4";

	/**
	 * Local Node ID
	 */
	protected String nodeID;

	/**
	 * Write exceptions into the log file
	 */
	protected boolean writeErrorsToLog = true;

	/**
	 * Stream inactivity/read timeout in MILLISECONDS (0 = no timeout). It may
	 * be useful if you want to remove the wrong packages from the memory.
	 */
	protected long streamTimeout;

	/**
	 * Max call level (for nested events)
	 */
	protected int maxCallLevel = 100;

	// --- REGISTRY LOCKS ---

	/**
	 * Reader lock of the Event Bus
	 */
	protected final Lock registryReadLock;

	/**
	 * Writer lock of the Event Bus
	 */
	protected final Lock registryWriteLock;

	// --- TIMERS ---
	
	/**
	 * Cancelable timer for request streams
	 */
	protected ScheduledFuture<?> streamTimeoutTimer;

	// --- COMPONENTS ---

	protected StrategyFactory strategy;
	protected Transporter transporter;
	protected ExecutorService executor;
	protected ServiceInvoker serviceInvoker;
	protected ScheduledExecutorService scheduler;
	protected UidGenerator uidGenerator;

	// --- REGISTERED STREAMS ---

	protected final HashMap<String, IncomingStream> requestStreams = new HashMap<>(1024);

	// --- STREAM LOCKS ---

	protected final ReadLock requestStreamReadLock;
	protected final WriteLock requestStreamWriteLock;

	// --- CONSTRUCTORS ---

	public DefaultEventbus() {
		this(false);
	}

	public DefaultEventbus(boolean asyncLocalInvocation) {

		// Async or direct local invocation
		this.asyncLocalInvocation = asyncLocalInvocation;

		// Init locks
		ReentrantReadWriteLock registryLock = new ReentrantReadWriteLock(true);
		registryReadLock = registryLock.readLock();
		registryWriteLock = registryLock.writeLock();

		ReentrantReadWriteLock requestStreamLock = new ReentrantReadWriteLock(false);
		requestStreamReadLock = requestStreamLock.readLock();
		requestStreamWriteLock = requestStreamLock.writeLock();
	}

	// --- START EVENT BUS ---

	/**
	 * Initializes default EventBus instance.
	 *
	 * @param broker
	 *            parent ServiceBroker
	 */
	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Set nodeID
		this.nodeID = broker.getNodeID();

		// Set the protocol version
		this.protocolVersion = broker.getProtocolVersion();
		
		// Set components
		ServiceBrokerConfig cfg = broker.getConfig();
		this.strategy = cfg.getStrategyFactory();
		this.transporter = cfg.getTransporter();
		this.executor = cfg.getExecutor();
		this.serviceInvoker = cfg.getServiceInvoker();
		this.scheduler = cfg.getScheduler();
		this.uidGenerator = cfg.getUidGenerator();

		// Start timer
		if (streamTimeout > 0) {
			long period = Math.max(5000, streamTimeout / 3);
			streamTimeoutTimer = scheduler.scheduleWithFixedDelay(this::checkTimeouts, period, period,
					TimeUnit.MILLISECONDS);
		}
	}

	// --- STOP EVENT BUS ---

	@Override
	public void stopped() {

		// Stop timer
		if (streamTimeoutTimer != null) {
			streamTimeoutTimer.cancel(false);
			streamTimeoutTimer = null;
		}

		// Clear endpoints
		registryWriteLock.lock();
		try {
			listeners.clear();
		} finally {

			// Clear caches
			emitterCache.clear();
			broadcasterCache.clear();
			localBroadcasterCache.clear();

			registryWriteLock.unlock();
		}
	}

	// --- CALL TIMEOUT CHECKER TASK ---

	protected void checkTimeouts() {
		long now = System.currentTimeMillis();

		// Check timeouted request streams
		IncomingStream stream;
		long timeoutAt;
		requestStreamWriteLock.lock();
		try {
			Iterator<IncomingStream> j = requestStreams.values().iterator();
			while (j.hasNext()) {
				stream = j.next();
				timeoutAt = stream.getTimeoutAt();
				if (timeoutAt > 0 && now >= timeoutAt) {
					stream.error(new RequestTimeoutError(this.nodeID, "unknown"));
					j.remove();
				}
			}
		} finally {
			requestStreamWriteLock.unlock();
		}
	}

	// --- RECEIVE EVENT FROM REMOTE SERVICE ---

	@Override
	public void receiveEvent(Tree message) {

		// Verify protocol version
		if (checkVersion) {
			String ver = message.get("ver", "unknown");
			if (!protocolVersion.equals(ver)) {
				logger.warn("Invalid protocol version (" + ver + ")!");
				return;
			}
		}

		// Get request's unique ID
		String id = message.get("id", "0");

		// Get sender's nodeID
		String sender = message.get("sender", (String) null);
		if (sender == null || sender.isEmpty()) {
			logger.warn("Missing \"sender\" property!");
			return;
		}

		// Incoming stream handling
		IncomingStream requestStream;
		if (id == null || "0".equals(id)) {
			requestStream = null;
		} else {
			requestStreamReadLock.lock();
			try {
				requestStream = requestStreams.get(id);
			} finally {
				requestStreamReadLock.unlock();
			}
			if (requestStream != null) {
				boolean remove = false;
				try {
					if (requestStream.receive(message)) {
						remove = true;
					}
				} catch (Throwable error) {
					remove = true;

					// Write error to log file
					if (writeErrorsToLog) {
						logger.error("Unexpected error occurred while streaming!", error);
					}

				}
				if (remove) {
					requestStreamWriteLock.lock();
					try {
						requestStreams.remove(id);
					} finally {
						requestStreamWriteLock.unlock();
					}
				}
			} else if (message.get("stream", false) || message.get("seq", 0) > 0) {
				requestStreamWriteLock.lock();
				try {
					requestStream = requestStreams.get(id);
					if (requestStream == null) {
						requestStream = new IncomingStream(nodeID, scheduler, streamTimeout);
						requestStreams.put(id, requestStream);
					}
				} finally {
					requestStreamWriteLock.unlock();
				}
				if (requestStream.receive(message)) {
					requestStreamWriteLock.lock();
					try {
						requestStreams.remove(id);
					} finally {
						requestStreamWriteLock.unlock();
					}
				}
			}
		}

		// Get event property
		String name = message.get("event", (String) null);
		if (name == null || name.isEmpty()) {
			if (requestStream == null) {
				logger.warn("Missing \"event\" property!");
			}
			return;
		}
		if (requestStream != null && requestStream.inited()) {

			// Event method invoked (do not invoke twice)
			return;
		}

		// Get data
		Tree data = message.get("data");
		Tree meta = message.get("meta");
		if (meta != null && !meta.isEmpty()) {
			if (data == null || data.isNull()) {
				data = new CheckedTree(null, meta.asObject());
			} else {
				data = new CheckedTree(data.asObject(), meta.asObject());
			}
		}

		// Process groups
		Tree groupArray = message.get("groups");
		Groups groups = null;
		if (groupArray != null) {
			int size = groupArray.size();
			if (size > 0) {
				String[] array = new String[groupArray.size()];
				int i = 0;
				for (Tree group : groupArray) {
					array[i++] = group.asString();
				}
				groups = Groups.of(array);
			}
		}

		// Get other properties
		int level = message.get("level", 1);
		String parentID = message.get("parentID", (String) null);
		String requestID = message.get("requestID", id);

		// Create Context
		PacketStream stream = requestStream == null ? null : requestStream.getPacketStream();
		Context ctx = new Context(serviceInvoker, this, uidGenerator, id, name, data, level, parentID, requestID,
				stream, null, sender);

		// Emit or broadcast?
		if (message.get("broadcast", true)) {

			// Broadcast
			broadcast(ctx, groups, true);

		} else {

			// Emit
			emit(ctx, groups, true);
		}
	}

	// --- ADD LOCAL LISTENER ---

	@Override
	public void addListeners(String serviceName, Service service) {

		// Service name with version
		String name = (serviceName == null || serviceName.isEmpty()) ? service.getName() : serviceName;
		Class<? extends Service> clazz = service.getClass();
		LinkedHashMap<String, Field> fields = new LinkedHashMap<>(64);
		for (Field f : clazz.getDeclaredFields()) {
			f.setAccessible(true);
			fields.putIfAbsent(f.getName(), f);
		}
		for (Field f : clazz.getFields()) {
			f.setAccessible(true);
			fields.putIfAbsent(f.getName(), f);
		}

		boolean hasListener = false;
		registryWriteLock.lock();
		try {

			// Initialize listeners in service
			for (Field field : fields.values()) {
				if (!Listener.class.isAssignableFrom(field.getType())) {
					continue;
				}

				// Register event listener
				hasListener = true;

				// Name of the action (eg. "service.action")
				String listenerName = nameOf(name, field);

				// Process "Subscribe" annotation
				Subscribe s = field.getAnnotation(Subscribe.class);
				String subscribe = null;
				if (s != null) {
					subscribe = s.value();
				}
				if (subscribe == null || subscribe.isEmpty()) {
					subscribe = listenerName;
				}

				// Process "Group" annotation
				String group = null;
				Group g = field.getAnnotation(Group.class);
				if (g != null) {
					group = g.value();
				}
				if (group == null || group.isEmpty()) {
					group = name;
				}

				// Register listener in EventBus
				field.setAccessible(true);
				Listener listener = (Listener) field.get(service);

				// Private (hidden) listener?
				boolean privateAccess = Modifier.isPrivate(field.getModifiers());

				// Get or create group map
				HashMap<String, Strategy<ListenerEndpoint>> groups = listeners.get(subscribe);
				if (groups == null) {
					groups = new HashMap<String, Strategy<ListenerEndpoint>>();
					listeners.put(subscribe, groups);
				}

				// Get or create strategy
				Strategy<ListenerEndpoint> strategy = groups.get(group);
				if (strategy == null) {
					strategy = this.strategy.create();
					groups.put(group, strategy);
				}

				// Add endpoint to strategy
				strategy.addEndpoint(new LocalListenerEndpoint(executor, nodeID, name, group, subscribe, listener,
						asyncLocalInvocation, privateAccess));
			}
		} catch (Exception cause) {
			logger.error("Unable to register local listener!", cause);
		} finally {

			// Clear caches
			if (hasListener) {
				emitterCache.clear();
				broadcasterCache.clear();
				localBroadcasterCache.clear();
			}

			// Unlock reader threads
			registryWriteLock.unlock();
		}
	}

	// --- ADD REMOTE LISTENER ---

	@Override
	public void addListeners(String nodeID, Tree config) {
		Tree events = config.get("events");
		if (events != null && events.isMap()) {
			String serviceName = Objects.requireNonNull(config.get("name", (String) null));
			registryWriteLock.lock();
			try {
				for (Tree listenerConfig : events) {
					String subscribe = listenerConfig.get("name", "");
					String group = listenerConfig.get("group", serviceName);

					// Register remote listener
					RemoteListenerEndpoint endpoint = new RemoteListenerEndpoint(transporter, nodeID, serviceName,
							group, subscribe);

					// Get or create group map
					HashMap<String, Strategy<ListenerEndpoint>> groups = listeners.get(subscribe);
					if (groups == null) {
						groups = new HashMap<String, Strategy<ListenerEndpoint>>();
						listeners.put(subscribe, groups);
					}

					// Get or create strategy
					Strategy<ListenerEndpoint> listenerStrategy = groups.get(group);
					if (listenerStrategy == null) {
						listenerStrategy = strategy.create();
						groups.put(group, listenerStrategy);
					}
					listenerStrategy.addEndpoint(endpoint);
				}
			} finally {

				// Clear caches
				emitterCache.clear();
				broadcasterCache.clear();
				localBroadcasterCache.clear();

				// Unlock reader threads
				registryWriteLock.unlock();
			}
		}
	}

	// --- REMOVE ALL REMOTE SERVICES/ACTIONS OF A NODE ---

	@Override
	public void removeListeners(String nodeID) {
		boolean found = false;
		registryWriteLock.lock();
		try {
			Iterator<HashMap<String, Strategy<ListenerEndpoint>>> groupIterator = listeners.values().iterator();
			while (groupIterator.hasNext()) {
				HashMap<String, Strategy<ListenerEndpoint>> groups = groupIterator.next();
				Iterator<Strategy<ListenerEndpoint>> strategyIterator = groups.values().iterator();
				while (strategyIterator.hasNext()) {
					Strategy<ListenerEndpoint> strategy = strategyIterator.next();
					if (strategy.remove(nodeID)) {
						found = true;
						if (strategy.isEmpty()) {
							strategyIterator.remove();
						}
					}
				}
				if (groups.isEmpty()) {
					groupIterator.remove();
				}
			}
		} finally {

			// Clear caches
			if (found) {
				emitterCache.clear();
				broadcasterCache.clear();
				localBroadcasterCache.clear();
			}

			registryWriteLock.unlock();
		}
	}

	// --- SEND EVENT TO ONE LISTENER IN THE SPECIFIED GROUP ---

	@Override
	@SuppressWarnings("unchecked")
	public void emit(Context ctx, Groups groups, boolean local) {

		// Verify call level
		if (maxCallLevel > 0 && ctx.level >= maxCallLevel) {
			MaxCallLevelError error = new MaxCallLevelError(nodeID, maxCallLevel);
			if (ctx.stream != null) {
				ctx.stream.sendError(error);
			}
			throw error;
		}

		String key = getCacheKey(ctx.name, groups);
		Strategy<ListenerEndpoint>[] strategies = emitterCache.get(key);
		if (strategies == null) {
			LinkedList<Strategy<ListenerEndpoint>> list = new LinkedList<>();
			registryReadLock.lock();
			try {
				for (Map.Entry<String, HashMap<String, Strategy<ListenerEndpoint>>> entry : listeners.entrySet()) {
					if (Matcher.matches(ctx.name, entry.getKey())) {
						if (groups != null) {
							for (Map.Entry<String, Strategy<ListenerEndpoint>> test : entry.getValue().entrySet()) {
								final String testGroup = test.getKey();
								for (String group : groups.groups()) {
									if (group.equals(testGroup)) {
										list.add(test.getValue());
									}
								}
							}
						} else {
							list.addAll(entry.getValue().values());
						}
					}
				}
			} finally {
				registryReadLock.unlock();
			}
			strategies = new Strategy[list.size()];
			list.toArray(strategies);
			emitterCache.put(key, strategies);
		}
		if (strategies.length == 0) {
			stopStreaming(ctx);
			return;
		}

		// Single listener
		if (strategies.length == 1) {
			try {

				// Invoke local or remote listener
				ListenerEndpoint endpoint = strategies[0].getEndpoint(ctx, local ? nodeID : null);
				if (endpoint == null || (endpoint.privateAccess && !nodeID.equals(ctx.nodeID))) {
					stopStreaming(ctx);
					return;
				}
				endpoint.on(ctx, groups, false);
			} catch (Exception cause) {
				if (ctx.stream != null) {
					ctx.stream.sendError(cause);
				}
				logger.error("Unable to invoke event listener!", cause);
			}
			return;
		}

		// Invoke local listeners
		if (local) {
			for (int i = 0; i < strategies.length; i++) {
				try {
					ListenerEndpoint endpoint = strategies[i].getEndpoint(ctx, nodeID);
					if (endpoint == null || (endpoint.privateAccess && !nodeID.equals(ctx.nodeID))) {
						continue;
					}
					endpoint.on(ctx, groups, false);
				} catch (Exception cause) {
					logger.error("Unable to invoke event listener!", cause);
				}
			}
			return;
		}

		// Invoke local and/or remote listeners
		// nodeID -> group set
		int size = strategies.length * 2;
		HashMap<String, HashSet<String>> groupsByNodeID = new HashMap<>(size);
		ListenerEndpoint[] endpoints = new ListenerEndpoint[strategies.length];

		// Group targets
		boolean foundLocal = false;
		for (int i = 0; i < strategies.length; i++) {
			ListenerEndpoint endpoint = strategies[i].getEndpoint(ctx, null);
			if (endpoint != null) {
				if (endpoint.isLocal()) {
					try {
						if (endpoint.privateAccess && !nodeID.equals(ctx.nodeID)) {
							continue;
						}
						foundLocal = true;
						endpoint.on(ctx, groups, false);
					} catch (Exception cause) {
						logger.error("Unable to invoke event listener!", cause);
					}
					continue;
				}
				HashSet<String> groupSet = groupsByNodeID.get(endpoint.getNodeID());
				if (groupSet == null) {
					groupSet = new HashSet<>(size);
					groupsByNodeID.put(endpoint.getNodeID(), groupSet);
				}
				groupSet.add(endpoint.group);
				endpoints[i] = endpoint;
			}
		}

		// Invoke endpoints
		if (groupsByNodeID.isEmpty()) {
			if (!foundLocal) {
				stopStreaming(ctx);
			}
		} else {
			for (ListenerEndpoint endpoint : endpoints) {
				if (endpoint != null) {
					try {
						HashSet<String> groupSet = groupsByNodeID.remove(endpoint.getNodeID());
						if (groupSet != null) {
							String[] array = new String[groupSet.size()];
							groupSet.toArray(array);
							endpoint.on(ctx, Groups.of(array), false);
						}
					} catch (Exception cause) {
						logger.error("Unable to invoke event listener!", cause);
					}
				}
			}
		}
	}

	protected void stopStreaming(Context ctx) {
		if (ctx.stream != null) {
			ctx.stream.sendError(new ListenerNotAvailableError(nodeID, ctx.name));
		}
	}

	// --- SEND EVENT TO ALL LISTENERS IN THE SPECIFIED GROUP ---

	@Override
	public void broadcast(Context ctx, Groups groups, boolean local) {

		// Verify call level
		if (maxCallLevel > 0 && ctx.level >= maxCallLevel) {
			MaxCallLevelError error = new MaxCallLevelError(nodeID, maxCallLevel);
			if (ctx.stream != null) {
				ctx.stream.sendError(error);
			}
			throw error;
		}

		String key = getCacheKey(ctx.name, groups);
		ListenerEndpoint[] endpoints;
		if (local) {
			endpoints = localBroadcasterCache.get(key);
		} else {
			endpoints = broadcasterCache.get(key);
		}
		if (endpoints == null) {
			HashSet<ListenerEndpoint> list = new HashSet<>();
			registryReadLock.lock();
			try {
				for (Map.Entry<String, HashMap<String, Strategy<ListenerEndpoint>>> entry : listeners.entrySet()) {
					if (Matcher.matches(ctx.name, entry.getKey())) {
						for (Map.Entry<String, Strategy<ListenerEndpoint>> test : entry.getValue().entrySet()) {
							if (groups != null) {
								final String testGroup = test.getKey();
								for (String group : groups.groups()) {
									if (group.equals(testGroup)) {
										for (ListenerEndpoint endpoint : test.getValue().getAllEndpoints()) {
											if (local) {
												if (endpoint.isLocal()) {
													list.add(endpoint);
												}
											} else {
												list.add(endpoint);
											}
										}
									}
								}
							} else {
								if (local) {
									for (ListenerEndpoint endpoint : test.getValue().getAllEndpoints()) {
										if (endpoint.isLocal()) {
											list.add(endpoint);
										}
									}
								} else {
									list.addAll(test.getValue().getAllEndpoints());
								}
							}
						}
					}
				}
			} finally {
				registryReadLock.unlock();
			}
			endpoints = new ListenerEndpoint[list.size()];
			list.toArray(endpoints);
			if (local) {
				localBroadcasterCache.put(key, endpoints);
			} else {
				broadcasterCache.put(key, endpoints);
			}
		}
		if (endpoints.length == 0) {
			stopStreaming(ctx);
			return;
		}

		// Single listener
		if (endpoints.length == 1) {
			try {
				if (endpoints[0].privateAccess && !nodeID.equals(ctx.nodeID)) {
					return;
				}
				endpoints[0].on(ctx, groups, true);
			} catch (Exception cause) {
				if (ctx.stream != null) {
					ctx.stream.sendError(cause);
				}
				logger.error("Unable to invoke event listener!", cause);
			}
			return;
		}

		// Group of listeners
		HashSet<String> nodeSet = new HashSet<>(endpoints.length * 2);
		for (ListenerEndpoint endpoint : endpoints) {
			try {
				if (endpoint.isLocal()) {
					if (endpoint.privateAccess && !nodeID.equals(ctx.nodeID)) {
						continue;
					}
				} else if (!nodeSet.add(endpoint.getNodeID())) {
					continue;
				}
				endpoint.on(ctx, groups, true);
			} catch (Exception cause) {
				logger.error("Unable to invoke event listener!", cause);
			}
		}
	}

	// --- CREATE CACHE KEY ---

	protected String getCacheKey(String name, Groups groups) {
		if (groups == null) {
			return name;
		}
		StringBuilder tmp = new StringBuilder(64);
		tmp.append(name);
		for (String group : groups.groups()) {
			tmp.append('|');
			tmp.append(group);
		}
		return tmp.toString();
	}

	// --- GENERATE LISTENER DESCRIPTOR ---

	@Override
	public Tree generateListenerDescriptor(String service) {
		LinkedHashMap<String, Object> descriptor = new LinkedHashMap<>();
		registryReadLock.lock();
		try {
			for (HashMap<String, Strategy<ListenerEndpoint>> groups : listeners.values()) {
				for (Strategy<ListenerEndpoint> strategy : groups.values()) {
					for (ListenerEndpoint endpoint : strategy.getAllEndpoints()) {
						if (endpoint.isLocal() && !endpoint.privateAccess && endpoint.serviceName.equals(service)) {
							LinkedHashMap<String, Object> map = new LinkedHashMap<>();
							descriptor.put(endpoint.subscribe, map);
							map.put("name", endpoint.subscribe);
							map.put("group", endpoint.group);
						}
					}
				}
			}
		} finally {
			registryReadLock.unlock();
		}
		return new CheckedTree(descriptor);
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

	public boolean isWriteErrorsToLog() {
		return writeErrorsToLog;
	}

	public void setWriteErrorsToLog(boolean writeErrorsToLog) {
		this.writeErrorsToLog = writeErrorsToLog;
	}

	public long getStreamTimeout() {
		return streamTimeout;
	}

	public void setStreamTimeout(long streamTimeout) {
		this.streamTimeout = streamTimeout;
	}

	public int getMaxCallLevel() {
		return maxCallLevel;
	}

	public void setMaxCallLevel(int maxCallLevel) {
		this.maxCallLevel = maxCallLevel;
	}

}