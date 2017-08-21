package services.moleculer.actions;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import services.moleculer.Action;
import services.moleculer.Cache;
import services.moleculer.Context;
import services.moleculer.InvocationStrategy;
import services.moleculer.ServiceBroker;
import services.moleculer.Version;
import services.moleculer.cachers.Cacher;

public class ActionRegistry implements Action {

	// --- VARIABLES ---

	/**
	 * ServiceBroker
	 */
	private final ServiceBroker broker;

	/**
	 * Cacher
	 */
	private final Cacher cacher;

	/**
	 * Strategy
	 */
	private final Class<? extends ActionInvoker> strategyClass;

	/**
	 * Registered actions
	 */
	private final HashMap<String, ActionInvoker> strategies;

	/**
	 * Reader lock
	 */
	private final Lock readerLock;

	/**
	 * Writer lock
	 */
	private final Lock writerLock;

	// --- CONSTRUCTOR ---

	public ActionRegistry(ServiceBroker broker, boolean fair) {

		// Init internal objects
		this.broker = broker;
		this.cacher = broker.getCacher();

		// Init invocation strategy
		InvocationStrategy strategy = broker.getInvocationStrategy();
		this.strategyClass = strategy == null || strategy == InvocationStrategy.ROUND_ROBIN
				? RoundRobinActionInvoker.class : RandomActionInvoker.class;

		// Init locker
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
		readerLock = lock.readLock();
		writerLock = lock.writeLock();

		// Init action / strategy map
		strategies = new HashMap<>(2048);
	}

	// --- INVOKE ACTION ---

	@Override
	public Object handler(Context ctx) throws Exception {
		return getAction(ctx.nodeID, ctx.name).handler(ctx);
	}
	
	// --- GET ACTION ---
	
	public final Action getAction(String nodeID, String name) {
		
		// Lock setter threads
		readerLock.lock();
		try {
			ActionInvoker strategy = strategies.get(name);
			if (strategy != null) {
				if (nodeID == null) {
					return strategy.next();
				}
				return strategy.get(nodeID);
			}
		} finally {
			readerLock.unlock();
		}
		throw new IllegalArgumentException(
				"Unable to invoke action (NodeID: " + nodeID + ", name: " + name + ")!");
	}

	// --- REGISTER LOCAL ACTION ---

	public final void registerLocalAction(String name, Action action) {
		registerAction(name, newLocalAction(name, action));
	}

	// --- UNREGISTER LOCAL ACTION ---

	public final void unregisterLocalAction(String name, Action action) {
		unregisterAction(name, newLocalAction(name, action));
	}

	// --- REGISTER REMOTE ACTION ---

	public final void registerRemoteAction(String name, boolean cached, String nodeID) {
		registerAction(name, newRemoteAction(name, cached, nodeID));
	}

	// --- UNREGISTER REMOTE ACTION ---

	public final void unregisterRemoteAction(String name, boolean cached, String nodeID) {
		unregisterAction(name, newRemoteAction(name, cached, nodeID));
	}

	// --- COMMON REGISTER METHOD ----

	private final void registerAction(String name, ActionContainer container) {

		// Lock getter and setter threads
		writerLock.lock();
		try {
			ActionInvoker strategy = strategies.get(name);
			if (strategy == null) {
				strategy = strategyClass.newInstance();
				strategies.put(name, strategy);
			}
			strategy.addContainer(container);
		} catch (Exception cause) {
			throw new IllegalArgumentException("Invalid strategy type!", cause);
		} finally {
			writerLock.unlock();
		}
	}

	// --- COMMON UNREGISTER METHOD ----

	private final void unregisterAction(String name, ActionContainer container) {

		// Lock getter and setter threads
		writerLock.lock();
		try {
			ActionInvoker strategy = strategies.get(name);
			if (strategy != null) {
				strategy.removeContainer(container);
				if (strategy.containers.length == 0) {
					strategies.remove(name);
				}
			}
		} finally {
			writerLock.unlock();
		}
	}

	// --- WRAP REMOTE ACTION ---

	private final ActionContainer newRemoteAction(String name, boolean cached, String nodeID) {
		return new RemoteAction(broker, cached ? cacher : null, nodeID, name);
	}
	
	// --- WRAP LOCAL ACTION ---

	private final ActionContainer newLocalAction(String name, Action action) {
		if (action instanceof ActionContainer) {
			return (ActionContainer) action;
		}
		Annotation[] annotations = action.getClass().getAnnotations();

		// Annotation values
		boolean cached = false;
		String version = null;

		for (Annotation annotation : annotations) {
			if (annotation instanceof Cache) {
				cached = ((Cache) annotation).value();
				continue;
			}
			if (annotation instanceof Version) {
				version = ((Version) annotation).value();
				continue;
			}
		}
		if (version != null && !version.isEmpty()) {
			name = version + '.' + name;
		}
		return new LocalAction(broker, cached ? cacher : null, name, action);
	}

}