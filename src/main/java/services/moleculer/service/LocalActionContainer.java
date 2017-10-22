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

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.context.CallingOptions;
import services.moleculer.context.Context;
import services.moleculer.context.ContextFactory;

/**
 * Container (action invoker) of local actions.
 */
public final class LocalActionContainer extends AbstractContainer {

	// --- PROPERTIES ---

	private final Action action;
	private final boolean asyncLocalInvocation;

	// --- COMPONENTS ---
	
	private ContextFactory context;
	private ExecutorService executor;
	
	// --- CONSTRUCTOR ---
	
	LocalActionContainer(Action action, boolean asyncLocalInvocation) {
		this.action = action;
		this.asyncLocalInvocation = asyncLocalInvocation;
	}
	
	// --- INIT CONTAINER ---

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
		
		// Set name
		if (name == null || name.isEmpty()) {
			name = nameOf(action, false);
		}

		// Set nodeID
		nodeID = broker.nodeID();
		
		// Set components
		context = broker.components().context();
		if (asyncLocalInvocation) {
			executor = broker.components().executor();
		}
	}

	// --- INVOKE LOCAL ACTION ---

	@Override
	public final Promise call(Tree params, CallingOptions opts, Context parent) {
		
		// Create new context
		Context ctx = context.create(name, params, opts, parent);
		
		// A.) Invoke local action via Thread pool
		if (asyncLocalInvocation) {
			return new Promise(CompletableFuture.supplyAsync(() -> {
				try {
					return action.handler(ctx);
				} catch (Throwable error) {
					return error;
				}
			}, executor));
		}

		// B.) In-process (direct) action invocation
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