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
package services.moleculer.service;

import static services.moleculer.util.CommonUtils.nameOf;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.context.CallingOptions;
import services.moleculer.context.Context;
import services.moleculer.context.ContextFactory;

/**
 * Container (action invoker) of local actions.
 */
public final class LocalActionEndpoint extends ActionEndpoint {

	// --- PROPERTIES ---

	/**
	 * Action instance (it's a field / inner class in Service object)
	 */
	private final Action action;

	/**
	 * Invoke all local actions via Thread pool (true) or directly (false)
	 */
	private boolean asyncLocalInvocation;

	/**
	 * Atomic counter for internal timout handling
	 */
	private final AtomicLong internalUID = new AtomicLong();

	// --- COMPONENTS ---

	private final DefaultServiceRegistry registry;
	private ContextFactory context;
	private ExecutorService executor;

	// --- CONSTRUCTOR ---

	LocalActionEndpoint(DefaultServiceRegistry registry, Action action, boolean asyncLocalInvocation) {
		this.registry = registry;
		this.action = action;
		this.asyncLocalInvocation = asyncLocalInvocation;
	}

	// --- START CONTAINER ---

	/**
	 * Initializes Container instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public final void start(ServiceBroker broker, Tree config) throws Exception {
		super.start(broker, config);

		// Process config
		asyncLocalInvocation = config.get("asyncLocalInvocation", asyncLocalInvocation);

		// Set name
		if (name == null || name.isEmpty()) {
			name = nameOf(action, false);
		}

		// Set nodeID
		nodeID = broker.nodeID();

		// Set components
		context = broker.components().context();
		executor = broker.components().executor();
	}

	// --- INVOKE LOCAL ACTION ---

	@Override
	protected final Promise callActionNoStore(Tree params, CallingOptions.Options opts, Context parent) {

		// Set timeout (limit timestamp in millis)
		int timeout;
		if (opts == null) {
			timeout = defaultTimeout;
		} else {
			timeout = opts.timeout();
			if (timeout < 1) {
				timeout = defaultTimeout;
			}
		}
		long timeoutAt;
		if (timeout > 0) {
			timeoutAt = System.currentTimeMillis() + (timeout * 1000L);
		} else {
			timeoutAt = 0;
		}

		// Create new context (without ID)
		final Context ctx = context.create(name, params, opts, parent, false);

		// A.) Async invocation
		if (asyncLocalInvocation || timeout > 0) {

			// Execute in thread pool
			Promise promise = new Promise(CompletableFuture.supplyAsync(() -> {
				try {
					return action.handler(ctx);
				} catch (Throwable error) {
					return error;
				}
			}, executor));

			// No timeout / done
			if (timeoutAt < 0 || promise.isDone()) {
				return promise;
			}

			// Register promise (timeout handling)
			final String id = Long.toString(internalUID.incrementAndGet());
			registry.register(id, promise, timeoutAt);
			return promise;
		}

		// B.) Faster in-process (direct) action invocation
		try {
			return new Promise(action.handler(ctx));
		} catch (Throwable error) {
			return Promise.reject(error);
		}

	}

	// --- PROPERTY GETTERS ---

	@Override
	public final boolean local() {
		return true;
	}

}