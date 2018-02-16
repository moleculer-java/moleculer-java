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
package services.moleculer.eventbus;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Tree;
import io.datatree.dom.Cache;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.strategy.Strategy;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.util.CheckedTree;

/**
 * Default EventBus implementation.
 */
@Name("Default Event Bus")
public class DefaultEventBus extends EventBus {

	// --- REGISTERED EVENT LISTENERS ---

	protected final HashMap<String, HashMap<String, Strategy<ListenerEndpoint>>> listeners = new HashMap<>(256);

	// --- CACHES ---

	protected final Cache<String, Strategy<ListenerEndpoint>[]> emitterCache = new Cache<>(1024, true);
	protected final Cache<String, ListenerEndpoint[]> broadcasterCache = new Cache<>(1024, true);
	protected final Cache<String, ListenerEndpoint[]> localBroadcasterCache = new Cache<>(1024, true);

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
	 * Local Node ID
	 */
	protected String nodeID;

	// --- LOCKS ---

	/**
	 * Reader lock of the Event Bus
	 */
	protected final Lock readLock;

	/**
	 * Writer lock of the Event Bus
	 */
	protected final Lock writeLock;

	// --- COMPONENTS ---

	protected ServiceBroker broker;
	protected StrategyFactory strategy;

	// --- CONSTRUCTORS ---

	public DefaultEventBus() {
		this(false);
	}

	public DefaultEventBus(boolean asyncLocalInvocation) {

		// Async or direct local invocation
		this.asyncLocalInvocation = asyncLocalInvocation;

		// Create locks
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
		readLock = lock.readLock();
		writeLock = lock.writeLock();
	}

	// --- START EVENT BUS ---

