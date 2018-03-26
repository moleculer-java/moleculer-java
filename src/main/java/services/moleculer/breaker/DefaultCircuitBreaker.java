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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.CallOptions;
import services.moleculer.context.Context;
import services.moleculer.context.ContextFactory;
import services.moleculer.service.Action;
import services.moleculer.service.ActionEndpoint;
import services.moleculer.service.Name;
import services.moleculer.service.ServiceRegistry;

/**
 * Default implementation of Circuit Breaker.
 */
@Name("Default Circuit Breaker")
public class DefaultCircuitBreaker extends CircuitBreaker {

	// --- PROPERTIES ---

	/**
	 * Enable Circuit Breaker
	 */
	protected boolean enabled;

	/**
	 * Number of max tries
	 */
	protected int maxTries = 32;

	/**
	 * Exit from "maxTries" loop, when strategy returns number of "maxSameNodes"
	 * corresponding node IDs
	 */
	protected int maxSameNodes = 3;

	/**
	 * Cleanup period time, in SECONDS (0 = disable cleanup process)
	 */
	protected int cleanup = 60;

	/**
	 * Length of time-window in MILLISECONDS
	 */
	protected long windowLength = 5 * 1000L;

	/**
	 * Maximum number of errors in time-window
	 */
	protected int maxErrors = 3;

	/**
	 * Half-open timeout in MILLISECONDS
	 */
	protected long lockTimeout = 10 * 1000L;

	// --- COMPONENTS ---

	protected ServiceRegistry serviceRegistry;
	protected ContextFactory contextFactory;

	// --- IGNORABLE ERRORS / EXCEPTIONS ---

	protected Set<Class<? extends Throwable>> ignoredTypes = new HashSet<>();

	// --- ERROR COUNTERS ---

	protected ConcurrentHashMap<EndpointKey, ErrorCounter> errorCounters = new ConcurrentHashMap<>(1024);

