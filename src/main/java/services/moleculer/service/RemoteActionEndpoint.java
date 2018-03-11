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

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.context.Context;
import services.moleculer.transporter.Transporter;

public class RemoteActionEndpoint extends ActionEndpoint {

	// --- CONSTRUCTOR ---

	public RemoteActionEndpoint(DefaultServiceRegistry registry, Transporter transporter, String nodeID, Tree config) {
		super(nodeID, config);
		this.current = new RemoteAction(registry, nodeID, transporter, this);
	}

	// --- REMOTE ACTION ---

	protected static class RemoteAction implements Action {

		// --- COMPONENTS ---

		protected Transporter transporter;
		protected DefaultServiceRegistry registry;

		// --- PROPERTIES ---

		protected final String nodeID;
		protected final RemoteActionEndpoint endpoint;

		// --- CONSTRUCTOR ---

		protected RemoteAction(DefaultServiceRegistry registry, String nodeID, Transporter transporter,
				RemoteActionEndpoint endpoint) {
			this.registry = registry;
			this.nodeID = nodeID;
			this.transporter = transporter;
			this.endpoint = endpoint;
		}

		@Override
		public Object handler(Context ctx) throws Exception {

			// Create new promise
			Promise promise = new Promise();

			// Set timeout
			long timeoutAt;
			if (ctx.opts != null && ctx.opts.timeout > 0) {
				timeoutAt = System.currentTimeMillis() + (ctx.opts.timeout * 1000L);
			} else {
				timeoutAt = 0;
			}

			// Register promise (socketTimeout and response handling)
			registry.register(ctx.id, promise, timeoutAt);

			// Send request via transporter
			Tree message = transporter.createRequestPacket(ctx);
			transporter.publish(Transporter.PACKET_REQUEST, nodeID, message);

			// Return promise
			return promise;
		}

	}

}