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
package services.moleculer.context;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.error.RequestRejectedError;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.eventbus.Groups;
import services.moleculer.service.ServiceInvoker;
import services.moleculer.stream.PacketStream;
import services.moleculer.uid.UidGenerator;
import services.moleculer.util.CheckedTree;
import services.moleculer.util.FastBuildTree;

public class Context extends ContextSource {

	// --- PROPERTIES ---

	/**
	 * Unique context ID
	 */
	public final String id;

	/**
	 * Action name
	 */
	public final String name;

	/**
	 * Request parameters (including {@link io.datatree.Tree#getMeta() meta})
	 */
	public final Tree params;

	/**
	 * Request level (in nested-calls) - the first level is 1
	 */
	public final int level;

	/**
	 * Parent context ID (in nested-calls)
	 */
	public final String parentID;

	/**
	 * Request ID (= first context ID)
	 */
	public final String requestID;

	/**
	 * Streamed content
	 */
	public final PacketStream stream;

	/**
	 * Calling options
	 */
	public final CallOptions.Options opts;

	/**
	 * Source nodeID
	 */
	public final String nodeID;
	
	// --- TIMESTAMP ---

	/**
	 * Timestamp
	 */
	protected final long createdAt;

	// --- CONSTRUCTORS ---

	public Context(ServiceInvoker serviceInvoker, Eventbus eventbus, UidGenerator uidGenerator, String id, String name,
			Tree params, int level, String parentID, String requestID, PacketStream stream, CallOptions.Options opts, String nodeID) {
		super(serviceInvoker, eventbus, uidGenerator, nodeID);

		// Set properties
		this.id = id;
		this.name = name;
		this.params = params;
		this.level = level;
		this.parentID = parentID;
		this.requestID = requestID == null ? id : requestID;
		this.stream = stream;
		this.opts = opts;
		this.nodeID = nodeID;

		// Store timestamp
		if (opts != null && opts.timeout > 0) {
			createdAt = System.currentTimeMillis();
		} else {
			createdAt = 0;
		}
	}

	// --- EMIT (WITH MERGED META) ---

	@Override
	protected void emit(String name, Tree payload, Groups groups, PacketStream stream, CallOptions.Options opts) {
		eventbus.emit(new Context(serviceInvoker, eventbus, uidGenerator, uidGenerator.nextUID(), name,
				mergeMeta(payload), level + 1, null, null, stream, opts, nodeID), groups, false);
	}

	// --- BROADCAST (WITH MERGED META) ---

	@Override
	protected void broadcast(String name, Tree payload, Groups groups, PacketStream stream, CallOptions.Options opts,
			boolean local) {
		eventbus.broadcast(new Context(serviceInvoker, eventbus, uidGenerator, uidGenerator.nextUID(), name,
				mergeMeta(payload), level + 1, null, null, stream, opts, nodeID), groups, local);
	}

	// --- CALL (WITH MERGED META AND DISTRIBUTED TIMEOUT) ---

	/**
	 * Calls an action (local or remote).
	 * 
	 * @param name
	 *            action name (eg. "math.add" in "service.action" syntax)
	 * @param params
	 *            {@link Tree} structure (input parameters of the method call)
	 * @param opts
	 *            calling options (target nodeID, call timeout, number of
	 *            retries)
	 * @param stream
	 *            streamed data (optional)
	 * 
	 * @return response Promise
	 */
	@Override
	protected Promise call(String name, Tree params, CallOptions.Options opts, PacketStream stream) {

		// Recalculate distributed timeout
		if (createdAt > 0) {

			// Distributed timeout handling. Decrementing the timeout value with
			// the elapsed time. If the timeout below 0, skip the call.
			final long duration = System.currentTimeMillis() - createdAt;
			final long distTimeout = this.opts.timeout - duration;

			if (distTimeout <= 0) {
				return Promise.reject(new RequestRejectedError(serviceInvoker.getBroker().getNodeID(), name));
			}

			if (opts == null) {
				opts = CallOptions.timeout(distTimeout);
			} else if (opts.timeout < 1 || distTimeout < opts.timeout) {
				opts = opts.timeout(distTimeout);
			}
		}
		return serviceInvoker.call(new Context(serviceInvoker, eventbus, uidGenerator, uidGenerator.nextUID(), name,
				mergeMeta(params), level + 1, id, requestID, stream, opts, nodeID));
	}

	protected Tree mergeMeta(Tree newParams) {
		if (params != null) {
			Tree meta = params.getMeta(false);
			if (meta != null && !meta.isEmpty()) {
				if (newParams == null) {
					return new CheckedTree(null, meta.asObject());
				}
				newParams.getMeta().copyFrom(meta, false);
			}
		}
		return newParams;
	}

	// --- HELPERS ---
	
	@Override
	public String toString() {
		FastBuildTree tree = new FastBuildTree(10);
		tree.putUnsafe("id", id);
		tree.putUnsafe("name", name);
		tree.putUnsafe("nodeID", nodeID);
		if (parentID != null) {
			tree.putUnsafe("parentID", parentID);
		}
		tree.putUnsafe("requestID", requestID);
		tree.putUnsafe("level", level);
		if (params != null) {
			tree.putUnsafe("params", params);
		}
		if (opts != null) {
			FastBuildTree o = new FastBuildTree(4);
			o.put("nodeID", opts.nodeID);
			o.put("retryCount", opts.retryCount);
			o.put("timeout", opts.timeout);
			tree.putUnsafe("opts", o);			
		}
		if (stream != null) {
			FastBuildTree s = new FastBuildTree(4);
			s.put("packetDelay", stream.getPacketDelay());
			s.put("packetSize", stream.getPacketSize());
			s.put("transferedBytes", stream.getTransferedBytes());
			tree.putUnsafe("stream", s);
		}
		return tree.toString();
	}	
	
}