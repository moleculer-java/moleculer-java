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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Tree;
import io.datatree.dom.Cache;
import services.moleculer.service.Name;

/**
 * 
 */
@Name("Default Event Bus")
public final class DefaultEventBus extends EventBus {

	// --- EVENT BUS VARIABLES ---

	/**
	 * Main listener registry of the Event Bus
	 */
	private final HashMap<String, HashMap<Listener, Boolean>> listeners;

	/**
	 * Cache of the Event Bus
	 */
	private final Cache<String, Listener[]> cache;

	/**
	 * Reader lock of the Event Bus
	 */
	private final Lock readLock;

	/**
	 * Writer lock of the Event Bus
	 */
	private final Lock writeLock;

	// --- CONSTRUCTOR ---

	public DefaultEventBus() {
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
		readLock = lock.readLock();
		writeLock = lock.writeLock();
		listeners = new HashMap<>(2048);
		cache = new Cache<>(2048, true);
	}

	// --- REGISTER LISTENER ----

	@Override
	public final void on(String name, Listener listener, boolean once) {

		// Lock getter and setter threads
		writeLock.lock();
		try {
			HashMap<Listener, Boolean> handlers = listeners.get(name);
			if (handlers == null) {
				handlers = new HashMap<>();
				listeners.put(name, handlers);
			}
			handlers.put(listener, once);
		} finally {
			writeLock.unlock();
		}

		// Clear cache
		cache.clear();
	}

	// --- UNREGISTER LISTENER ---

	/**
	 * Unsubscribe from an event
	 * 
	 * @param name
	 * @param handler
	 */
	@Override
	public final void off(String name, Listener listener) {

		// Check listener
		boolean found = false;

		// Lock setter threads
		readLock.lock();
		try {
			HashMap<Listener, Boolean> handlers = listeners.get(name);
			if (handlers != null) {
				found = handlers.containsKey(listener);
			}
		} finally {
			readLock.unlock();
		}

		// Remove listener
		if (found) {

			// Lock getter and setter threads
			writeLock.lock();
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
				writeLock.unlock();
			}

			// Clear cache
			cache.clear();
		}
	}

	// --- EMIT EVENT TO LISTENERS ---

	@Override
	public final void emit(String name, Tree payload) {

		// Get from cache
		Listener[] cachedListeners = cache.get(name);

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
			writeLock.lock();
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
				writeLock.unlock();
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
				cache.put(name, cachedListeners);
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