	/**
	 * Initializes default EventBus instance.
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
		checkVersion = config.get("checkVersion", checkVersion);

		// Set nodeID
		nodeID = broker.nodeID();

		// Set components
		this.broker = broker;
		this.strategy = broker.components().strategy();
	}

	// --- STOP EVENT BUS ---

	@Override
	public void stop() {

		// Stop listener endpoints
		writeLock.lock();
		try {
			for (HashMap<String, Strategy<ListenerEndpoint>> groups : listeners.values()) {
				for (Strategy<ListenerEndpoint> strategy : groups.values()) {
					for (ListenerEndpoint endpoint : strategy.getAllEndpoints()) {
						endpoint.stop();
					}
					strategy.stop();
				}
			}
			listeners.clear();
		} finally {

			// Clear caches
			emitterCache.clear();
			broadcasterCache.clear();
			localBroadcasterCache.clear();

			writeLock.unlock();
		}
	}

	// --- RECEIVE EVENT FROM REMOTE SERVICE ---

	@Override
	public void receiveEvent(Tree message) {

		// Verify protocol version
		if (checkVersion) {
			String ver = message.get("ver", "unknown");
			if (!ServiceBroker.PROTOCOL_VERSION.equals(ver)) {
				logger.warn("Invalid protocol version (" + ver + ")!");
				return;
			}
		}

		// Get event property
		String name = message.get("event", (String) null);
		if (name == null || name.isEmpty()) {
			logger.warn("Missing \"event\" property!");
			return;
		}

		// Get data
		Tree payload = message.get("data");

		// Process events in Moleculer V2 style
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

		// Emit or broadcast?
		if (message.get("broadcast", true)) {

			// Broadcast
			broadcast(name, payload, groups, true);

		} else {

			// Emit
			emit(name, payload, groups, true);
		}
	}

	// --- ADD LOCAL LISTENER ---

	@Override
	public void addListeners(Service service, Tree config) throws Exception {
		writeLock.lock();
		try {

			// Initialize actions in services
			Class<? extends Service> clazz = service.getClass();
			Field[] fields = clazz.getFields();
			for (Field field : fields) {

				// Register event listener
				if (Listener.class.isAssignableFrom(field.getType())) {
					String listenerName = field.getName();
					Tree listenerConfig = config.get(listenerName);
					if (listenerConfig == null) {
						if (config.isMap()) {
							listenerConfig = config.putMap(listenerName);
						} else {
							listenerConfig = new Tree();
						}
					}

					// Name of the listener (eg. "v2.service.listener")
					// It's the subscribed event name by default
					listenerName = service.name() + '.' + listenerName;
					listenerConfig.put("name", listenerName);
					listenerConfig.put("nodeID", nodeID);
					listenerConfig.put("service", service.name());

					// Process "Subscribe" annotation
					String subscribe = listenerConfig.get("subscribe", (String) null);
					if (subscribe == null || subscribe.isEmpty()) {
						Subscribe s = field.getAnnotation(Subscribe.class);
						if (s != null) {
							subscribe = s.value();
						}
						if (subscribe == null || subscribe.isEmpty()) {
							subscribe = listenerName;
						}
						listenerConfig.put("subscribe", subscribe);
					}

					// Process "Group" annotation
					String group = listenerConfig.get("group", (String) null);
					if (group == null || group.isEmpty()) {
						Group g = field.getAnnotation(Group.class);
						if (g != null) {
							group = g.value();
						}
						if (group == null || group.isEmpty()) {
							group = service.name();
						}
						listenerConfig.put("group", group);
					}

					// Register listener in EventBus
					field.setAccessible(true);
					Listener listener = (Listener) field.get(service);

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
						strategy.start(broker, config);
						groups.put(group, strategy);
					}

					// Add endpoint to strategy
					LocalListenerEndpoint endpoint = new LocalListenerEndpoint(listener, asyncLocalInvocation);
					endpoint.start(broker, listenerConfig);
					strategy.addEndpoint(endpoint);
				}
			}
		} finally {

			// Clear caches
			emitterCache.clear();
			broadcasterCache.clear();
			localBroadcasterCache.clear();

			// Unlock reader threads
			writeLock.unlock();
		}
	}

	// --- ADD REMOTE LISTENER ---

	@Override
	public void addListeners(Tree config) throws Exception {
		Tree events = config.get("events");
		if (events != null && events.isMap()) {
			String nodeID = Objects.requireNonNull(config.get("nodeID", (String) null));
			String serviceName = Objects.requireNonNull(config.get("name", (String) null));
			writeLock.lock();
			try {
				for (Tree listenerConfig : events) {
					String subscribe = listenerConfig.get("name", "");
					String group = listenerConfig.get("group", serviceName);
					listenerConfig.putObject("nodeID", nodeID, true);
					listenerConfig.putObject("service", serviceName, true);
					listenerConfig.putObject("group", group, true);
					listenerConfig.putObject("subscribe", subscribe, true);

					// Register remote listener
					RemoteListenerEndpoint endpoint = new RemoteListenerEndpoint();
					endpoint.start(broker, listenerConfig);

					// Get or create group map
					HashMap<String, Strategy<ListenerEndpoint>> groups = listeners.get(subscribe);
					if (groups == null) {
						groups = new HashMap<String, Strategy<ListenerEndpoint>>();
						listeners.put(subscribe, groups);
					}

					// Get or create strategy
					Strategy<ListenerEndpoint> listenerStrategy = groups.get(group);
					if (listenerStrategy == null) {
						listenerStrategy = this.strategy.create();
						listenerStrategy.start(broker, config);
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
				writeLock.unlock();
			}
		}
	}

	// --- REMOVE ALL REMOTE SERVICES/ACTIONS OF A NODE ---

	@Override
	public void removeListeners(String nodeID) {
		boolean found = false;
		writeLock.lock();
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
							try {
								strategy.stop();
							} catch (Throwable cause) {
								logger.warn("Unable to stop strategy!", cause);
							}
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

			writeLock.unlock();
		}
	}

	// --- SEND EVENT TO ONE LISTENER IN THE SPECIFIED GROUP ---

	@Override
	@SuppressWarnings("unchecked")
	public void emit(String name, Tree payload, Groups groups, boolean local) {
		String key = getCacheKey(name, groups);
		Strategy<ListenerEndpoint>[] strategies = emitterCache.get(key);
		if (strategies == null) {
			LinkedList<Strategy<ListenerEndpoint>> list = new LinkedList<>();
			readLock.lock();
			try {
				for (Map.Entry<String, HashMap<String, Strategy<ListenerEndpoint>>> entry : listeners.entrySet()) {
					if (Matcher.matches(name, entry.getKey())) {
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
				readLock.unlock();
			}
			strategies = new Strategy[list.size()];
			list.toArray(strategies);
			emitterCache.put(key, strategies);
		}
		if (strategies.length == 0) {
			return;
		}
		if (strategies.length == 1) {
			try {

				// Invoke local or remote listener
				ListenerEndpoint endpoint = strategies[0].getEndpoint(local ? nodeID : null);
				if (endpoint != null) {
					endpoint.on(name, payload, groups, false);
				}
			} catch (Exception cause) {
				logger.error("Unable to invoke event listener!", cause);
			}
			return;
		}
		if (strategies.length > 0) {
			if (local) {

				// Invoke local listeners
				for (int i = 0; i < strategies.length; i++) {
					try {
						ListenerEndpoint endpoint = strategies[i].getEndpoint(nodeID);
						if (endpoint != null) {
							endpoint.on(name, payload, groups, false);
						}
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
			for (int i = 0; i < strategies.length; i++) {
				ListenerEndpoint endpoint = strategies[i].getEndpoint(null);
				if (endpoint != null) {
					HashSet<String> groupSet = groupsByNodeID.get(endpoint.nodeID);
					if (groupSet == null) {
						groupSet = new HashSet<>(size);
						groupsByNodeID.put(endpoint.nodeID, groupSet);
					}
					groupSet.add(endpoint.group);
					endpoints[i] = endpoint;
				}
			}

			// Invoke endpoints
			for (ListenerEndpoint endpoint : endpoints) {
				if (endpoint != null) {
					try {
						HashSet<String> groupSet = groupsByNodeID.remove(endpoint.nodeID);
						if (groupSet != null) {
							String[] array = new String[groupSet.size()];
							groupSet.toArray(array);
							endpoint.on(name, payload, Groups.of(array), false);
						}
					} catch (Exception cause) {
						logger.error("Unable to invoke event listener!", cause);
					}
				}
			}
		}
	}

	// --- SEND EVENT TO ALL LISTENERS IN THE SPECIFIED GROUP ---

	@Override
	public void broadcast(String name, Tree payload, Groups groups, boolean local) {
		String key = getCacheKey(name, groups);
		ListenerEndpoint[] endpoints;
		if (local) {
			endpoints = localBroadcasterCache.get(key);
		} else {
			endpoints = broadcasterCache.get(key);
		}
		if (endpoints == null) {
			HashSet<ListenerEndpoint> list = new HashSet<>();
			readLock.lock();
			try {
				for (Map.Entry<String, HashMap<String, Strategy<ListenerEndpoint>>> entry : listeners.entrySet()) {
					if (Matcher.matches(name, entry.getKey())) {
						for (Map.Entry<String, Strategy<ListenerEndpoint>> test : entry.getValue().entrySet()) {
							if (groups != null) {
								final String testGroup = test.getKey();
								for (String group : groups.groups()) {
									if (group.equals(testGroup)) {
										for (ListenerEndpoint endpoint : test.getValue().getAllEndpoints()) {
											if (local) {
												if (endpoint.local()) {
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
										if (local) {
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
				readLock.unlock();
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
			return;
		}
		if (endpoints.length == 1) {
			try {
				endpoints[0].on(name, payload, groups, true);
			} catch (Exception cause) {
				logger.error("Unable to invoke event listener!", cause);
			}
			return;
		}
		HashSet<String> nodeSet = new HashSet<>(endpoints.length * 2);
		for (ListenerEndpoint endpoint : endpoints) {
			if (nodeSet.add(endpoint.nodeID)) {
				try {
					endpoint.on(name, payload, groups, true);
				} catch (Exception cause) {
					logger.error("Unable to invoke event listener!", cause);
				}
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
		readLock.lock();
		try {
			for (HashMap<String, Strategy<ListenerEndpoint>> groups : listeners.values()) {
				for (Strategy<ListenerEndpoint> strategy : groups.values()) {
					for (ListenerEndpoint endpoint : strategy.getAllEndpoints()) {
						if (endpoint.local() && endpoint.service.endsWith(service)) {
							LinkedHashMap<String, Object> map = new LinkedHashMap<>();
							descriptor.put(endpoint.subscribe, map);
							map.put("name", endpoint.subscribe);
							map.put("group", endpoint.group);
						}
					}
				}
			}
		} finally {
			readLock.unlock();
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

}