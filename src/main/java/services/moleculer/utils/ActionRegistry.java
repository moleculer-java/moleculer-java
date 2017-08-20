package services.moleculer.utils;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import services.moleculer.Action;
import services.moleculer.Cache;
import services.moleculer.Version;
import services.moleculer.transporters.Transporter;

public class ActionRegistry {

	/**
	 * Registered actions
	 */
	private final HashMap<String, ActionContainer[]> actions;

	/**
	 * Reader lock of the Action Registry
	 */
	private final Lock readerLock;

	/**
	 * Writer lock of the Action Registry
	 */
	private final Lock writerLock;

	/**
	 * Transporter instance of the ServiceBroker (or null)
	 */
	private final Transporter transporter;

	// --- CONSTRUCTOR ---

	public ActionRegistry(Transporter transporter) {
		this.transporter = transporter;
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
		readerLock = lock.readLock();
		writerLock = lock.writeLock();
		actions = new HashMap<>(2048);
	}

	// --- REGISTER LOCAL ACTION ---

	public final void registerLocalAction(String name, Action action) {
		registerUnregisterLocalAction(name, action, true);
	}

	// --- REGISTER REMOTE ACTION ---

	public final void registerRemoteAction(String name, boolean cached, String nodeID) {

		// Register remote action
		registerAction(name, new ActionRemoteContainer(cached, nodeID, transporter));
	}

	// --- UNREGISTER LOCAL ACTION ---
	
	public final void unregisterLocalAction(String name, Action action) {
		registerUnregisterLocalAction(name, action, false);
	}
	
	// --- UNREGISTER REMOTE ACTION ---

	public final void unregisterRemoteAction(String name, boolean cached, String nodeID) {

		// Register remote action
		unregisterAction(name, new ActionRemoteContainer(cached, nodeID, transporter));
	}
	
	// --- COMMON REGISTER / UNREGISTER LOCAL ACTION ---
	
	private final void registerUnregisterLocalAction(String name, Action action, boolean register) {
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
		ActionContainer container = new ActionLocalContainer(cached, action);
		if (register) {
			registerAction(name, container);			
		} else {
			unregisterAction(name, container);			
		}
	}
	
	// --- COMMON REGISTER METHOD ----

	private final void registerAction(String name, ActionContainer container) {

		// Lock getter and setter threads
		writerLock.lock();
		try {

			ActionContainer[] containers = actions.get(name);
			if (containers == null) {
				containers = new ActionContainer[1];
				containers[0] = container;
				actions.put(name, containers);
			} else {
				for (int i = 0; i < containers.length; i++) {
					if (containers[i].equals(container)) {

						// Already registered
						return;
					}
				}

				// Add to array
				containers = Arrays.copyOf(containers, containers.length + 1);
				containers[containers.length - 1] = container;
				actions.put(name, containers);
			}

		} finally {
			writerLock.unlock();
		}
	}
	
	// --- COMMON UNREGISTER METHOD ----

	private final void unregisterAction(String name, ActionContainer container) {

		// Lock getter and setter threads
		writerLock.lock();
		try {

			ActionContainer[] containers = actions.get(name);
			if (containers != null) {
				boolean found = false;
				for (int i = 0; i < containers.length; i++) {
					if (containers[i].equals(container)) {
						ActionContainer[] copy = new ActionContainer[containers.length - 1];
						System.arraycopy(containers, 0, copy, 0, i);
						System.arraycopy(containers, i + 1, copy, i, containers.length - i - 1);
						containers = copy;
						found = true;
						break;
					}
				}
				if (found) {
					if (containers.length < 1) {
						actions.remove(name);
					} else {
						actions.put(name, containers);
					}
				}
			}

		} finally {
			writerLock.unlock();
		}
	}

}