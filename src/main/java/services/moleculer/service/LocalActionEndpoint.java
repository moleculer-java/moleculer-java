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
package services.moleculer.service;

import java.util.concurrent.ExecutorService;

import io.datatree.Promise;
import io.datatree.Tree;

public class LocalActionEndpoint extends ActionEndpoint {

	// --- CONSTRUCTOR ---

	public LocalActionEndpoint(DefaultServiceRegistry registry, ExecutorService executor, String nodeID, Tree config,
			Action action) {
		super(nodeID, config);

		// Handle local timeout with a handler
		current = ctx -> {
			if (ctx.opts != null && ctx.opts.timeout > 0) {

				// Create promise
				Promise promise = new Promise();

				// Execute local task
				executor.execute(() -> {

					// Set timeout
					long timeoutAt = System.currentTimeMillis() + ctx.opts.timeout;

					// Register promise
					registry.register(ctx.id, promise, timeoutAt);

					// Invoke async method
					try {
						Object rsp = action.handler(ctx);

						// Deregister
						Promise.resolve(rsp).then(in -> {
							if (promise.complete(in)) {
								registry.deregister(ctx.id);
							}
						}).catchError(err -> {
							if (promise.complete(err)) {
								registry.deregister(ctx.id);
							}
						});
					} catch (Exception cause) {
						registry.deregister(ctx.id);
						promise.complete(cause);
					}

				});

				// Return promise
				return promise;

			} else {

				// Invoke handler without timeout handling
				return action.handler(ctx);
			}

		};
	}

}