package services.moleculer.actions;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import services.moleculer.ServiceBroker;
import services.moleculer.strategies.InvocationStrategy;
import services.moleculer.strategies.InvocationStrategyFactory;

public final class ActionRegistry {

	// --- PROPERTIES ---

	/**
	 * Perent broker
	 */
	private final ServiceBroker broker;

	/**
	 * Default invocation strategy factory
	 */
	private final InvocationStrategyFactory strategyFactory;

	/**
	 * Default "preferLocal" flag
	 */
	private final boolean preferLocal;

	/**
	 * Invocation strategies by service
	 */
	private final HashMap<String, InvocationStrategy> strategies;

	/**
	 * Reader lock
	 */
	private final Lock readerLock;

	/**
	 * Writer lock
	 */
	private final Lock writerLock;

	// --- CONSTRUCTOR ---

	public ActionRegistry(ServiceBroker broker) {

		// Init internal objects
		this.broker = broker;

		// Set the default invocation strategy and "preferLocal" flag
		strategyFactory = broker.invocationStrategy();
		preferLocal = broker.isPreferLocal();

		// Init locks
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
		readerLock = lock.readLock();
		writerLock = lock.writeLock();

		// Init action / selector map
		strategies = new HashMap<>(512);
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
			InvocationStrategy serviceStrategy = strategies.get(name);
			if (serviceStrategy == null) {
				strategies.put(name, strategy);
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
			InvocationStrategy selector = strategies.get(name);
			if (selector != null) {
				selector.remove(container);
				if (selector.isEmpty()) {
					strategies.remove(name);
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
			InvocationStrategy selector = strategies.get(name);
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