	// --- START BREAKER ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Set components
		ServiceBrokerConfig cfg = broker.getConfig();
		this.serviceRegistry = cfg.getServiceRegistry();
		this.contextFactory = cfg.getContextFactory();
	}

	// --- STOP BREAKER ---

	@Override
	public void stopped() {
		errorCounters.clear();
		ignoredTypes.clear();
	}

	// --- CALL SERVICE ---

	public Promise call(String name, Tree params, CallOptions.Options opts, Context parent) {
		int remaining = opts == null ? 0 : opts.retryCount;
		if (enabled) {
			return callWithBreaker(name, params, opts, remaining, parent);
		}
		return callWithoutBreaker(name, params, opts, remaining, parent);
	}

	// --- CALL SERVICE WITHOUT BREAKER FUNCTION ---

	protected Promise callWithoutBreaker(String name, Tree params, CallOptions.Options opts, int remaining,
			Context parent) {
		try {
			String targetID = opts == null ? null : opts.nodeID;
			Action action = serviceRegistry.getAction(name, targetID);
			Context ctx = contextFactory.create(name, params, opts, parent);
			if (remaining < 1) {
				return Promise.resolve(action.handler(ctx));
			}
			return Promise.resolve(action.handler(ctx)).catchError(cause -> {
				return retryWithoutBreaker(cause, name, params, opts, remaining, parent);
			});
		} catch (Throwable cause) {
			if (remaining < 1) {
				return Promise.reject(cause);
			}
			return retryWithoutBreaker(cause, name, params, opts, remaining, parent);
		}
	}

	protected Promise retryWithoutBreaker(Throwable cause, String name, Tree params, CallOptions.Options opts,
			int remaining, Context parent) {
		remaining--;
		logger.warn("Retrying request (" + remaining + " attempts left)...", cause);
		return callWithoutBreaker(name, params, opts, remaining, parent);
	}

	// --- CALL SERVICE WITH BREAKER FUNCTION ---

	protected Promise callWithBreaker(String name, Tree params, CallOptions.Options opts, int remaining,
			Context parent) {
		EndpointKey endpointKey = null;
		ErrorCounter errorCounter = null;
		try {

			// Get the first recommended Endpoint and Error Counter
			String targetID = opts == null ? null : opts.nodeID;
			ActionEndpoint action = (ActionEndpoint) serviceRegistry.getAction(name, targetID);
			String nodeID = action.getNodeID();
			endpointKey = new EndpointKey(nodeID, name);
			errorCounter = errorCounters.get(endpointKey);

			// Check availability of the Endpoint (if endpoint isn't targetted)
			if (targetID == null) {
				LinkedHashSet<String> nodeIDs = new LinkedHashSet<>(maxSameNodes * 2);
				int sameNodeCounter = 0;
				long now;
				if (errorCounter == null) {
					now = 0;
				} else {
					now = System.currentTimeMillis();
				}
				for (int i = 0; i < maxTries; i++) {
					if (errorCounter == null || errorCounter.isAvailable(now)) {

						// Endpoint is available
						break;
					}

					// Store nodeID
					if (!nodeIDs.add(nodeID)) {
						sameNodeCounter++;
						if (sameNodeCounter >= maxSameNodes) {

							// The "maxSameNodes" limit is reached
							break;
						}
					}

					// Try to choose another endpoint
					action = (ActionEndpoint) serviceRegistry.getAction(name, targetID);
					nodeID = action.getNodeID();
					endpointKey = new EndpointKey(nodeID, name);
					errorCounter = errorCounters.get(endpointKey);
				}
			}

			// Create new Context
			Context ctx = contextFactory.create(name, params, opts, parent);

			// Invoke Endpoint
			final ErrorCounter currentCounter = errorCounter;
			final EndpointKey currentKey = endpointKey;
			return Promise.resolve(action.handler(ctx)).then(rsp -> {

				// Reset error counter
				if (currentCounter != null) {
					currentCounter.reset();
				}

				// Return response
				return rsp;

			}).catchError(cause -> {

				// Increment error counter
				increment(currentCounter, currentKey, cause, System.currentTimeMillis());

				// Return with error
				if (remaining < 1) {
					return cause;
				}

				// Retry
				return retryWithBreaker(cause, name, params, opts, remaining, parent);
			});

		} catch (Throwable cause) {

			// Increment error counter
			increment(errorCounter, endpointKey, cause, System.currentTimeMillis());

			// Reject
			if (remaining < 1) {
				return Promise.reject(cause);
			}

			// Retry
			return retryWithBreaker(cause, name, params, opts, remaining, parent);
		}
	}

	protected Promise retryWithBreaker(Throwable cause, String name, Tree params, CallOptions.Options opts,
			int remaining, Context parent) {
		remaining--;
		logger.warn("Retrying request (" + remaining + " attempts left)...", cause);
		return callWithBreaker(name, params, opts, remaining, parent);
	}

	protected void increment(ErrorCounter errorCounter, EndpointKey endpointKey, Throwable cause, long now) {
		if (endpointKey != null) {

			// Check error type
			if (!ignoredTypes.isEmpty()) {
				Class<? extends Throwable> test = cause.getClass();
				for (Class<? extends Throwable> type : ignoredTypes) {
					if (type.isAssignableFrom(test)) {

						// Ignore error
						return;
					}
				}
			}

			// Create new Error Counter
			if (errorCounter == null) {
				errorCounter = new ErrorCounter(windowLength, lockTimeout, maxErrors);
				ErrorCounter prev = errorCounters.put(endpointKey, errorCounter);
				if (prev != null) {
					errorCounter = prev;
				}
				errorCounter.increment(now);
			} else {
				errorCounter.increment(now);
			}
		}
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

	public int getMaxTries() {
		return maxTries;
	}

	public void setMaxTries(int maxTries) {
		this.maxTries = maxTries;
	}

	public int getMaxSameNodes() {
		return maxSameNodes;
	}

	public void setMaxSameNodes(int maxSameNodes) {
		this.maxSameNodes = maxSameNodes;
	}

	public int getCleanup() {
		return cleanup;
	}

	public void setCleanup(int cleanup) {
		this.cleanup = cleanup;
	}

	public long getWindowLength() {
		return windowLength;
	}

	public void setWindowLength(long windowLength) {
		this.windowLength = windowLength;
	}

	public int getMaxErrors() {
		return maxErrors;
	}

	public void setMaxErrors(int maxErrors) {
		this.maxErrors = maxErrors;
	}

}