package services.moleculer.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.dom.Cache;
import services.moleculer.Listener;

public final class EventBus {

	// --- EVENT BUS VARIABLES ---

	/**
	 * Main listener registry of the Event Bus
	 */
	private final HashMap<String, HashMap<Listener, Boolean>> listeners;

	/**
	 * Cache of the Event Bus
	 */
	private final Cache<String, Listener[]> listenerCache;

	/**
	 * Reader lock of the Event Bus
	 */
	private final Lock readerLock;

	/**
	 * Writer lock of the Event Bus
	 */
	private final Lock writerLock;
	
	// --- CONSTRUCTOR ---
	
	public EventBus() {
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
		readerLock = lock.readLock();
		writerLock = lock.writeLock();
		listeners = new HashMap<>(2048);
		listenerCache = new io.datatree.dom.Cache<>(2048, true);
	}
	
	// --- REGISTER LISTENER ----
	
	public final void on(String name, Listener listener, boolean once) {

		// Lock getter and setter threads
		writerLock.lock();
		try {
			HashMap<Listener, Boolean> handlers = listeners.get(name);
			if (handlers == null) {
				handlers = new HashMap<>();
				listeners.put(name, handlers);
			}
			handlers.put(listener, once);
		} finally {
			writerLock.unlock();
		}

		// Clear cache
		listenerCache.clear();
	}
	
	// --- UNREGISTER LISTENER ---
	
	/**
	 * Unsubscribe from an event
	 * 
	 * @param name
	 * @param handler
	 */
	public final void off(String name, Listener listener) {
		
		// Check listener
		boolean found = false;

		// Lock setter threads
		readerLock.lock();
		try {
			HashMap<Listener, Boolean> handlers = listeners.get(name);
			if (handlers != null) {
				found = handlers.containsKey(listener);
			}
		} finally {
			readerLock.unlock();
		}

		// Remove listener
		if (found) {

			// Lock getter and setter threads
			writerLock.lock();
			try {
				HashMap<Listener, Boolean> handlers = listeners.get(name);
				if (handlers != null) {

					// Remove listener
					handlers.remove(listener);
					if (handlers.isEmpty()) {
						listeners.remove(name);
					}

				}
			} finally {
				writerLock.unlock();
			}

			// Clear cache
			listenerCache.clear();
		}
	}
	
	// --- EMIT EVENT TO LISTENERS ---
	
	public final void emit(String name, Object payload, String sender) {

		// Get from cache
		Listener[] cachedListeners = listenerCache.get(name);
		
		// If not found...
		if (cachedListeners == null) {

			// Collected handlers
			final HashSet<Listener> collected = new HashSet<Listener>();

			// Processing variables
			Entry<String, HashMap<Listener, Boolean>> mappedEntry;
			HashMap<Listener, Boolean> listenersAndOnce;
			Iterator<Entry<Listener, Boolean>> listenersAndOnceIterator;
			Entry<Listener, Boolean> listenerAndOnce;
			boolean foundOnce = false;

			// Lock getter and setter threads
			writerLock.lock();
			try {

				// Iterator of all listener mappings
				final Iterator<Entry<String, HashMap<Listener, Boolean>>> mappingIterator = listeners.entrySet()
						.iterator();

				// Collect listeners
				while (mappingIterator.hasNext()) {
					mappedEntry = mappingIterator.next();
					listenersAndOnce = mappedEntry.getValue();

					// Matches?
					if (GlobMatcher.matches(name, mappedEntry.getKey())) {
						listenersAndOnceIterator = listenersAndOnce.entrySet().iterator();
						while (listenersAndOnceIterator.hasNext()) {
							listenerAndOnce = listenersAndOnceIterator.next();

							// Invoke once?
							if (listenerAndOnce.getValue()) {
								listenersAndOnceIterator.remove();
								foundOnce = true;
							}

							// Add to listener set
							collected.add(listenerAndOnce.getKey());
						}
					}

					// Empty map?
					if (listenersAndOnce.isEmpty()) {
						mappingIterator.remove();
						continue;
					}
				}

			} finally {
				writerLock.unlock();
			}

			// Convert listener set to array
			if (collected.isEmpty()) {
				cachedListeners = new Listener[0];
			} else {
				cachedListeners = new Listener[collected.size()];
				collected.toArray(cachedListeners);
			}

			// Store into cache
			if (!foundOnce) {
				listenerCache.put(name, cachedListeners);
			}
		}
		
		// Invoke one listener without looping
		if (cachedListeners.length == 1) {
			try {
				cachedListeners[0].on(payload);
			} catch (Exception cause) {
				cause.printStackTrace();
			}
			return;
		}

		// Invoke more listeners in loop
		if (cachedListeners.length > 1) {
			for (Listener listener : cachedListeners) {
				try {
					listener.on(payload);
				} catch (Exception cause) {
					cause.printStackTrace();
				}
			}
		}
	}
	
}