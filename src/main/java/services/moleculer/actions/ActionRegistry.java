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
	 * Strategy
	 */
	private final Class<? extends ActionInvoker> strategyClass;

	/**
	 * Action invokers
	 */
	private final HashMap<String, ActionInvoker> invokers;

	/**
	 * Reader lock
	 */
	private final Lock readerLock;

	/**
	 * Writer lock
	 */
	private final Lock writerLock;

	// --- CONSTRUCTOR ---

	public ActionRegistry(ServiceBroker broker, int initialCapacity, boolean fair) {

		// Init internal objects
		this.broker = broker;

		// Init invocation strategy
		InvocationStrategy strategy = broker.invocationStrategy();
		this.strategyClass = strategy == null || strategy == InvocationStrategy.ROUND_ROBIN
				? RoundRobinActionInvoker.class : RandomActionInvoker.class;

		// Init locker
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
		readerLock = lock.readLock();
		writerLock = lock.writeLock();

		// Init action / strategy map
		invokers = new HashMap<>(initialCapacity);
	}

	// --- ADD ACTION ---

	public final void add(String name, boolean cached, Action action) {
		LocalAction localAction;
		if (action instanceof LocalAction) {
			localAction = (LocalAction) action;
		} else {
			localAction = new LocalAction(broker, name, cached, action);
		}
		add(name, localAction);
	}

	public final void add(String name, boolean cached, String nodeID) {
		add(name, new RemoteAction(broker, nodeID, name, cached));
	}

	private final void add(String name, ActionContainer container) {
		writerLock.lock();
		try {
			ActionInvoker strategy = invokers.get(name);
			if (strategy == null) {
				strategy = strategyClass.newInstance();
				invokers.put(name, strategy);
			}
			strategy.add(container);
		} catch (Exception cause) {
			throw new IllegalArgumentException("Invalid strategy type!", cause);
		} finally {
			writerLock.unlock();
		}
	}
	
	// --- REMOVE ACTION ---

	public final void remove(String name, String nodeID) {
		remove(name, new RemoteAction(broker, nodeID, name, false));
	}

	public final void remove(String name, Action action) {
		LocalAction localAction;
		if (action instanceof LocalAction) {
			localAction = (LocalAction) action;
		} else {
			localAction = new LocalAction(broker, name, false, action);
		}
		remove(name, localAction);
	}

	private final void remove(String name, ActionContainer container) {
		writerLock.lock();
		try {
			ActionInvoker strategy = invokers.get(name);
			if (strategy != null) {
				strategy.remove(container);
				if (strategy.containers.length == 0) {
					invokers.remove(name);
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
			ActionInvoker strategy = invokers.get(name);
			if (strategy != null) {
				if (nodeID == null) {
					return strategy.next();
				}
				return strategy.get(nodeID);
			}
		} finally {
			readerLock.unlock();
		}
		throw new IllegalArgumentException("Unable to invoke action (NodeID: " + nodeID + ", name: " + name + ")!");
	}

}