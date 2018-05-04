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

import static services.moleculer.util.CommonUtils.parseParams;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.eventbus.Groups;
import services.moleculer.service.ServiceInvoker;
import services.moleculer.util.ParseResult;

public class Context {

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
	 * 
	 * @see Tree.getMeta
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
	 * Calling options
	 */
	public final CallOptions.Options opts;
	
	// --- COMPONENTS ---

	protected final ServiceInvoker serviceInvoker;
	protected final Eventbus eventbus;

	// --- CONSTRUCTORS ---

	public Context(ServiceInvoker serviceInvoker, Eventbus eventbus, String id, String name, Tree params,
			CallOptions.Options opts) {

		// Set components
		this.serviceInvoker = serviceInvoker;
		this.eventbus = eventbus;

		// Set properties
		this.id = id;
		this.name = name;
		this.params = params;
		this.level = 1;
		this.parentID = null;
		this.opts = opts;

		// Set the first ID
		this.requestID = id;
	}

	public Context(String id, String name, Tree params, CallOptions.Options opts, Context parent) {

		// Set components
		this.serviceInvoker = parent.serviceInvoker;
		this.eventbus = parent.eventbus;

		// Set properties
		this.id = id;
		this.name = name;
		this.params = params;
		this.level = parent.level + 1;
		this.parentID = parent.id;
		this.opts = opts;

		// Get the request ID from parent
		this.requestID = parent.requestID;
	}

	public Context(ServiceInvoker serviceInvoker, Eventbus eventbus, String id, String name, Tree params, CallOptions.Options opts, int level, String requestID, String parentID) {

		// Set components
		this.serviceInvoker = serviceInvoker;
		this.eventbus = eventbus;

		// Set properties
		this.id = id;
		this.name = name;
		this.params = params;
		this.level = level;
		this.parentID = parentID;
		this.opts = opts;
		this.requestID = requestID;
	}
	
	// --- INVOKE LOCAL OR REMOTE ACTION ---

	/**
	 * Calls an action (local or remote). Sample code:
	 * 
	 * <pre>
	 * Promise promise = broker.call("math.add", "a", 1, "b", 2);
	 * </pre>
	 * 
	 * ...or with CallOptions:
	 * 
	 * <pre>
	 * broker.call("math.add", "a", 1, "b", 2, CallOptions.nodeID("node2"));
	 * </pre>
	 */
	public Promise call(String name, Object... params) {
		ParseResult res = parseParams(params);

		// TODO Recalculate distribuuted timeout
		// https://github.com/moleculerjs/moleculer/blob/master/src/context.js#L194
		return serviceInvoker.call(name, res.data, res.opts, this);
	}

	public Promise call(String name, Tree params) {
		return serviceInvoker.call(name, params, null, this);
	}

	public Promise call(String name, Tree params, CallOptions.Options opts) {
		return serviceInvoker.call(name, params, opts, this);
	}

	// --- EMIT EVENT TO EVENT GROUP ---

	/**
	 * Emits an event (grouped & balanced global event)
	 */
	public void emit(String name, Object... params) {
		ParseResult res = parseParams(params);
		eventbus.emit(name, res.data, res.groups, false);
	}

	/**
	 * Emits an event (grouped & balanced global event)
	 */
	public void emit(String name, Tree payload, Groups groups) {
		eventbus.emit(name, payload, groups, false);
	}

	/**
	 * Emits an event (grouped & balanced global event)
	 */
	public void emit(String name, Tree payload) {
		eventbus.emit(name, payload, null, false);
	}

	// --- BROADCAST EVENT TO ALL LISTENERS ---

	/**
	 * Emits an event for all local & remote services
	 */
	public void broadcast(String name, Object... params) {
		ParseResult res = parseParams(params);
		eventbus.broadcast(name, res.data, res.groups, false);
	}

	/**
	 * Emits an event for all local & remote services
	 */
	public void broadcast(String name, Tree payload, Groups groups) {
		eventbus.broadcast(name, payload, groups, false);
	}

	/**
	 * Emits an event for all local & remote services
	 */
	public void broadcast(String name, Tree payload) {
		eventbus.broadcast(name, payload, null, false);
	}

	// --- BROADCAST EVENT TO LOCAL LISTENERS ---

	/**
	 * Emits an event for all local services.
	 */
	public void broadcastLocal(String name, Object... params) {
		ParseResult res = parseParams(params);
		eventbus.broadcast(name, res.data, res.groups, true);
	}

	/**
	 * Emits an event for all local services.
	 */
	public void broadcastLocal(String name, Tree payload, Groups groups) {
		eventbus.broadcast(name, payload, groups, true);
	}

	/**
	 * Emits an event for all local services.
	 */
	public void broadcastLocal(String name, Tree payload) {
		eventbus.broadcast(name, payload, null, true);
	}

}