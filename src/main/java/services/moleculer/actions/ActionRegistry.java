package services.moleculer.actions;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import services.moleculer.Action;
import services.moleculer.InvocationStrategy;
import services.moleculer.ServiceBroker;

public final class ActionRegistry {

	// --- PROPERTIES ---

	/**
	 * Perent broker
	 */
	private final ServiceBroker broker;

	/**
	 * Prefer local calls
	 */
	private final boolean preferLocal;
	
	/**
	 * Invoker type
	 */
	private final Class<? extends ActionSelector> selectorClass;

	/**
	 * Action selectors
	 */
	private final HashMap<String, ActionSelector> selectors;

	/**
	 * Reader lock
	 */
	private final Lock readerLock;

	/**
	 * Writer lock
	 */
	private final Lock writerLock;

	// --- CONSTRUCTOR ---

	public ActionRegistry(ServiceBroker broker, boolean preferLocal, int initialCapacity, boolean fair) {

		// Init internal objects
		this.broker = broker;
		this.preferLocal = preferLocal;

		// Init invocation strategy
		InvocationStrategy strategy = broker.invocationStrategy();
		this.selectorClass = strategy == null || strategy == InvocationStrategy.ROUND_ROBIN
				? RoundRobinActionInvoker.class : RandomActionInvoker.class;

		// Init locker
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
		readerLock = lock.readLock();
		writerLock = lock.writeLock();

		// Init action / selector map
		selectors = new HashMap<>(initialCapacity);
	}

	// --- ADD ACTION ---

	public final void add(String name, boolean cached, String[] keys, Action action) {
		LocalAction localAction;
		if (action instanceof LocalAction) {
			localAction = (LocalAction) action;
		} else {
			localAction = new LocalAction(broker, name, cached, keys, action);
		}
		add(name, localAction);
	}

	public final void add(String name, boolean cached, String[] keys, String nodeID) {
		add(name, new RemoteAction(broker, nodeID, name, cached, keys));
	}

	private final void add(String name, ActionContainer container) {
		writerLock.lock();
		try {
			ActionSelector selector = selectors.get(name);
			if (selector == null) {
				selector = selectorClass.newInstance();
				selectors.put(name, selector);
			}
			selector.add(container);
		} catch (Exception cause) {
			throw new IllegalArgumentException("Invalid invocation strategy type!", cause);
		} finally {
			writerLock.unlock();
		}
	}

	// --- REMOVE ACTION ---

	public final void remove(String name, String nodeID) {
		remove(name, new RemoteAction(broker, nodeID, name, false, null));
	}

	public final void remove(String name, Action action) {
		LocalAction localAction;
		if (action instanceof LocalAction) {
			localAction = (LocalAction) action;
		} else {
			localAction = new LocalAction(broker, name, false, null, action);
		}
		remove(name, localAction);
	}

	private final void remove(String name, ActionContainer container) {
		writerLock.lock();
		try {
			ActionSelector selector = selectors.get(name);
			if (selector != null) {
				selector.remove(container);
				if (selector.containers.length == 0) {
					selectors.remove(name);
				}
			}
		} finally {
			writerLock.unlock();
		}
	}

	// --- GET ACTION ---

	public final Action get(String name) {
		return get(null, name);
	}

	public final Action get(String nodeID, String name) {
		readerLock.lock();
		try {
			ActionSelector selector = selectors.get(name);
			if (selector != null) {
				if (nodeID == null) {
					if (preferLocal) {
						return selector.nextButPreferLocal();
					}
					return selector.next();
				}
				return selector.get(nodeID);
			}
		} finally {
			readerLock.unlock();
		}
		throw new IllegalArgumentException("Unable to find action (NodeID: " + nodeID + ", name: " + name + ")!");
	}

}