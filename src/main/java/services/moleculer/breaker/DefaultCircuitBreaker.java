/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
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
package services.moleculer.breaker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.CallOptions;
import services.moleculer.context.Context;
import services.moleculer.context.ContextFactory;
import services.moleculer.service.ActionEndpoint;
import services.moleculer.service.Name;
import services.moleculer.service.ServiceRegistry;

/**
 * Default implementation of Circuit Breaker.
 */
@Name("Default Circuit Breaker")
public class DefaultCircuitBreaker extends CircuitBreaker implements Runnable {

	// --- PROPERTIES ---

	/**
	 * Enable Circuit Breaker
	 */
	protected boolean enabled;

	/**
	 * Number of max tries
	 */
	protected int maxTries = 3;

	/**
	 * Cleanup period time, in SECONDS (0 = disable cleanup process)
	 */
	protected int cleanup = 60;

	protected long windowSize = 10 * 1000L;
	
	protected int maxErrors = 5;
	
	// --- COMPONENTS ---

	protected ServiceRegistry serviceRegistry;
	protected ContextFactory contextFactory;

	// --- IGNORABLE ERRORS / EXCEPTIONS ---

	protected Set<Class<? extends Throwable>> ignoredTypes = new HashSet<>();

	// --- ERROR STORE ---

	protected HashMap<StatusKey, EndpointStatus> endpointStatuses = new HashMap<>(1024);

	// --- LOCKS ---

	protected final Lock readLock;
	protected final Lock writeLock;

	// --- CONSTRUCTOR ---

	public DefaultCircuitBreaker() {

		// Init locks
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
		readLock = lock.readLock();
		writeLock = lock.writeLock();
	}

	// --- START BREAKER ---

	/**
	 * Cancelable timer
	 */
	protected volatile ScheduledFuture<?> timer;

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Set components
		ServiceBrokerConfig cfg = broker.getConfig();
		this.serviceRegistry = cfg.getServiceRegistry();
		this.contextFactory = cfg.getContextFactory();

		// Start timer
		if (cleanup > 0) {
			timer = broker.getConfig().getScheduler().scheduleWithFixedDelay(this, cleanup, cleanup, TimeUnit.SECONDS);
		}
	}

	// --- REMOVE OLD ENTRIES ---

	@Override
	public void run() {
		if (!enabled) {
			
			// Circuit Breaker is disabled
			return;
		}

		// Do cleanup process
		writeLock.lock();
		try {
			Iterator<EndpointStatus> i = endpointStatuses.values().iterator();
			while (i.hasNext()) {
				if (i.next().isRemovable()) {
					i.remove();
				}
			}
		} finally {
			writeLock.unlock();
		}
	}

	// --- STOP BREAKER ---

	@Override
	public void stopped() {

		// Stop timer
		if (timer != null) {
			timer.cancel(false);
			timer = null;
		}

		// Clear statuses
		writeLock.lock();
		try {
			endpointStatuses.clear();
		} finally {
			writeLock.unlock();
		}

		// Clear types
		ignoredTypes.clear();
	}

	// --- CALL SERVICE ---

	public Promise call(String name, Tree params, CallOptions.Options opts, Context parent) {
		int remaining = opts == null ? 0 : opts.retryCount;
		return call(name, params, opts, remaining, parent);
	}

	protected Promise call(String name, Tree params, CallOptions.Options opts, int remaining, Context parent) {
		ActionEndpoint action = null;
		try {
			String targetID = opts == null ? null : opts.nodeID;
			action = (ActionEndpoint) serviceRegistry.getAction(name, targetID);
			if (enabled && targetID == null) {

				// Circuit Breaker is enabled -> check availability of endpoint
				for (int i = 0; i < maxTries; i++) {
					if (isAvailable(action.getNodeID(), name)) {

						// Endpoint is available -> OK
						break;
					}

					// Try to choose another endpoint
					action = (ActionEndpoint) serviceRegistry.getAction(name, targetID);
				}
			}
			Context ctx = contextFactory.create(name, params, opts, parent);
			if (remaining < 1) {
				if (enabled) {

					// Circuit Breaker is enabled -> catch all errors
					final String nodeID = action.getNodeID();
					return Promise.resolve(action.handler(ctx)).catchError(cause -> {
						onError(nodeID, name, cause);
						return cause;
					});
				}
				return Promise.resolve(action.handler(ctx));
			}
			final String nodeID = action.getNodeID();
			return Promise.resolve(action.handler(ctx)).catchError(cause -> {
				if (enabled) {

					// Circuit Breaker is enabled -> catch error
					onError(nodeID, name, cause);
				}
				return retry(cause, name, params, opts, remaining, parent);
			});
		} catch (Throwable cause) {
			if (enabled && action != null) {

				// Circuit Breaker is enabled -> catch error
				onError(action.getNodeID(), name, cause);
			}
			if (remaining < 1) {
				return Promise.reject(cause);
			}
			return retry(cause, name, params, opts, remaining, parent);
		}
	}

	protected Promise retry(Throwable cause, String name, Tree params, CallOptions.Options opts, int remaining,
			Context parent) {
		remaining--;
		logger.warn("Retrying request (" + remaining + " attempts left)...", cause);
		return call(name, params, opts, remaining, parent);
	}

	// --- STORE ERROR ---

	protected void onError(String nodeID, String name, Throwable cause) {
		if (!ignoredTypes.isEmpty()) {
			Class<? extends Throwable> test = cause.getClass();
			for (Class<? extends Throwable> type : ignoredTypes) {
				if (type.isAssignableFrom(test)) {

					// Ignore error
					return;
				}
			}
		}

		// Get or create status container
		StatusKey key = new StatusKey(nodeID, name);
		EndpointStatus status;
		writeLock.lock();
		try {
			status = endpointStatuses.get(key);
			if (status == null) {
				status = new EndpointStatus(windowSize, maxErrors);
				endpointStatuses.put(key, status);
			}
		} finally {
			writeLock.unlock();
		}

		// Store error time
		status.onError();
	}

	// --- CHECK ERROR ---

	protected boolean isAvailable(String nodeID, String name) {

		// Get status container
		StatusKey key = new StatusKey(nodeID, name);
		EndpointStatus status;
		readLock.lock();
		try {
			status = endpointStatuses.get(key);
		} finally {
			readLock.unlock();
		}
		return status == null || status.isAvailable();
	}

	// --- ADD / REMOVE IGNORED ERROR / EXCEPTION ---

	public void addIgnoredType(Class<? extends Throwable> type) {
		ignoredTypes.add(type);
	}

	public void removeIgnoredType(Class<? extends Throwable> type) {
		ignoredTypes.remove(type);
	}

	// --- GETTERS / SETTERS ---

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Set<Class<? extends Throwable>> getIgnoredTypes() {
		return ignoredTypes;
	}

	public void setIgnoredTypes(Set<Class<? extends Throwable>> ignoredTypes) {
		this.ignoredTypes = Objects.requireNonNull(ignoredTypes);
	}

}