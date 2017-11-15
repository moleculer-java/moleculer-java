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

	// --- REGISTERED STRATEGIES PER ACTIONS ---

	private final HashMap<String, HashMap<String, Strategy<ListenerEndpoint>>> strategies = new HashMap<>(256);

	// --- CACHE ---

	/**
	 * Cache of the Event Bus
	 */
	private final Cache<String, Listener[]> cache = new Cache<>(2048, true);

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
			strategies.clear();

		} finally {
			writeLock.unlock();
		}
	}
	
	// --- RECEIVE EVENT FROM REMOTE SERVICE ---

	public final void receiveEvent(Tree message) {
		
	}
	
	// --- ADD LOCAL LISTENER ---

	public final void addListener(Listener listener, Tree config) throws Exception {
		
		// Process config
		String subscribe = config.get("subscribe", (String) null);
		if (subscribe == null || subscribe.isEmpty()) {
			Subscribe s = listener.getClass().getAnnotation(Subscribe.class);
			if (s != null) {
				subscribe = s.value();
				config.put("subscribe", subscribe);
			}
		}
		boolean async = config.get(ASYNC_LOCAL_INVOCATION, asyncLocalInvocation);
		
		// TODO Get or create strategy
		Strategy<ListenerEndpoint> strategy = null;
		
		// Add endpoint
		LocalListenerEndpoint endpoint = new LocalListenerEndpoint(listener, async);
		endpoint.start(broker, config);
		strategy.addEndpoint(endpoint);
	}

	// --- ADD REMOTE LISTENER ---

	public final void addListener(Tree config) throws Exception {
		
	}
	
	// --- REMOVE ALL REMOTE SERVICES/ACTIONS OF A NODE ---

	public final void removeListeners(String nodeID) {
		
	}

	// --- EMIT EVENT TO LOCAL AND REMOTE LISTENERS BY GROUPS ---

	public final void emit(String name, Tree payload, String[] groups) {
		
	}

	// --- EMIT EVENT TO LOCAL AND REMOTE LISTENERS ---

	public final void broadcast(String name, Tree payload, String[] groups) {
		
	}

	// --- EMIT EVENT TO LOCAL LISTENERS ONLY ---

	public final void broadcastLocal(String name, Tree payload, String[] groups) {
		
	}
	
}