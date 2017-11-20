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
package services.moleculer.eventbus;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Tree;
import io.datatree.dom.Cache;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;
import services.moleculer.strategy.Strategy;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.transporter.Transporter;

/**
 * Default EventBus implementation.
 */
@Name("Default Event Bus")
public final class DefaultEventBus extends EventBus {

	// --- REGISTERED EVENT LISTENERS ---

	private final HashMap<String, HashMap<String, Strategy<ListenerEndpoint>>> listeners = new HashMap<>(256);

	// --- CACHES ---

	private final Cache<String, Strategy<ListenerEndpoint>[]> emitterCache = new Cache<>(1024, true);
	private final Cache<String, ListenerEndpoint[]> broadcasterCache = new Cache<>(1024, true);

	// --- PROPERTIES ---

	/**
	 * Invoke all local listeners via Thread pool (true) or directly (false)
	 */
	private boolean asyncLocalInvocation;

	// --- LOCKS ---

	/**
	 * Reader lock of the Event Bus
	 */
	private final Lock readLock;

	/**
	 * Writer lock of the Event Bus
	 */
	private final Lock writeLock;

	// --- COMPONENTS ---

	private ServiceBroker broker;
	private StrategyFactory strategy;
	private Transporter transporter;

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
	public final void start(ServiceBroker broker, Tree config) throws Exception {

		// Process config
		asyncLocalInvocation = config.get(ASYNC_LOCAL_INVOCATION, asyncLocalInvocation);

		// Set components
		this.broker = broker;
		this.strategy = broker.components().strategy();
		this.transporter = broker.components().transporter();
	}

	// --- STOP EVENT BUS ---

	@Override
	public final void stop() {

		// Stop listener endpoints
		writeLock.lock();
		try {

			// TODO Stop strategies (and registered listeners)
			listeners.clear();

		} finally {
			writeLock.unlock();
		}
	}

	// --- RECEIVE EVENT FROM REMOTE SERVICE ---

	@Override
	public final void receiveEvent(Tree message) {

	}

	// --- ADD LOCAL LISTENER ---

	@Override
	public final void addListener(Listener listener, Tree config) throws Exception {
		writeLock.lock();
		try {

			// Process config
			String subscribe = config.get("subscribe", (String) null);
			if (subscribe == null || subscribe.isEmpty()) {
				Subscribe s = listener.getClass().getAnnotation(Subscribe.class);
				if (s != null) {
					subscribe = s.value();
					config.put("subscribe", subscribe);
				}
			}
			String group = Objects.requireNonNull(config.get("group", (String) null));
			boolean async = config.get(ASYNC_LOCAL_INVOCATION, asyncLocalInvocation);

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
			LocalListenerEndpoint endpoint = new LocalListenerEndpoint(listener, async);
			endpoint.start(broker, config);
			strategy.addEndpoint(endpoint);

		} finally {
			writeLock.unlock();
		}
	}

	// --- ADD REMOTE LISTENER ---

	@Override
	public final void addListener(Tree config) throws Exception {
		writeLock.lock();
		try {

			// TODO Add remote listener

		} finally {
			writeLock.unlock();
		}
	}

	// --- REMOVE ALL REMOTE SERVICES/ACTIONS OF A NODE ---

	@Override
	public final void removeListeners(String nodeID) {
		writeLock.lock();
		try {
			Iterator<HashMap<String, Strategy<ListenerEndpoint>>> groupIterator = listeners.values().iterator();
			while (groupIterator.hasNext()) {
				HashMap<String, Strategy<ListenerEndpoint>> groups = groupIterator.next();
				Iterator<Strategy<ListenerEndpoint>> strategyIterator = groups.values().iterator();
				while (strategyIterator.hasNext()) {
					Strategy<ListenerEndpoint> strategy = strategyIterator.next();
					strategy.remove(nodeID);
					if (strategy.isEmpty()) {
						strategyIterator.remove();
					}
				}
				if (groups.isEmpty()) {
					groupIterator.remove();
				}
			}
		} finally {
			writeLock.unlock();
		}
	}

	// --- EMIT EVENT TO LOCAL AND REMOTE LISTENERS BY GROUPS ---

	@Override
	@SuppressWarnings("unchecked")
	public final void emit(String name, Tree payload, Set<String> groups) {
		readLock.lock();
		try {
			String key = getCacheKey(false, name, groups);
			Strategy<ListenerEndpoint>[] strategies = emitterCache.get(key);
			if (strategies == null) {
				LinkedList<Strategy<ListenerEndpoint>> list = new LinkedList<>();
				for (Map.Entry<String, HashMap<String, Strategy<ListenerEndpoint>>> entry : listeners.entrySet()) {
					if (GlobMatcher.matches(name, entry.getKey())) {
						list.addAll(entry.getValue().values());
					}
				}
				strategies = new Strategy[list.size()];
				list.toArray(strategies);
				emitterCache.put(key, strategies);
			}
			for (Strategy<ListenerEndpoint> strategy : strategies) {
				try {
					strategy.getEndpoint(null).on(payload);
				} catch (Exception cause) {
					logger.error("Unable to invoke event listener!", cause);
				}
			}

		} finally {
			readLock.unlock();
		}
	}

	private static final String getCacheKey(boolean local, String name, Set<String> groups) {
		StringBuilder tmp = new StringBuilder(64);
		if (local) {
			tmp.append('>');
		} else {
			tmp.append('<');
		}
		tmp.append(name);
		if (groups != null && !groups.isEmpty()) {
			for (String group : groups) {
				tmp.append('|');
				tmp.append(group);
			}
		}
		return tmp.toString();
	}

	// --- EMIT EVENT TO LOCAL AND REMOTE LISTENERS ---

	@Override
	public final void broadcast(String name, Tree payload, Set<String> groups) {
		readLock.lock();
		try {

		} finally {
			readLock.unlock();
		}
	}

	// --- EMIT EVENT TO LOCAL LISTENERS ONLY ---

	@Override
	public final void broadcastLocal(String name, Tree payload, Set<String> groups) {
		readLock.lock();
		try {

		} finally {
			readLock.unlock();
		}
	}

}