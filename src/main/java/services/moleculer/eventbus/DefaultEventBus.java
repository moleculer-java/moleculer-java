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
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Tree;
import io.datatree.dom.Cache;
import services.moleculer.service.Name;

/**
 * Default EventBus implementation.
 */
@Name("Default Event Bus")
public final class DefaultEventBus extends EventBus {

	// --- PROPERTIES ---

	/**
	 * Listener registry of the Event Bus
	 */
	private final HashMap<String, Listener[]> listeners;

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
	public final void on(String name, Listener listener) {

		// Lock getter and setter threads
		writeLock.lock();
		try {
			Listener[] array = listeners.get(name);
			if (array == null) {
				array = new Listener[1];
				array[0] = listener;
				listeners.put(name, array);
			} else {
				for (int i = 0; i < array.length; i++) {
					if (array[i] == listener) {

						// Already registered
						return;
					}
				}

				// Add to array
				Listener[] copy = new Listener[array.length + 1];
				System.arraycopy(array, 0, copy, 0, array.length);
				copy[array.length] = listener;
				listeners.put(name, copy);
			}
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

		// Lock getter and setter threads
		writeLock.lock();
		try {
			Listener[] array = listeners.get(name);
			if (array != null) {
				for (int i = 0; i < array.length; i++) {
					if (array[i] == listener) {
						if (array.length == 1) {
							listeners.remove(name);
						} else {
							Listener[] copy = new Listener[array.length - 1];
							System.arraycopy(array, 0, copy, 0, i);
							System.arraycopy(array, i + 1, copy, i, array.length - i - 1);
							listeners.put(name, copy);
						}
					}
				}
			}
		} finally {
			writeLock.unlock();
		}

		// Clear cache
		cache.clear();
	}

	// --- EMIT EVENT TO LISTENERS ---

	@Override
	public final void emit(String name, Tree payload) {

		// Get from cache
		Listener[] cachedListeners = cache.get(name);

		// If not found in cache...
		if (cachedListeners == null) {

			// Collected listeners
			final HashSet<Listener> collected = new HashSet<Listener>();

			// Lock getter and setter threads
			readLock.lock();
			try {
				for (Entry<String, Listener[]> entry : listeners.entrySet()) {

					// Matches?
					if (GlobMatcher.matches(name, entry.getKey())) {
						for (Listener listener : entry.getValue()) {
							collected.add(listener);
						}
					}
				}
			} finally {
				readLock.unlock();
			}

			// Convert listener set to array
			if (collected.isEmpty()) {
				cachedListeners = new Listener[0];
			} else {
				cachedListeners = new Listener[collected.size()];
				collected.toArray(cachedListeners);
			}

			// Store into cache
			cache.put(name, cachedListeners);
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